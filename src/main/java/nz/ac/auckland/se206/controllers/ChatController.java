package nz.ac.auckland.se206.controllers;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import nz.ac.auckland.apiproxy.chat.openai.ChatCompletionRequest;
import nz.ac.auckland.apiproxy.chat.openai.ChatCompletionRequest.Model;
import nz.ac.auckland.apiproxy.chat.openai.ChatCompletionRequest.ReasoningEffort;
import nz.ac.auckland.apiproxy.chat.openai.ChatCompletionResult;
import nz.ac.auckland.apiproxy.chat.openai.ChatMessage;
import nz.ac.auckland.apiproxy.chat.openai.ChatResponse;
import nz.ac.auckland.apiproxy.config.ApiProxyConfig;
import nz.ac.auckland.apiproxy.exceptions.ApiProxyException;
import nz.ac.auckland.model.Customer;
import nz.ac.auckland.se206.App;
import nz.ac.auckland.se206.prompts.PromptEngineering;

/**
 * Controller class for the chat view. Handles user interactions and communication with the GPT
 * model via the API proxy.
 */
public class ChatController {

  @FXML private TextArea txtaChat;
  @FXML private TextField txtInput;
  @FXML private Button btnSend;
  @FXML private ImageView imagePerson;

  private Task<ChatMessage> currentGptTask;

  private ChatCompletionRequest chatCompletionRequest;
  private Customer customer;

  /** Initializes the chat view. */
  @FXML
  public void initialize() {
    // Any required initialization code can be placed here
  }

  /**
   * Generates the developer prompt based on the selected customer.
   *
   * @return the developer prompt string
   */
  private String getDeveloperPrompt() {
    Map<String, String> map = new HashMap<>();
    map.put("name", customer.getName());
    map.put("age", String.valueOf(customer.getAge()));
    map.put("profession", customer.getProfession());
    return PromptEngineering.getPrompt("chat.txt", map);
  }

  /**
   * Sets the customer for the chat context and initializes the ChatCompletionRequest.
   *
   * @param customer the customer to set
   */
  public void setCustomer(Customer customer) {
    this.customer = customer;
    setImage(customer.getImage());
    try {
      ApiProxyConfig config = ApiProxyConfig.readConfig();
      chatCompletionRequest =
          new ChatCompletionRequest(config)
              .setModel(Model.GPT_5_4_NANO)
              .setReasoningEffort(ReasoningEffort.LOW)
              .setMaxCompletionTokens(300);
      runGptBackground(new ChatMessage("developer", getDeveloperPrompt()));
    } catch (ApiProxyException e) {
      e.printStackTrace();
    }
  }

  public void setImage(String image) {
    URL imageResource = ChatController.class.getResource(image);
    if (imageResource == null) {
      throw new IllegalArgumentException("Image resource not found: " + image);
    }
    imagePerson.setImage(new Image(imageResource.toExternalForm()));
  }

  /**
   * Appends a chat message to the chat text area.
   *
   * @param msg the chat message to append
   */
  private void appendChatMessage(ChatMessage msg) {
    txtaChat.appendText(msg.getRole() + ": " + msg.getContent() + "\n\n");
  }

  /**
   * Runs the GPT model with a given chat message.
   *
   * @param msg the chat message to process
   * @return the response chat message
   * @throws ApiProxyException if there is an error communicating with the API proxy
   */
  private ChatMessage executeGPT(ChatMessage msg) throws ApiProxyException {
    if (!msg.getRole().equals("developer")) {
      appendChatMessage(msg);
    }
    chatCompletionRequest.addMessage(msg);
    ChatCompletionResult chatCompletionResult = chatCompletionRequest.execute();
    ChatResponse response = chatCompletionResult.getChatResponse();
    chatCompletionRequest.addMessage(response.getChatMessage());

    // TextToSpeech.speak(response.getChatMessage().getContent());
    return response.getChatMessage();
  }

  private void runGptBackground(ChatMessage msg) {

    if (currentGptTask != null) {
      return;
    }

    waitGPT(true);

    currentGptTask =
        new Task<>() {
          @Override
          protected ChatMessage call() throws Exception {
            return executeGPT(msg);
          }
        };

    currentGptTask.setOnSucceeded(
        event -> {
          ChatMessage result = currentGptTask.getValue();
          appendChatMessage(result);
          currentGptTask = null;
          waitGPT(false);
          txtInput.requestFocus();
        });

    currentGptTask.setOnFailed(
        event -> {
          currentGptTask.getException().printStackTrace();
          currentGptTask = null;
          waitGPT(false);
        });

    currentGptTask.setOnCancelled(
        event -> {
          currentGptTask = null;
          waitGPT(false);
        });

    Thread worker = new Thread(currentGptTask, "background-worker");
    worker.setDaemon(true);
    worker.start();
  }

  private void waitGPT(boolean isWait) {
    btnSend.setDisable(isWait);
    txtInput.setDisable(isWait);
    btnSend.setText(isWait ? "Thinking" : "Send");
  }

  /**
   * Sends a message to the GPT model.
   *
   * @param event the action event triggered by the send button
   */
  @FXML
  private void onSendMessage(ActionEvent event) {
    String message = txtInput.getText().trim();
    if (message.isEmpty()) {
      return;
    }
    txtInput.clear();
    ChatMessage msg = new ChatMessage("user", message);
    // appendChatMessage(msg);
    runGptBackground(msg);
  }

  /**
   * Navigates back to the previous view.
   *
   * @param event the action event triggered by the go back button
   * @throws IOException if there is an I/O error
   */
  @FXML
  private void onGoBack(ActionEvent event) throws IOException {
    if (currentGptTask != null) {
      currentGptTask.cancel();
    }
    App.setRoot("room");
  }
}
