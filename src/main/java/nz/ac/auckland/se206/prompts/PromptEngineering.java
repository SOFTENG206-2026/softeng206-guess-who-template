package nz.ac.auckland.se206.prompts;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Utility class for prompt engineering. This class provides methods to load and fill prompt
 * templates with dynamic data.
 */
public class PromptEngineering {

  /**
   * Retrieves a prompt template, fills it with the provided data, and returns the filled prompt.
   *
   * @param promptId the ID of the prompt template to load
   * @param data the data to fill into the template
   * @return the filled prompt
   * @throws IllegalArgumentException if there is an error loading or filling the template
   */
  public static String getPrompt(String promptId, Map<String, String> data) {
    String resourcePath = "prompts/" + promptId;
    try (InputStream inputStream =
        PromptEngineering.class.getClassLoader().getResourceAsStream(resourcePath)) {
      if (inputStream == null) {
        throw new IllegalArgumentException("Prompt template not found: " + resourcePath);
      }
      String template = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
      return fillTemplate(template, data);
    } catch (IOException e) {
      throw new IllegalArgumentException("Unable to load prompt template: " + resourcePath, e);
    }
  }

  /**
   * Fills a template string with the provided data. Replaces placeholders in the template with
   * corresponding values from the data map.
   *
   * @param template the template string to fill
   * @param data the data to fill into the template
   * @return the filled template string
   */
  private static String fillTemplate(String template, Map<String, String> data) {
    for (Map.Entry<String, String> entry : data.entrySet()) {
      template = template.replace("{" + entry.getKey() + "}", entry.getValue());
    }
    return template;
  }
}
