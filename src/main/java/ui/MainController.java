package ui;

import client.GameClient;
import common.dto.RoomDescriptionDTO;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import ui.util.RoomView;
import ui.util.TextAreaOutputStream;
import ui.windows.ChatWindow;
import ui.windows.JournalWindow;
import ui.windows.TasksWindow;

import java.io.PrintStream;

/**
 * Main controller for the Detective Game JavaFX GUI.
 * Manages the BorderPane layout with terminal, room view, and window buttons.
 */
public class MainController {

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
  private JournalWindow journalWindow;
  private ChatWindow chatWindow;
  private TasksWindow tasksWindow;
  private RoomView roomView;
  private int unreadChatCount = 0;

  /**
   * Initialize method called by FXML loader after UI components are loaded.
   */
  @FXML
  public void initialize() {
    // Set terminal to be non-editable
    terminalTextArea.setEditable(false);
    terminalTextArea.setWrapText(true);

    // Set up Enter key handler for terminal input
    terminalInputField.setOnAction(event -> handleTerminalInput());

    // Initialize button handlers
    tasksButton.setOnAction(event -> openTasksWindow());
    journalButton.setOnAction(event -> openJournalWindow());
    chatButton.setOnAction(event -> openChatWindow());

    // Initialize status
    updateStatus("GUI Ready - Waiting for client connection...");

    // Hide unread chat label initially
    unreadChatLabel.setVisible(false);

    // Set up split pane divider position (70% terminal, 30% status)
    bottomSplitPane.setDividerPositions(0.7);

    // Initialize RoomView
    roomView = new RoomView(this);
    roomPane.getChildren().clear();
    roomPane.getChildren().add(roomView);
  }

  /**
   * Sets the GameClient instance and redirects its console output to the GUI.
   */
  public void setGameClient(GameClient client) {
    this.gameClient = client;

    // Redirect System.out to the terminal TextArea
    TextAreaOutputStream taos = new TextAreaOutputStream(terminalTextArea);
    PrintStream ps = new PrintStream(taos, true);
    System.setOut(ps);

    updateStatus("Connected to game client");
  }

  /**
   * Handles user input from the terminal text field.
   */
  @FXML
  private void handleTerminalInput() {
    String input = terminalInputField.getText().trim();
    if (!input.isEmpty()) {
      // Echo the input to the terminal
      terminalTextArea.appendText("> " + input + "\n");

      // Send to GameClient if available
      if (gameClient != null) {
        gameClient.enqueueUserInput(input);
      }

      // Clear input field
      terminalInputField.clear();
    }
  }

  /**
   * Opens the Tasks window.
   */
  private void openTasksWindow() {
    if (tasksWindow == null) {
      tasksWindow = new TasksWindow();
    }
    tasksWindow.show();
  }

  /**
   * Opens the Journal window.
   */
  private void openJournalWindow() {
    if (journalWindow == null) {
      journalWindow = new JournalWindow(this);
    }
    journalWindow.show();
  }

  /**
   * Opens the Chat window and resets unread count.
   */
  private void openChatWindow() {
    if (chatWindow == null) {
      chatWindow = new ChatWindow(this);
    }
    chatWindow.show();

    // Reset unread count when opening chat
    unreadChatCount = 0;
    updateUnreadChatLabel();
  }

  /**
   * Increments the unread chat message counter.
   */
  public void incrementUnreadChat() {
    unreadChatCount++;
    updateUnreadChatLabel();
  }

  /**
   * Updates the unread chat label visibility and text.
   */
  private void updateUnreadChatLabel() {
    if (unreadChatCount > 0) {
      unreadChatLabel.setText(String.valueOf(unreadChatCount));
      unreadChatLabel.setVisible(true);
    } else {
      unreadChatLabel.setVisible(false);
    }
  }

  /**
   * Updates the status label.
   */
  public void updateStatus(String status) {
    if (statusLabel != null) {
      statusLabel.setText(status);
    }
  }

  /**
   * Gets the terminal TextArea for external updates.
   */
  public TextArea getTerminalTextArea() {
    return terminalTextArea;
  }

  /**
   * Gets the room pane for displaying room visualizations.
   */
  public StackPane getRoomPane() {
    return roomPane;
  }

  /**
   * Gets the right info panel for displaying room info.
   */
  public VBox getRightInfoPanel() {
    return rightInfoPanel;
  }

  /**
   * Sends a command through the game client.
   */
  public void sendCommand(String command) {
    if (gameClient != null) {
      gameClient.enqueueUserInput(command);
    }
  }

  /**
   * Gets the game client instance.
   */
  public GameClient getGameClient() {
    return gameClient;
  }
}
