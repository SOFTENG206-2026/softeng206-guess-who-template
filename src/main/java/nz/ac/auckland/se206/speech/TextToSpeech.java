package nz.ac.auckland.se206.speech;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import javafx.concurrent.Task;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;
import nz.ac.auckland.apiproxy.config.ApiProxyConfig;
import nz.ac.auckland.apiproxy.exceptions.ApiProxyException;
import nz.ac.auckland.apiproxy.tts.TextToSpeechRequest;
import nz.ac.auckland.apiproxy.tts.TextToSpeechRequest.Provider;
import nz.ac.auckland.apiproxy.tts.TextToSpeechRequest.Voice;
import nz.ac.auckland.apiproxy.tts.TextToSpeechResult;

/** A utility class for converting text to speech using the specified API proxy. */
public class TextToSpeech {

  /**
   * Converts the given text to speech and plays the audio.
   *
   * @param text the text to be converted to speech
   * @throws IllegalArgumentException if the text is null or empty
   */
  public static void speak(String text) {
    // Reject invalid input before starting asynchronous work.
    if (text == null || text.isEmpty()) {
      throw new IllegalArgumentException("Text should not be null or empty");
    }

    // Perform the network request and audio playback away from the JavaFX application thread.
    Task<Void> backgroundTask =
        new Task<>() {
          @Override
          protected Void call() {
            try {
              // Load the proxy credentials and select the speech provider and voice.
              ApiProxyConfig config = ApiProxyConfig.readConfig();
              Provider provider = Provider.OPENAI;
              Voice voice = Voice.OPENAI_FABLE;

              // Configure the text-to-speech request with the caller's text.
              TextToSpeechRequest ttsRequest = new TextToSpeechRequest(config);
              ttsRequest.setText(text).setProvider(provider).setVoice(voice);

              // Ask the proxy to generate speech and return the resulting audio location.
              TextToSpeechResult ttsResult = ttsRequest.execute();
              String audioUrl = ttsResult.getAudioUrl();

              // Stream and play the generated audio, closing the stream when playback finishes.
              try (InputStream inputStream =
                  new BufferedInputStream(URI.create(audioUrl).toURL().openStream())) {
                Player player = new Player(inputStream);
                player.play();
              } catch (JavaLayerException | IOException e) {
                // Playback failures occur on the background task and must not terminate the UI.
                e.printStackTrace();
              }

            } catch (ApiProxyException e) {
              // Report proxy configuration or request failures from the background task.
              e.printStackTrace();
            }
            return null;
          }
        };

    // A daemon worker lets the application exit even if speech is still playing.
    Thread backgroundThread = new Thread(backgroundTask);
    backgroundThread.setDaemon(true);
    backgroundThread.start();
  }
}
