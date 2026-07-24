package nz.ac.auckland.se206.gpt.openai;

import static org.junit.jupiter.api.Assertions.assertTrue;

import nz.ac.auckland.apiproxy.chat.openai.ChatCompletionRequest;
import nz.ac.auckland.apiproxy.chat.openai.ChatCompletionRequest.ReasoningEffort;
import nz.ac.auckland.apiproxy.chat.openai.ChatCompletionResult;
import nz.ac.auckland.apiproxy.config.ApiProxyConfig;
import nz.ac.auckland.apiproxy.exceptions.ApiProxyException;
import org.junit.jupiter.api.Test;

public class ChatCompletionServiceTest {

  @Test
  public void testChatCompletion() throws ApiProxyException {
    ApiProxyConfig config = ApiProxyConfig.readConfig();

    ChatCompletionRequest chatCompletionRequest =
        new ChatCompletionRequest(config)
            .addMessage(
                "developer",
                "Answer questions about New Zealand using only the requested place name.")
            .addMessage("user", "We are discussing New Zealand.")
            .addMessage("assistant", "Understood.")
            .addMessage("user", "What is the capital of New Zealand?")
            .setMaxCompletionTokens(100)
            .setReasoningEffort(ReasoningEffort.NONE);

    ChatCompletionResult result = chatCompletionRequest.execute();
    String answer = result.getChatResponse().getChatMessage().getContent();

    assertTrue(answer.toLowerCase().contains("wellington"));
  }
}
