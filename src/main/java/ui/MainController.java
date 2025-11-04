package ui;

import java.io.PrintStream;
import java.util.List;
import client.GameClient;
import common.NetworkConstants;
import common.dto.RoomDescriptionDTO;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import server.ServerMain;
import singleplayer.SinglePlayerMain;
import ui.util.GameOutputParser;
import ui.util.RoomView;
import ui.util.TextAreaOutputStream;
import ui.windows.ChatWindow;
import ui.windows.JournalWindow;
import ui.windows.TasksWindow;

public class MainController {

  private enum UIState {
    MENU,
    GAME_SINGLE,
    GAME_MULTI
  }

  @FXML private BorderPane mainBorderPane;
  @FXML private Button tasksButton;
  @FXML private Button journalButton;
  @FXML private Button chatButton;
  @FXML private Label unreadChatLabel;
  @FXML private StackPane roomPane;
  @FXML private VBox rightInfoPanel;
  @FXML private TextArea terminalTextArea;
  @FXML private TextField terminalInputField;
  @FXML private Label statusLabel;
  @FXML private SplitPane bottomSplitPane;

  private GameClient gameClient;
  private Thread gameClientThread;
  private JournalWindow journalWindow;
  private ChatWindow chatWindow;
  private TasksWindow tasksWindow;
  private RoomView roomView;
  private int unreadChatCount = 0;

  private VBox mainMenuVBox;
  private List<String> launchArgs;
  private HostServices hostServices;
  private UIState currentState = UIState.MENU;

  @FXML
  public void initialize() {
    terminalTextArea.setEditable(false);
    terminalTextArea.setWrapText(true);
    terminalInputField.setOnAction(event -> handleTerminalInput());
    tasksButton.setOnAction(event -> openTasksWindow());
    journalButton.setOnAction(event -> openJournalWindow());
    chatButton.setOnAction(event -> openChatWindow());
    updateStatus("GUI Ready. Please select a game mode.");
    unreadChatLabel.setVisible(false);
    bottomSplitPane.setDividerPositions(0.7);

    roomView = new RoomView(this);
    roomPane.getChildren().add(roomView);

    // Redirect System.out to the terminal TextArea
    TextAreaOutputStream taos = new TextAreaOutputStream(terminalTextArea);
    GameOutputParser parser = new GameOutputParser(this);
    taos.setParser(parser);
    System.setOut(new PrintStream(taos, true));

    createMainMenu();
    updateUIVisibility();
  }

  public void setLaunchArgs(List<String> args) {
      this.launchArgs = args;
  }

  public void setHostServices(HostServices hostServices) {
      this.hostServices = hostServices;
  }

  private void createMainMenu() {
    mainMenuVBox = new VBox(15);
    mainMenuVBox.setAlignment(Pos.CENTER);
    mainMenuVBox.getStyleClass().add("main-menu-container");

    Button singlePlayerButton = new Button("Single Player");
    singlePlayerButton.getStyleClass().add("main-menu-button");
    singlePlayerButton.setOnAction(event -> startSinglePlayer());

    Button multiplayerButton = new Button("Multiplayer (Join/Host)");
    multiplayerButton.getStyleClass().add("main-menu-button");
    multiplayerButton.setOnAction(event -> startMultiplayer());

    Button startServerButton = new Button("Start Server Only");
    startServerButton.getStyleClass().add("main-menu-button");
    startServerButton.setOnAction(event -> startServer());

    Button quitButton = new Button("Quit");
    quitButton.getStyleClass().add("main-menu-button");
    quitButton.setOnAction(event -> shutdown());

    mainMenuVBox
        .getChildren()
        .addAll(singlePlayerButton, multiplayerButton, startServerButton, quitButton);
  }

  private void updateUIVisibility() {
    Platform.runLater(() -> {
      switch (currentState) {
        case MENU:
          roomPane.getChildren().clear();
          roomPane.getChildren().add(mainMenuVBox);
          tasksButton.setVisible(false);
          journalButton.setVisible(false);
          chatButton.setVisible(false);
          rightInfoPanel.setVisible(false);
          break;
        case GAME_SINGLE:
          roomPane.getChildren().clear();
          roomPane.getChildren().add(roomView);
          tasksButton.setVisible(true);
          journalButton.setVisible(true);
          chatButton.setVisible(false);
          rightInfoPanel.setVisible(true);
          break;
        case GAME_MULTI:
          roomPane.getChildren().clear();
          roomPane.getChildren().add(roomView);
          tasksButton.setVisible(true);
          journalButton.setVisible(true);
          chatButton.setVisible(true);
          rightInfoPanel.setVisible(true);
          break;
      }
    });
  }

