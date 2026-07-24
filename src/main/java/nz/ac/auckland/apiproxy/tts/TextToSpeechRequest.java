package nz.ac.auckland.apiproxy.tts;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import nz.ac.auckland.apiproxy.config.ApiProxyConfig;
import nz.ac.auckland.apiproxy.exceptions.ApiProxyException;
import nz.ac.auckland.apiproxy.service.EndPoints;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class TextToSpeechRequest {

  public enum Provider {
    OPENAI("openai");

    private final String providerCode;

    Provider(String providerCode) {
      this.providerCode = providerCode;
    }

    public String getProviderCode() {
      return providerCode;
    }

    public Voice getDefaultVoice() {
      return Voice.OPENAI_NOVA;
    }
  }

  public enum Voice {
    NOT_SET("not_set"),

    // OpenAI voices
    OPENAI_ALLOY("alloy"),
    OPENAI_ASH("ash"),
    OPENAI_CORAL("coral"),
    OPENAI_ECHO("echo"),
    OPENAI_FABLE("fable"),
    OPENAI_ONYX("onyx"),
    OPENAI_NOVA("nova"),
    OPENAI_SAGE("sage"),
    OPENAI_SHIMMER("shimmer");

    private final String voiceCode;

    Voice(String voiceCode) {
      this.voiceCode = voiceCode;
    }

    public String getVoiceCode() {
      return voiceCode;
    }
  }

  private static final int CONNECT_TIMEOUT_MILLISECONDS = 10_000;
  private static final int RESPONSE_TIMEOUT_MILLISECONDS = 120_000;

  private ApiProxyConfig config;

  private String text = null; // Required
  private Provider provider = Provider.OPENAI; // Default provider
  private Voice voice = Voice.NOT_SET;

  public TextToSpeechRequest(ApiProxyConfig config) {
    this.config = config;
  }

  public TextToSpeechRequest setText(String text) {
    this.text = text;
    return this;
  }

  public TextToSpeechRequest setProvider(Provider provider) {
    this.provider = provider;
    return this;
  }

  public TextToSpeechRequest setVoice(Voice voice) {
    this.voice = voice;
    return this;
  }

  public TextToSpeechResult execute() throws ApiProxyException {

    // Validate the input and resolve provider defaults before building the API request.
    if (isEmpty(text)) {
      throw new ApiProxyException("The text is missing or empty.");
    }

    // Use OpenAI when the caller does not specify a provider.
    if (provider == null) {
      provider = Provider.OPENAI;
    }

    // Select the provider's standard voice when no explicit voice is supplied.
    if (voice == null || voice == Voice.NOT_SET) {
      voice = provider.getDefaultVoice();
    }

    // Make sure the selected voice belongs to the chosen provider.
    String providerName = provider.name();
    String voiceName = voice.name();
    if (!voiceName.startsWith(providerName)) {
      throw new ApiProxyException(
          "The voice '"
              + voiceName
              + "' is not supported by the provider '"
              + providerName
              + "'. Please choose a different voice starting with '"
              + providerName
              + "_xxx'.");
    }

    try {
      // Build the JSON payload expected by the text-to-speech proxy.
      JsonObjectBuilder jsonOverallBuilder =
          Json.createObjectBuilder() //
              .add("provider", provider.getProviderCode()) //
              .add("text", text);

      jsonOverallBuilder.add("voice", voice.getVoiceCode());
      jsonOverallBuilder.add("access_token", config.getApiKey()).add("email", config.getEmail());

      JsonObject value = jsonOverallBuilder.build();

      // Prepare an authenticated UTF-8 JSON request.
      HttpPost httpPost = new HttpPost(EndPoints.PROXY_TEXT_TO_SPEECH);
      httpPost.setHeader("Content-Type", "application/json");
      httpPost.setHeader("Accept", "application/json");
      httpPost.setEntity(new StringEntity(value.toString(), StandardCharsets.UTF_8));
      ObjectMapper mapperApiMapper = new ObjectMapper();

      // Bound connection setup and response waiting times.
      RequestConfig requestConfig =
          RequestConfig.custom()
              .setConnectTimeout(CONNECT_TIMEOUT_MILLISECONDS)
              .setConnectionRequestTimeout(CONNECT_TIMEOUT_MILLISECONDS)
              .setSocketTimeout(RESPONSE_TIMEOUT_MILLISECONDS)
              .build();

      // Execute the request while ensuring all HTTP resources are closed.
      ResponseTtsViaProxy responseTts = null;
      try (CloseableHttpClient client =
              HttpClients.custom().setDefaultRequestConfig(requestConfig).build();
          CloseableHttpResponse httpResponse = client.execute(httpPost)) {
        int statusCode = httpResponse.getStatusLine().getStatusCode();
        if (httpResponse.getEntity() == null) {
          throw new ApiProxyException("API proxy returned an empty response.");
        }
        String responseBody =
            EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8);
        try {
          responseTts = mapperApiMapper.readValue(responseBody, ResponseTtsViaProxy.class);
        } catch (Exception parseError) {
          // A successful response must contain valid proxy JSON; failed responses may not.
          if (statusCode >= 200 && statusCode < 300) {
            throw parseError;
          }
        }
        if (statusCode < 200 || statusCode >= 300) {
          String detail = responseTts == null ? "" : " " + responseTts.message;
          throw new ApiProxyException(
              "API proxy returned HTTP status " + statusCode + "." + detail);
        }
      }

      // Validate the proxy result before exposing the encoded audio to the caller.
      if (responseTts == null || !Boolean.TRUE.equals(responseTts.success)) {
        throw new ApiProxyException(
            "Problem calling API: "
                + (responseTts == null ? "empty response" : responseTts.message));
      }
      if (isEmpty(responseTts.audio)) {
        throw new ApiProxyException("API proxy response did not include audio.");
      }
      return new TextToSpeechResult(responseTts.audio);

    } catch (ApiProxyException e) {
      throw e;
    } catch (Exception e) {
      throw new ApiProxyException("Problem calling API: " + e.getMessage(), e);
    }
  }

  private boolean isEmpty(String text) {
    return text == null || text.isEmpty();
  }
}
