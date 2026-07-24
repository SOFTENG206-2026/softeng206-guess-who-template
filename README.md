# SOFTENG 2026 - Sample JavaFX application using Proxy API

> This is a functional prototype, not a reference architecture or a finished game. It deliberately
> leaves design and engineering improvements for students to identify and implement.

> you do not need to implement this game, this is only a starter code you need to implement the Time Mistery project explaiend here [https://softeng206design.digitaledu.ac.nz/project/](https://softeng206design.digitaledu.ac.nz/project/)

## To setup the API to access Chat Completions and TTS

- add in the root of the project (i.e., the same level where `pom.xml` is located) a file named `apiproxy.config`
- put inside the credentials that you received from no-reply@digitaledu.ac.nz (put the quotes "")

  ```
  email: "UPI@aucklanduni.ac.nz"
  apiKey: "YOUR_KEY"
  ```
  These are your credentials to invoke the APIs. 

  The starter supports `GPT_5_4_NANO` for chat completions and OpenAI voices for
  Text-to-Speech.

  The token credits are charged as follows:
  - 4 token credits per character for OpenAI Text-to-Speech.
  - 1 token credit per 1 token for OpenAI Chat Completions (as determined by OpenAI, charging both input and output tokens).


## To setup codestyle's API

- add in the root of the project (i.e., the same level where `pom.xml` is located) a file named `codestyle.config`
- put inside the credentials that you received from gradestyle@digitaledu.ac.nz (put the quotes "")

  ```
  email: "UPI@aucklanduni.ac.nz"
  accessToken: "YOUR_KEY"
  ```

 these are your credentials to invoke gradestyle

## To run the game

`./mvnw clean javafx:run`

## To debug the game

`./mvnw clean javafx:run@debug` then in VS Code "Run & Debug", then run "Debug JavaFX"

## To run codestyle

`./mvnw clean compile exec:java@style`

## To run the test

The test calls the API proxy and consumes token credits.

`./mvnw test`
