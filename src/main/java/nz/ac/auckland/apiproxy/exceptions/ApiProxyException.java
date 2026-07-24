package nz.ac.auckland.apiproxy.exceptions;

public class ApiProxyException extends Exception {

  private static final long serialVersionUID = 1L;

  public ApiProxyException(String message) {
    super(message);
  }

  public ApiProxyException(String message, Throwable cause) {
    super(message, cause);
  }
}
