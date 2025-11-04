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

/**
 * Journal window for the Detective Game.
 * Allows players to search journal entries and add new notes.
 */
public class JournalWindow {

  private Stage stage;
  private MainController mainController;
  private ListView<String> entriesListView;
  private TextField searchField;
  private TextArea noteTextArea;
  private java.util.List<String> allEntries;

  public JournalWindow(MainController controller) {
    this.mainController = controller;
    this.allEntries = new java.util.ArrayList<>();
    initializeWindow();
  }

  private void initializeWindow() {
    stage = new Stage();
    stage.setTitle("Journal");

    // Main layout
    BorderPane root = new BorderPane();
    root.setPadding(new Insets(10));
    root.setStyle("-fx-background-color: #1a1a1a;");

    // Top: Search bar
    VBox topBox = new VBox(5);
    Label searchLabel = new Label("Search Journal:");
    searchLabel.setStyle("-fx-text-fill: #d4af37; -fx-font-weight: bold;");
    
    searchField = new TextField();
    searchField.setPromptText("Enter keyword and press Enter...");
    searchField.setStyle("-fx-background-color: #2a2a2a; -fx-text-fill: #cccccc; -fx-prompt-text-fill: #666666;");
    searchField.setOnAction(e -> performSearch());
    
    topBox.getChildren().addAll(searchLabel, searchField);
    root.setTop(topBox);

    // Center: Entries list
    VBox centerBox = new VBox(5);
    centerBox.setPadding(new Insets(10, 0, 10, 0));
    
    Label entriesLabel = new Label("Journal Entries:");
    entriesLabel.setStyle("-fx-text-fill: #d4af37; -fx-font-weight: bold;");
    
    entriesListView = new ListView<>();
    entriesListView.setStyle("-fx-background-color: #0a0a0a; -fx-control-inner-background: #0a0a0a;");
    entriesListView.setPrefHeight(300);
    
    // Add some placeholder entries for testing
    allEntries.add("[Auto] Entered Ballroom - The grand ballroom is a scene of chaos.");
    allEntries.add("[Auto] Found clue: Shattered glass broken from inside");
    allEntries.add("[Note] Need to question Lord Ashworth about the cigar");
    updateEntriesList(allEntries);
    
    centerBox.getChildren().addAll(entriesLabel, entriesListView);
    VBox.setVgrow(entriesListView, javafx.scene.layout.Priority.ALWAYS);
    root.setCenter(centerBox);

    // Bottom: Add note section
    VBox bottomBox = new VBox(5);
    Label noteLabel = new Label("Add New Note:");
    noteLabel.setStyle("-fx-text-fill: #d4af37; -fx-font-weight: bold;");
    
    noteTextArea = new TextArea();
    noteTextArea.setPromptText("Type your note here...");
    noteTextArea.setPrefHeight(80);
    noteTextArea.setWrapText(true);
    noteTextArea.setStyle("-fx-control-inner-background: #0a0a0a; -fx-text-fill: #cccccc; -fx-prompt-text-fill: #666666;");
    
    Button addNoteButton = new Button("Add Note");
    addNoteButton.setStyle("-fx-background-color: #3a3a3a; -fx-text-fill: #d4af37; -fx-font-weight: bold;");
    addNoteButton.setOnAction(e -> addNote());
    
    HBox buttonBox = new HBox();
    buttonBox.setAlignment(Pos.CENTER_RIGHT);
    buttonBox.getChildren().add(addNoteButton);
    
    bottomBox.getChildren().addAll(noteLabel, noteTextArea, buttonBox);
    root.setBottom(bottomBox);

    // Create scene
    Scene scene = new Scene(root, 600, 500);
    
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
  }

  /**
   * Performs a search on journal entries.
   */
  private void performSearch() {
    String keyword = searchField.getText().trim();
    
    if (keyword.isEmpty()) {
      // Show all entries if search is empty
      updateEntriesList(allEntries);
    } else {
      // Send journal search command to game client
      if (mainController != null) {
        mainController.sendCommand("journal " + keyword);
      }
      
      // Filter entries locally as well
      java.util.List<String> filtered = allEntries.stream()
          .filter(entry -> entry.toLowerCase().contains(keyword.toLowerCase()))
          .collect(java.util.stream.Collectors.toList());
      updateEntriesList(filtered);
    }
  }

  /**
   * Adds a new note to the journal.
   */
  private void addNote() {
    String note = noteTextArea.getText().trim();
    
    if (note.isEmpty()) {
      Alert alert = new Alert(Alert.AlertType.WARNING);
      alert.setTitle("Empty Note");
      alert.setHeaderText(null);
      alert.setContentText("Please enter a note before adding.");
      alert.showAndWait();
      return;
    }
    
    // Send journal add command to game client
    if (mainController != null) {
      mainController.sendCommand("journal add " + note);
    }
    
    // Add to local list
    String entry = "[Note] " + note;
    allEntries.add(entry);
    updateEntriesList(allEntries);
    
    // Clear the text area
    noteTextArea.clear();
    
    // Show confirmation
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.setTitle("Note Added");
    alert.setHeaderText(null);
    alert.setContentText("Your note has been added to the journal.");
    alert.showAndWait();
  }

  /**
   * Updates the entries list view with the given entries.
   */
  private void updateEntriesList(java.util.List<String> entries) {
    entriesListView.getItems().clear();
    entriesListView.getItems().addAll(entries);
  }

  /**
   * Adds an entry to the journal (called externally).
   */
  public void addEntry(String entry) {
    allEntries.add(entry);
    updateEntriesList(allEntries);
  }

  /**
   * Shows the journal window.
   */
  public void show() {
    if (stage != null) {
      stage.show();
      stage.toFront();
    }
  }

  /**
   * Hides the journal window.
   */
  public void hide() {
    if (stage != null) {
      stage.hide();
    }
  }
}
