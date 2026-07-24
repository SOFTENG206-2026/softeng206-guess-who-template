package nz.ac.auckland.apiproxy.tts;

public class TextToSpeechResult {
  private String audioUrl;

  protected TextToSpeechResult(String audioUrl) {
    this.audioUrl = audioUrl;
  }

  public String getAudioUrl() {
    return audioUrl;
  }
}
