package nz.ac.auckland.apiproxy.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.File;
import nz.ac.auckland.apiproxy.exceptions.ApiProxyException;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiProxyConfig {

  private static ApiProxyConfig instance;

  public static synchronized ApiProxyConfig readConfig() throws ApiProxyException {
    return readConfig(new File("apiproxy.config"));
  }

  public static synchronized ApiProxyConfig readConfig(File file) throws ApiProxyException {
    if (instance != null) {
      return instance;
    }
    try {
      ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
      instance = objectMapper.readValue(file, ApiProxyConfig.class);
      return instance;
    } catch (Exception e) {
      e.printStackTrace();
      String message =
          "Unable to read "
              + file.getAbsolutePath()
              + ". Please check the file exists and is valid.";
      throw new ApiProxyException(message);
    }
  }

  private String email = null;
  private String apiKey = null;

  private ApiProxyConfig() {}

  public String getApiKey() {
    return apiKey;
  }

  public String getEmail() {
    return email;
  }
}
