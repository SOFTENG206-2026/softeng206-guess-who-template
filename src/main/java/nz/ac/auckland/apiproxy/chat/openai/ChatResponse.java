package nz.ac.auckland.apiproxy.chat.openai;

public class ChatResponse {
  private final ChatMessage message;
  private final String finishReason;

  protected ChatResponse(ChatMessage message, String finishReason) {
    this.message = message;
    this.finishReason = finishReason;
  }

  public ChatMessage getChatMessage() {
    return message;
  }

  public String getFinishReason() {
    return finishReason;
  }
}
