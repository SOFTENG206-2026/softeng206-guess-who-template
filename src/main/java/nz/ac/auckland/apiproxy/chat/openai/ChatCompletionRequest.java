package nz.ac.auckland.apiproxy.chat.openai;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Set;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
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

public class ChatCompletionRequest {

  public enum ReasoningEffort {
    NONE("none"),
    LOW("low"),
    MEDIUM("medium"),
    HIGH("high"),
    XHIGH("xhigh");

    private final String value;

    ReasoningEffort(String value) {
      // Receive the value used by the proxy API.
      // Keep the Java enum name separate from the wire representation.
      // Store the value once because enum instances are immutable.
      // Preserve the exact lowercase spelling required in request JSON.
      // Make the value available through getValue().
      this.value = value;
    }

    public String getValue() {
      // Expose the API-facing representation of this effort level.
      // Do not return the uppercase Java enum constant name.
      // Avoid transforming the value each time it is requested.
      // Let request builders use the same canonical spelling.
      // Return the immutable value assigned by the enum constructor.
      return value;
    }
  }

  public enum Model {
    GPT_5_4_NANO("gpt-5.4-nano", Set.of(ReasoningEffort.values()));

    private final String modelName;
    private final Set<ReasoningEffort> supportedReasoningEfforts;

    Model(String modelName, Set<ReasoningEffort> supportedReasoningEfforts) {
      // Receive the model identifier understood by the proxy.
      // Receive the effort levels supported by this model.
      // Keep display-independent API metadata on the enum constant.
      // Store the metadata once because model constants are immutable.
      // Make both values available to request validation and serialization.
      this.modelName = modelName;
      this.supportedReasoningEfforts = supportedReasoningEfforts;
    }

    public String getModelName() {
      // Expose the model identifier used in API requests.
      // Keep callers independent of the Java enum constant name.
      // Preserve punctuation and version information in the identifier.
      // Avoid rebuilding the identifier whenever a request is created.
      // Return the immutable name assigned by the enum constructor.
      return modelName;
    }

    public boolean supportsReasoningEffort(ReasoningEffort effort) {
      // Consult this model's declared capability set.
      // Use enum membership for an exact capability match.
      // Keep compatibility rules centralized in the Model enum.
      // Let validation reject unsupported model-effort combinations.
      // Return whether the requested effort is present in the set.
      return supportedReasoningEfforts.contains(effort);
    }
  }

  private static final int NOT_SET = -1;
  private static final int CONNECT_TIMEOUT_MILLISECONDS = 10_000;
  private static final int RESPONSE_TIMEOUT_MILLISECONDS = 120_000;

  private final ApiProxyConfig config;
  private final ArrayList<ChatMessage> messages = new ArrayList<>();
  // Null deliberately means that proxy mode uses the server's authoritative default.
  private Model model = null;
  private int maxCompletionTokens = NOT_SET;
  private ReasoningEffort reasoningEffort = null;

  public ChatCompletionRequest(ApiProxyConfig config) {
    // Receive the credentials and identity used by the API proxy.
    // Keep the configuration associated with this request builder.
    // Leave the message collection ready to accept conversation entries.
    // Preserve optional request fields at their server-default values.
    // Store the configuration for use when execute() builds the payload.
    this.config = config;
  }

  public ChatCompletionRequest addMessage(String role, String content) {
    // Accept the role and content in a convenient two-argument form.
    // Delegate ChatMessage construction to the domain object.
    // Reuse the overload that owns collection mutation.
    // Preserve the order in which conversation messages are added.
    // Return the delegated fluent-builder result to the caller.
    return addMessage(new ChatMessage(role, content));
  }

  public ChatCompletionRequest addMessage(ChatMessage message) {
    // Accept an already constructed conversation message.
    // Append rather than replace earlier conversation context.
    // Preserve insertion order through the backing ArrayList.
    // Defer JSON conversion until the request is executed.
    // Return this request so calls can be chained fluently.
    messages.add(message);
    return this;
  }

  public ChatCompletionRequest setModel(Model model) {
    // Accept the model selected by the caller.
    // Allow null to retain the proxy server's authoritative default.
    // Replace any model selection previously made on this request.
    // Defer model-effort compatibility checking until execution.
    // Return this request so configuration calls can be chained.
    this.model = model;
    return this;
  }

  public ChatCompletionRequest setMaxCompletionTokens(int value) {
    // Accept an explicit upper bound for generated completion tokens.
    // Require a positive value because zero-token completions are invalid.
    // Reject invalid input before mutating the request state.
    // Store the validated value for later JSON serialization.
    // Return this request so configuration calls can be chained.
    if (value < 1) {
      throw new IllegalArgumentException(
          "'max_completion_tokens' must be at least 1, but was given " + value);
    }
    maxCompletionTokens = value;
    return this;
  }

  public ChatCompletionRequest setReasoningEffort(ReasoningEffort effort) {
    // Accept the reasoning effort requested by the caller.
    // Require an explicit enum value when this setter is used.
    // Reject null before mutating the request state.
    // Store the effort for compatibility validation and serialization.
    // Return this request so configuration calls can be chained.
    if (effort == null) {
      throw new IllegalArgumentException("'reasoning_effort' must not be null");
    }
    reasoningEffort = effort;
    return this;
  }

