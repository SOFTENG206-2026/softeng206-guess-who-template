package nz.ac.auckland.apiproxy.chat.openai;

import java.util.List;
import java.util.Map;

public class ChatCompletionResult {

  private static Object required(Map<?, ?> values, String key) {
    Object value = values.get(key);
    if (value == null) {
      throw new IllegalArgumentException("Chat completion did not include '" + key + "'.");
    }
    return value;
  }

  private String model;
  private long created;
  private int usagePromptTokens;
  private int usageCompletionTokens;
  private int usageReasoningTokens;
  private int usageTotalTokens;
  private ChatResponse chatResponse;

  protected ChatCompletionResult(Map<String, Object> completion) {
    model = required(completion, "model").toString();
    created =
        completion.get("created") == null
            ? 0
            : Long.parseLong(completion.get("created").toString());
    usagePromptTokens = getUsage("prompt_tokens", completion);
    usageCompletionTokens = getUsage("completion_tokens", completion);
    usageReasoningTokens =
        getNestedUsage("completion_tokens_details", "reasoning_tokens", completion);
    usageTotalTokens = getUsage("total_tokens", completion);

    List<?> choices = (List<?>) completion.get("choices");
    if (choices == null || choices.isEmpty()) {
      throw new IllegalArgumentException("Chat completion did not contain a response.");
    }
    if (choices.size() != 1) {
      throw new IllegalArgumentException(
          "Chat completion contained " + choices.size() + " responses; expected exactly one.");
    }
    Map<?, ?> choice = (Map<?, ?>) choices.get(0);
    Map<?, ?> message = (Map<?, ?>) required(choice, "message");
    String role = required(message, "role").toString();
    Object contentValue = message.get("content");
    String content = contentValue == null ? "" : contentValue.toString();
    Object finishValue = choice.get("finish_reason");
    String finishReason = finishValue == null ? null : finishValue.toString();
    chatResponse = new ChatResponse(new ChatMessage(role, content), finishReason);
  }

  private int getUsage(String key, Map<String, Object> completion) {
    Map<?, ?> usage = (Map<?, ?>) required(completion, "usage");
    return Integer.parseInt(required(usage, key).toString());
  }

  private int getNestedUsage(String detailsKey, String usageKey, Map<String, Object> completion) {
    Map<?, ?> usage = (Map<?, ?>) required(completion, "usage");
    Object detailsValue = usage.get(detailsKey);
    if (!(detailsValue instanceof Map)) {
      return 0;
    }
    Object usageValue = ((Map<?, ?>) detailsValue).get(usageKey);
    return usageValue == null ? 0 : Integer.parseInt(usageValue.toString());
  }

  public int getUsagePromptTokens() {
    return usagePromptTokens;
  }

  public int getUsageCompletionTokens() {
    return usageCompletionTokens;
  }

  /** Reasoning tokens are already included in completion tokens. */
  public int getUsageReasoningTokens() {
    return usageReasoningTokens;
  }

  public int getUsageTotalTokens() {
    return usageTotalTokens;
  }

  /** Returns the actual model reported by OpenAI, including any dated model suffix. */
  public String getModel() {
    return model;
  }

  public long getCreated() {
    return created;
  }

  public ChatResponse getChatResponse() {
    return chatResponse;
  }
}
