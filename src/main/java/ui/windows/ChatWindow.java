package ui.windows;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import ui.MainController;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Chat window for the Detective Game multiplayer mode.
 * Allows players to communicate with each other during the game.
 */
public class ChatWindow {

  private Stage stage;
  private MainController mainController;
  private ListView<String> chatListView;
  private TextField chatInputField;
  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

  public ChatWindow(MainController controller) {
    this.mainController = controller;
    initializeWindow();
  }

  private void initializeWindow() {
    stage = new Stage();
    stage.setTitle("Chat");

    // Main layout
    BorderPane root = new BorderPane();
    root.setPadding(new Insets(10));
    root.setStyle("-fx-background-color: #1a1a1a;");

    // Center: Chat history
    VBox centerBox = new VBox(5);
    
    Label chatLabel = new Label("Chat History:");
    chatLabel.setStyle("-fx-text-fill: #d4af37; -fx-font-weight: bold; -fx-font-size: 14;");
    
    chatListView = new ListView<>();
    chatListView.setStyle("-fx-background-color: #0a0a0a; -fx-control-inner-background: #0a0a0a;");
    chatListView.setPrefHeight(400);
    
    // Add welcome message
    addChatMessage("[SYSTEM]", "Chat window opened. Type your message below and press Enter.");
    
    centerBox.getChildren().addAll(chatLabel, chatListView);
    VBox.setVgrow(chatListView, javafx.scene.layout.Priority.ALWAYS);
    root.setCenter(centerBox);

    // Bottom: Chat input
    VBox bottomBox = new VBox(5);
    bottomBox.setPadding(new Insets(10, 0, 0, 0));
    
    Label inputLabel = new Label("Send Message:");
    inputLabel.setStyle("-fx-text-fill: #d4af37; -fx-font-weight: bold;");
    
    HBox inputBox = new HBox(10);
    inputBox.setAlignment(Pos.CENTER_LEFT);
    
    chatInputField = new TextField();
    chatInputField.setPromptText("Type your message...");
    chatInputField.setStyle("-fx-background-color: #2a2a2a; -fx-text-fill: #cccccc; -fx-prompt-text-fill: #666666;");
    chatInputField.setOnAction(e -> sendChatMessage());
    HBox.setHgrow(chatInputField, javafx.scene.layout.Priority.ALWAYS);
    
    Button sendButton = new Button("Send");
    sendButton.setStyle("-fx-background-color: #3a3a3a; -fx-text-fill: #d4af37; -fx-font-weight: bold;");
    sendButton.setOnAction(e -> sendChatMessage());
    
    inputBox.getChildren().addAll(chatInputField, sendButton);
    
    bottomBox.getChildren().addAll(inputLabel, inputBox);
    root.setBottom(bottomBox);

    // Create scene
    Scene scene = new Scene(root, 500, 500);
    
    // Apply CSS if available
    if (mainController != null) {
      String cssPath = getClass().getResource("/css/detective.css") != null 
          ? getClass().getResource("/css/detective.css").toExternalForm() 
          : null;
      if (cssPath != null) {
        scene.getStylesheets().add(cssPath);
      }
    }
    
    stage.setScene(scene);
    
    // When window is shown, reset unread count
    stage.setOnShown(e -> {
      if (mainController != null) {
        // The MainController handles resetting unread count when opening
      }
    });
  }

  /**
   * Sends a chat message to the game client.
   */
  private void sendChatMessage() {
    String message = chatInputField.getText().trim();
    
    if (message.isEmpty()) {
      return;
    }
    
    // Send chat command to game client (no need to prefix with /chat, the GUI does that)
    if (mainController != null) {
      mainController.sendCommand("/chat " + message);
    }
    
    // Add to local chat display (will also come back from server)
    String timestamp = LocalTime.now().format(TIME_FORMATTER);
    chatListView.getItems().add("[" + timestamp + "] You: " + message);
    scrollToBottom();
    
    // Clear input field
    chatInputField.clear();
  }

  /**
   * Adds a chat message to the display (called externally when receiving messages).
   */
  public void addChatMessage(String sender, String message) {
    String timestamp = LocalTime.now().format(TIME_FORMATTER);
    String formattedMessage = "[" + timestamp + "] " + sender + ": " + message;
    chatListView.getItems().add(formattedMessage);
    scrollToBottom();
    
    // If window is not showing, increment unread count
    if (!stage.isShowing() && mainController != null) {
      mainController.incrementUnreadChat();
    }
  }

  /**
   * Scrolls the chat list to the bottom.
   */
  private void scrollToBottom() {
    if (chatListView.getItems().size() > 0) {
      chatListView.scrollTo(chatListView.getItems().size() - 1);
    }
  }

  /**
   * Shows the chat window.
   */
  public void show() {
    if (stage != null) {
      stage.show();
      stage.toFront();
      chatInputField.requestFocus();
    }
  }

  /**
   * Hides the chat window.
   */
  public void hide() {
    if (stage != null) {
      stage.hide();
    }
  }

  /**
   * Checks if the chat window is currently showing.
   */
  public boolean isShowing() {
    return stage != null && stage.isShowing();
  }
}