  private void validateParameters() {
    // Validate relationships between optional request parameters.
    // Skip compatibility checking when no effort was requested.
    // Skip model-specific checking when the server will choose the model.
    // Ask the selected model whether it supports the requested effort.
    // Fail before network activity if the combination is unsupported.
    if (reasoningEffort != null
        && model != null
        && !model.supportsReasoningEffort(reasoningEffort)) {
      throw new IllegalArgumentException(
          "'"
              + model.getModelName()
              + "' does not support reasoning effort '"
              + reasoningEffort.getValue()
              + "'");
    }
  }

  public ChatCompletionResult execute() throws ApiProxyException {
    // Validate request options before constructing or sending a payload.
    // Serialize conversation messages in their original insertion order.
    // Include credentials and only the optional fields selected by the caller.
    // Apply bounded HTTP timeouts and close the client automatically.
    // Preserve proxy exceptions while wrapping unexpected failures consistently.
    try {
      validateParameters();
      JsonArrayBuilder jsonMessages = Json.createArrayBuilder();
      for (ChatMessage message : messages) {
        jsonMessages.add(
            Json.createObjectBuilder()
                .add("role", message.getRole())
                .add("content", message.getContent()));
      }

      JsonObjectBuilder builder = Json.createObjectBuilder().add("messages", jsonMessages);
      builder.add("access_token", config.getApiKey()).add("email", config.getEmail());
      if (maxCompletionTokens != NOT_SET) {
        builder.add("max_completion_tokens", maxCompletionTokens);
      }
      if (reasoningEffort != null) {
        builder.add("reasoning_effort", reasoningEffort.getValue());
      }
      if (model != null) {
        builder.add("model", model.getModelName());
      }

      JsonObject requestBody = builder.build();
      RequestConfig requestConfig =
          RequestConfig.custom()
              .setConnectTimeout(CONNECT_TIMEOUT_MILLISECONDS)
              .setConnectionRequestTimeout(CONNECT_TIMEOUT_MILLISECONDS)
              .setSocketTimeout(RESPONSE_TIMEOUT_MILLISECONDS)
              .build();

      try (CloseableHttpClient client =
          HttpClients.custom().setDefaultRequestConfig(requestConfig).build()) {
        return executeViaProxy(client, requestBody);
      }
    } catch (ApiProxyException e) {
      throw e;
    } catch (Exception e) {
      throw new ApiProxyException("Problem calling API: " + e.getMessage(), e);
    }
  }

  private ChatCompletionResult executeViaProxy(CloseableHttpClient client, JsonObject requestBody)
      throws Exception {
    // Create a JSON POST request for the chat-completions proxy endpoint.
    // Execute the request and close its response resource automatically.
    // Decode structured proxy JSON when the response body permits it.
    // Convert HTTP and proxy-level failures into ApiProxyException instances.
    // Return a result only after confirming completion data is present.
    HttpPost post = createPost(EndPoints.PROXY_OPENAI_CHAT_COMPLETIONS, requestBody);
    try (CloseableHttpResponse response = client.execute(post)) {
      int status = response.getStatusLine().getStatusCode();
      String body = readResponseBody(response);
      ObjectMapper mapper = new ObjectMapper();
      ResponseChatCompletionViaProxy proxyResponse = null;
      try {
        proxyResponse = mapper.readValue(body, ResponseChatCompletionViaProxy.class);
      } catch (Exception parseError) {
        if (status >= 200 && status < 300) {
          throw parseError;
        }
      }
      if (status < 200 || status >= 300) {
        String detail = proxyResponse == null ? "" : " " + proxyResponse.message;
        throw new ApiProxyException("API proxy returned HTTP status " + status + "." + detail);
      }
      if (proxyResponse == null || !Boolean.TRUE.equals(proxyResponse.success)) {
        throw new ApiProxyException(
            "Problem calling API: "
                + (proxyResponse == null ? "empty response" : proxyResponse.message));
      }
      if (proxyResponse.chatCompletion == null) {
        throw new ApiProxyException("API proxy response did not include a chat completion.");
      }
      return new ChatCompletionResult(proxyResponse.chatCompletion);
    }
  }

  private HttpPost createPost(String endpoint, JsonObject requestBody) {
    // Create a POST request targeting the supplied proxy endpoint.
    // Declare that the request body contains JSON.
    // Ask the proxy to return a JSON response.
    // Serialize the request payload using UTF-8.
    // Return the fully configured request to the HTTP execution helper.
    HttpPost post = new HttpPost(endpoint);
    post.setHeader("Content-Type", "application/json");
    post.setHeader("Accept", "application/json");
    post.setEntity(new StringEntity(requestBody.toString(), StandardCharsets.UTF_8));
    return post;
  }

  private String readResponseBody(CloseableHttpResponse response) throws Exception {
    // Inspect the HTTP response before attempting body conversion.
    // Treat a missing entity as an invalid empty API response.
    // Keep empty-response handling consistent with other proxy failures.
    // Decode response bytes explicitly as UTF-8 text.
    // Return the decoded body for JSON parsing by the caller.
    if (response.getEntity() == null) {
      throw new ApiProxyException("API returned an empty response.");
    }
    return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
  }
}
