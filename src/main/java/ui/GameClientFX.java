package ui;

import client.GameClient;
import common.NetworkConstants;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * JavaFX launcher for the Detective Game GUI client.
 * This class initializes the JavaFX application and integrates it with the existing GameClient.
 */
public class GameClientFX extends Application {

  private GameClient gameClient;
  private Thread gameClientThread;
  private MainController mainController;

  public static void main(String[] args) {
    launch(args);
  }

  @Override
  public void start(Stage primaryStage) throws Exception {
    // Load FXML
    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
    Parent root = loader.load();
    mainController = loader.getController();

    // Set up scene
    Scene scene = new Scene(root, 1280, 800);
    
    // Load CSS if available
    String cssPath = getClass().getResource("/css/detective.css") != null 
        ? getClass().getResource("/css/detective.css").toExternalForm() 
        : null;
    if (cssPath != null) {
      scene.getStylesheets().add(cssPath);
    }

    // Set up stage
    primaryStage.setTitle("Detective Game - JavaFX Client");
    primaryStage.setScene(scene);
    primaryStage.setOnCloseRequest(event -> {
      shutdown();
    });

    // Initialize GameClient
    String host = NetworkConstants.DEFAULT_HOST;
    int port = NetworkConstants.DEFAULT_PORT;

    // Check for command-line arguments
    Parameters params = getParameters();
    java.util.List<String> args = params.getRaw();
    if (args.size() >= 1) {
      host = args.get(0);
    }
    if (args.size() >= 2) {
      try {
        port = Integer.parseInt(args.get(1));
      } catch (NumberFormatException e) {
        System.err.println("Invalid port number, using default: " + port);
      }
    }

    gameClient = new GameClient(host, port);
    mainController.setGameClient(gameClient);

    // Start GameClient in a background thread
    gameClientThread = new Thread(() -> {
      try {
        gameClient.run();
      } catch (Exception e) {
        Platform.runLater(() -> {
          System.err.println("GameClient error: " + e.getMessage());
          e.printStackTrace();
        });
      }
    }, "GameClient-Thread");
    gameClientThread.setDaemon(true);
    gameClientThread.start();

    // Show the stage
    primaryStage.show();
    
    System.out.println("========================================");
    System.out.println("  Detective Game JavaFX Client Started");
    System.out.println("========================================");
    System.out.println("GUI initialized. You can use either:");
    System.out.println("  - The terminal at the bottom of the window");
    System.out.println("  - Or the GUI buttons and windows");
    System.out.println("========================================\n");
  }

  /**
   * Shutdown the application gracefully.
   */
  private void shutdown() {
    System.out.println("\nShutting down application...");
    
    if (gameClient != null) {
      gameClient.stopClient();
    }
    
    if (gameClientThread != null && gameClientThread.isAlive()) {
      try {
        gameClientThread.join(2000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
    
    Platform.exit();
    System.exit(0);
  }

  @Override
  public void stop() {
    shutdown();
  }
}
