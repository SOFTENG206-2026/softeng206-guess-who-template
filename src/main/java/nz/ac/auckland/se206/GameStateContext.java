package nz.ac.auckland.se206;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;
import nz.ac.auckland.model.Customer;
import nz.ac.auckland.se206.states.GameOver;
import nz.ac.auckland.se206.states.GameStarted;
import nz.ac.auckland.se206.states.GameState;
import nz.ac.auckland.se206.states.Guessing;
import org.yaml.snakeyaml.Yaml;

/**
 * Context class for managing the state of the game. Handles transitions between different game
 * states and maintains game data such as the professions and rectangle IDs.
 */
public class GameStateContext {

  private final String rectIdToGuess;
  private final String professionToGuess;
  private final Map<String, Customer> rectanglesToCustomer;
  private final GameStarted gameStartedState;
  private final Guessing guessingState;
  private final GameOver gameOverState;
  private final IntegerProperty elapsedSeconds;
  private final Timeline gameTimer;

  private GameState gameState;

  /** Constructs a new GameStateContext and initializes the game states and professions. */
  public GameStateContext() {

    elapsedSeconds = new SimpleIntegerProperty(119);
    KeyFrame oneSecond =
        new KeyFrame(
            Duration.seconds(1),
            event -> {
              elapsedSeconds.set(elapsedSeconds.get() - 1);
            });
    gameTimer = new Timeline(oneSecond);
    gameTimer.setCycleCount(Animation.INDEFINITE);

    gameStartedState = new GameStarted(this);
    guessingState = new Guessing(this);
    gameOverState = new GameOver(this);

    gameState = gameStartedState; // Initial state
    Yaml yaml = new Yaml();
    List<String> professions;
    try (InputStream inputStream =
        GameStateContext.class.getClassLoader().getResourceAsStream("data/professions.yaml")) {
      if (inputStream == null) {
        throw new IllegalStateException("Profession data file not found: data/professions.yaml");
      }
      Object loadedData = yaml.load(inputStream);
      if (!(loadedData instanceof Map)) {
        throw new IllegalStateException("Profession data must contain a 'professions' list.");
      }
      Object professionData = ((Map<?, ?>) loadedData).get("professions");
      if (!(professionData instanceof List)) {
        throw new IllegalStateException("Profession data must contain a 'professions' list.");
      }
      professions = new ArrayList<>();
      for (Object value : (List<?>) professionData) {
        if (!(value instanceof String) || ((String) value).isBlank()) {
          throw new IllegalStateException("Every profession must be a non-empty string.");
        }
        professions.add((String) value);
      }
    } catch (IOException e) {
      throw new IllegalStateException("Unable to read profession data.", e);
    }

    if (new HashSet<>(professions).size() < 3) {
      throw new IllegalStateException("Profession data must contain at least three unique values.");
    }

    Random random = new Random(1);
    Set<String> randomProfessions = new HashSet<>();
    while (randomProfessions.size() < 3) {
      String profession = professions.get(random.nextInt(professions.size()));
      randomProfessions.add(profession);
    }

    String[] randomProfessionsArray = randomProfessions.toArray(new String[3]);
    rectanglesToCustomer = new HashMap<>();
    String profession1 = randomProfessionsArray[0];
    System.out.println(profession1);
    String profession2 = randomProfessionsArray[1];
    String profession3 = randomProfessionsArray[2];

    rectanglesToCustomer.put(
        "rectPerson1", new Customer("Jon", 28, profession1, "/images/jon.png"));
    rectanglesToCustomer.put(
        "rectPerson2", new Customer("Jane", 34, profession2, "/images/jane.png"));
    rectanglesToCustomer.put(
        "rectPerson3", new Customer("Mark", 46, profession3, "/images/mark.png"));

    int randomNumber = random.nextInt(3);
    rectIdToGuess =
        randomNumber == 0 ? "rectPerson1" : ((randomNumber == 1) ? "rectPerson2" : "rectPerson3");
    professionToGuess = rectanglesToCustomer.get(rectIdToGuess).getProfession();
  }

  /**
   * Sets the current state of the game.
   *
   * @param state the new state to set
   */
  public void setState(GameState state) {
    if (state == gameOverState) {
      stopTimer();
    }

    this.gameState = state;
  }

  /**
   * Gets the initial game started state.
   *
   * @return the game started state
   */
  public GameState getGameStartedState() {
    return gameStartedState;
  }

  /**
   * Gets the guessing state.
   *
   * @return the guessing state
   */
  public GameState getGuessingState() {
    return guessingState;
  }

  /**
   * Gets the game over state.
   *
   * @return the game over state
   */
  public GameState getGameOverState() {
    return gameOverState;
  }

  /**
   * Gets the profession to be guessed.
   *
   * @return the profession to guess
   */
  public String getProfessionToGuess() {
    return professionToGuess;
  }

  /**
   * Gets the ID of the rectangle to be guessed.
   *
   * @return the rectangle ID to guess
   */
  public String getRectIdToGuess() {
    return rectIdToGuess;
  }

  /**
   * Gets the profession associated with a specific rectangle ID.
   *
   * @param rectangleId the rectangle ID
   * @return the profession associated with the rectangle ID
   */
  public String getProfession(String rectangleId) {
    return rectanglesToCustomer.get(rectangleId).getProfession();
  }

  /**
   * Gets the customer associated with a specific rectangle ID.
   *
   * @param rectangleId the rectangle ID
   * @return the customer associated with the rectangle ID
   */
  public Customer getCustomer(String rectangleId) {
    return rectanglesToCustomer.get(rectangleId);
  }

  /**
   * Handles the event when a rectangle is clicked.
   *
   * @param event the mouse event triggered by clicking a rectangle
   * @param rectangleId the ID of the clicked rectangle
   * @throws IOException if there is an I/O error
   */
  public void handleRectangleClick(MouseEvent event, String rectangleId) throws IOException {
    gameState.handleRectangleClick(event, rectangleId);
  }

  public String getImage(String rectangleId) {
    return rectanglesToCustomer.get(rectangleId).getImage();
  }

  /**
   * Handles the event when the guess button is clicked.
   *
   * @throws IOException if there is an I/O error
   */
  public void handleGuessClick() throws IOException {
    gameState.handleGuessClick();
  }

  public void startTimer() {
    if (gameState == gameOverState || gameTimer.getStatus() == Animation.Status.RUNNING) {
      return;
    }
    gameTimer.play();
  }

  public void stopTimer() {
    gameTimer.stop();
  }

  public ReadOnlyIntegerProperty elapsedSecondsProperty() {
    return elapsedSeconds;
  }
}
