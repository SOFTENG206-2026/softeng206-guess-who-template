package nz.ac.auckland.se206;

import java.net.URL;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

/** Owns the single background-music player used across application scenes. */
public final class AudioManager {

  private static final double DEFAULT_VOLUME = 0.05;
  private static MediaPlayer backgroundPlayer;

  private AudioManager() {}

  /** Starts looping background music unless it has already been started. */
  public static void playBackgroundMusic() {
    if (backgroundPlayer != null) {
      return;
    }

    URL musicResource = AudioManager.class.getResource("/sounds/background.mp3");
    if (musicResource == null) {
      System.err.println("Background music not found: /sounds/background.mp3");
      return;
    }

    Media media = new Media(musicResource.toExternalForm());
    backgroundPlayer = new MediaPlayer(media);
    backgroundPlayer.setCycleCount(MediaPlayer.INDEFINITE);
    backgroundPlayer.setVolume(DEFAULT_VOLUME);
    backgroundPlayer.setOnError(
        () -> System.err.println("Background music error: " + backgroundPlayer.getError()));
    backgroundPlayer.play();
  }

  /** Mutes or unmutes the background player. */
  public static void setMuted(boolean muted) {
    if (backgroundPlayer != null) {
      backgroundPlayer.setMute(muted);
    }
  }

  /** Returns whether the background player is currently muted. */
  public static boolean isMuted() {
    return backgroundPlayer != null && backgroundPlayer.isMute();
  }

  /** Stops playback and releases native media resources. */
  public static void stopBackgroundMusic() {
    if (backgroundPlayer == null) {
      return;
    }

    backgroundPlayer.stop();
    backgroundPlayer.dispose();
    backgroundPlayer = null;
  }
}