  private void startSinglePlayer() {
    updateStatus("Starting Single Player...");
    Thread singlePlayerThread = new Thread(() -> {
      try {
        SinglePlayerMain.main(launchArgs.toArray(new String[0]));
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        currentState = UIState.MENU;
        updateUIVisibility();
      }
    });
    singlePlayerThread.setDaemon(true);
    singlePlayerThread.start();
    currentState = UIState.GAME_SINGLE;
    updateUIVisibility();
  }

  private void startMultiplayer() {
    updateStatus("Starting Multiplayer Client...");
    String host = getLaunchArg(0, NetworkConstants.DEFAULT_HOST);
    int port = getLaunchArg(1, NetworkConstants.DEFAULT_PORT);

    gameClient = new GameClient(host, port);

    gameClientThread = new Thread(() -> {
      try {
        gameClient.run();
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        currentState = UIState.MENU;
        updateUIVisibility();
      }
    }, "GameClient-Thread");
    gameClientThread.setDaemon(true);
    gameClientThread.start();
    currentState = UIState.GAME_MULTI;
    updateUIVisibility();
  }

  private void startServer() {
      updateStatus("Starting Game Server...");
      Thread serverThread = new Thread(() -> {
          try {
              ServerMain.main(launchArgs.toArray(new String[0]));
          } catch (Exception e) {
              e.printStackTrace();
          }
      });
      serverThread.setDaemon(true);
      serverThread.start();
      terminalTextArea.appendText("Server started in background. You can now start a multiplayer client.\n");
  }

  private String getLaunchArg(int index, String defaultValue) {
    if (launchArgs != null && launchArgs.size() > index) {
      return launchArgs.get(index);
    }
    return defaultValue;
  }

  private int getLaunchArg(int index, int defaultValue) {
    if (launchArgs != null && launchArgs.size() > index) {
      try {
        return Integer.parseInt(launchArgs.get(index));
      } catch (NumberFormatException e) {
        // Ignore
      }
    }
    return defaultValue;
  }

  public void shutdown() {
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

  @FXML
  private void handleTerminalInput() {
    String input = terminalInputField.getText().trim();
    if (!input.isEmpty()) {
      terminalTextArea.appendText("> " + input + "\n");
      if (gameClient != null) {
        gameClient.enqueueUserInput(input);
      }
      terminalInputField.clear();
    }
  }

  private void openTasksWindow() {
    if (tasksWindow == null) {
      tasksWindow = new TasksWindow();
    }
    tasksWindow.show();
  }

  private void openJournalWindow() {
    if (journalWindow == null) {
      journalWindow = new JournalWindow(this);
    }
    journalWindow.show();
  }

  private void openChatWindow() {
    if (chatWindow == null) {
      chatWindow = new ChatWindow(this);
    }
    chatWindow.show();
    unreadChatCount = 0;
    updateUnreadChatLabel();
  }

  public void incrementUnreadChat() {
    unreadChatCount++;
    updateUnreadChatLabel();
  }

  private void updateUnreadChatLabel() {
    if (unreadChatCount > 0) {
      unreadChatLabel.setText(String.valueOf(unreadChatCount));
      unreadChatLabel.setVisible(true);
    } else {
      unreadChatLabel.setVisible(false);
    }
  }

  public void updateStatus(String status) {
    if (statusLabel != null) {
      statusLabel.setText(status);
    }
  }

  public TextArea getTerminalTextArea() {
    return terminalTextArea;
  }

  public StackPane getRoomPane() {
    return roomPane;
  }

  public VBox getRightInfoPanel() {
    return rightInfoPanel;
  }

  public void sendCommand(String command) {
    if (gameClient != null) {
      gameClient.enqueueUserInput(command);
    }
  }

  public GameClient getGameClient() {
    return gameClient;
  }

  public void updateRoomView(RoomDescriptionDTO roomDescription) {
    if (roomView != null && roomDescription != null) {
      Platform.runLater(() -> {
        roomView.loadRoom(roomDescription);
        updateRightPanel(roomDescription);
        updateStatus("Current room: " + roomDescription.getName());
      });
    }
  }

  private void updateRightPanel(RoomDescriptionDTO roomDescription) {
    // This can be expanded later.
  }

  public void showRoomResponse(String targetName, String response) {
    if (roomView != null) {
      Platform.runLater(() -> {
        roomView.showResponseBubble(targetName, response);
      });
    }
  }

  public void addJournalEntry(String entry) {
    if (journalWindow != null) {
      Platform.runLater(() -> {
        journalWindow.addEntry(entry);
      });
    }
  }

  public void addChatMessage(String sender, String message) {
    if (chatWindow != null) {
      Platform.runLater(() -> {
        chatWindow.addChatMessage(sender, message);
      });
    }
  }
}
