package nz.ac.auckland.apiproxy.tts;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ResponseTtsViaProxy {

  public final Boolean success;
  public final Integer code;
  public final String message;
  public final String audio;

  public ResponseTtsViaProxy(
      @JsonProperty("success") Boolean success,
      @JsonProperty("code") Integer code,
      @JsonProperty("message") String message,
      @JsonProperty("audio") String audio) {
    this.success = success;
    this.code = code;
    this.message = message;
    this.audio = audio;
  }
}
