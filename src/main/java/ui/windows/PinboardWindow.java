package ui.windows;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import common.dto.JournalEntryDTO;
import java.util.List;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import singleplayer.PinboardState;
import javafx.scene.layout.Pane;
import javafx.animation.PauseTransition;
import javafx.collections.ListChangeListener;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.util.Duration;
import ui.MainController;
import ui.util.ConnectionLine;
import ui.util.EvidenceCard;
import ui.util.StickyNote;

public class PinboardWindow {

    private Stage stage;
    private MainController mainController;
    private Pane canvas;
    private boolean isConnecting = false;
    private Region startNode = null;
    private boolean isHighlighting = false;
    private double currentScale = 1.0;

    public PinboardWindow(MainController controller) {
        this.mainController = controller;
        initializeWindow();
    }

    private void initializeWindow() {
        stage = new Stage();
        stage.setTitle("Detective Pinboard");

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));
        root.setStyle("-fx-background-color: #f5f5dc;"); // A corkboard-like color

        HBox toolbar = new HBox(10);
        toolbar.setPadding(new Insets(0, 0, 10, 0));
        Button newNoteButton = new Button("New Note");
        newNoteButton.setOnAction(e -> createStickyNote());
        Button connectButton = new Button("Connect");
        connectButton.setOnAction(e -> isConnecting = true);
        Button clearConnectionsButton = new Button("Clear Connections");
        clearConnectionsButton.setOnAction(e -> clearConnections());
        Button saveButton = new Button("Save");
        saveButton.setOnAction(e -> savePinboardState());
        Button loadButton = new Button("Load");
        loadButton.setOnAction(e -> loadPinboardState());
        ToggleButton highlightButton = new ToggleButton("Highlight");
        highlightButton.setOnAction(e -> isHighlighting = highlightButton.isSelected());
        Button zoomInButton = new Button("Zoom In");
        zoomInButton.setOnAction(e -> zoom(1.1));
        Button zoomOutButton = new Button("Zoom Out");
        zoomOutButton.setOnAction(e -> zoom(0.9));
        Button clearButton = new Button("Clear Board");
        clearButton.setOnAction(e -> clearBoard());
        toolbar.getChildren().addAll(newNoteButton, connectButton, clearConnectionsButton, saveButton, loadButton,
                highlightButton, zoomInButton, zoomOutButton, clearButton);
        root.setTop(toolbar);

        canvas = new Pane();
        canvas.setStyle("-fx-background-color: #f5f5dc;");
        root.setCenter(canvas);

        root.setRight(createFinalExamTemplate());

        Scene scene = new Scene(root, 1280, 768);
        stage.setScene(scene);
        setupAutoSave();
    }

    public void show() {
        if (stage != null) {
            loadEvidence();
            loadPinboardState();
            stage.show();
            stage.toFront();
        }
    }

    public void hide() {
        if (stage != null) {
            stage.hide();
        }
    }

    private void setupAutoSave() {
        PauseTransition saveDebounce = new PauseTransition(Duration.seconds(1));
        saveDebounce.setOnFinished(event -> savePinboardState());

        canvas.getChildren().addListener((ListChangeListener<javafx.scene.Node>) c -> {
            while (c.next()) {
                if (c.wasAdded() || c.wasRemoved()) {
                    saveDebounce.playFromStart();
                }
            }
        });
    }

    private void loadEvidence() {
        canvas.getChildren().removeIf(node -> node instanceof EvidenceCard);
        List<JournalEntryDTO> entries = null;
        if (mainController.getGameClient() != null) {
            entries = mainController.getGameClient().getJournalEntries();
        } else if (mainController.getSinglePlayerGame() != null) {
            entries = mainController.getSinglePlayerGame().getGameContext().getJournalEntries(null);
        }

        if (entries != null) {
            for (int i = 0; i < entries.size(); i++) {
                JournalEntryDTO entry = entries.get(i);
                String text = entry.getText();
                String[] lines = text.split("\n", 2);
                String title = lines[0];
                String summary = lines.length > 1 ? lines[1] : "";
                EvidenceCard card = new EvidenceCard(entry.getId(), title, summary);
                card.setLayoutX(10 + (i * 210) % 800);
                card.setLayoutY(10 + ((i / 4) * 110));
                makeDraggable(card);
                canvas.getChildren().add(card);
            }
        }
    }

    private void createStickyNote() {
        StickyNote stickyNote = new StickyNote();
        makeDraggable(stickyNote);
        canvas.getChildren().add(stickyNote);
    }

    private void makeDraggable(javafx.scene.Node node) {
        final double[] mouseX = new double[1];
        final double[] mouseY = new double[1];

        node.setOnMousePressed(event -> {
            if (isConnecting) {
                if (startNode == null) {
                    startNode = (Region) node;
                } else {
                    finishConnection((Region) node);
                }
            } else if (isHighlighting) {
                toggleHighlight((Region) node);
            } else {
                mouseX[0] = event.getSceneX() - node.getLayoutX();
                mouseY[0] = event.getSceneY() - node.getLayoutY();
            }
        });

        node.setOnMouseDragged(event -> {
            if (!isConnecting) {
                node.setLayoutX(event.getSceneX() - mouseX[0]);
                node.setLayoutY(event.getSceneY() - mouseY[0]);
            }
        });
    }

    private void finishConnection(Region endNode) {
        ConnectionLine line = new ConnectionLine(startNode, endNode);
        canvas.getChildren().add(0, line);
        isConnecting = false;
        startNode = null;
    }

    private void clearConnections() {
        canvas.getChildren().removeIf(node -> node instanceof ConnectionLine);
    }

    private void clearBoard() {
        canvas.getChildren().clear();
    }

    private void zoom(double factor) {
        currentScale *= factor;
        canvas.setScaleX(currentScale);
        canvas.setScaleY(currentScale);
    }

    private void toggleHighlight(Region node) {
        if (node.getStyle().contains("-fx-border-color: yellow;")) {
            node.setStyle(node.getStyle().replace("-fx-border-color: yellow;", "-fx-border-color: black;"));
        } else {
            node.setStyle(node.getStyle().replace("-fx-border-color: black;", "-fx-border-color: yellow;"));
        }
    }

    private TitledPane createFinalExamTemplate() {
        VBox templateContent = new VBox(10);
        templateContent.setPadding(new Insets(10));
        GridPane grid = new GridPane();
        grid.setVgap(5);
        grid.setHgap(10);

        String[] labels = {
            "Main Suspect(s)", "Primary Motive(s)", "Opportunity / Means",
            "Weapon (if identifiable)", "Key Contradictions Noticed",
            "Supporting Evidence", "Suspicious Behavior or Lies",
            "Alibi Verification Notes", "Remaining Questions"
        };

        for (int i = 0; i < labels.length; i++) {
            grid.add(new Label(labels[i]), 0, i);
            TextField textField = new TextField();
            GridPane.setHgrow(textField, Priority.ALWAYS);
            grid.add(textField, 1, i);
        }

        templateContent.getChildren().add(grid);
        TitledPane titledPane = new TitledPane("Case Summary Template", templateContent);
        titledPane.setAnimated(true);
        titledPane.setExpanded(true);
        return titledPane;
    }

    private void savePinboardState() {
        List<PinboardState.CardState> cardStates = new ArrayList<>();
        List<PinboardState.NoteState> noteStates = new ArrayList<>();
        List<PinboardState.ConnectionState> connectionStates = new ArrayList<>();
        List<Region> nodes = new ArrayList<>();

        for (javafx.scene.Node node : canvas.getChildren()) {
            if (node instanceof EvidenceCard) {
                EvidenceCard card = (EvidenceCard) node;
                cardStates.add(new PinboardState.CardState(card.getEvidenceId().toString(), card.getLayoutX(), card.getLayoutY()));
                nodes.add(card);
            } else if (node instanceof StickyNote) {
                StickyNote note = (StickyNote) node;
                noteStates.add(new PinboardState.NoteState(note.getStickyNoteId().toString(), note.getText(), note.getLayoutX(), note.getLayoutY()));
                nodes.add(note);
            }
        }

        for (javafx.scene.Node node : canvas.getChildren()) {
            if (node instanceof ConnectionLine) {
                ConnectionLine line = (ConnectionLine) node;
                String startId = getNodeId(line.getStartNode());
                String endId = getNodeId(line.getEndNode());
                if (startId != null && endId != null) {
                    connectionStates.add(new PinboardState.ConnectionState(startId, endId));
                }
            }
        }

        PinboardState state = new PinboardState(cardStates, noteStates, connectionStates);
        ObjectMapper mapper = new ObjectMapper();
        try {
            String caseName = mainController.getSelectedCaseFile().getUniversalTitle();
            String fileName = "pinboard_state_" + caseName.replaceAll("\\s+", "_") + ".json";
            File file = new File(System.getProperty("user.home"), ".detective_game" + File.separator + "pinboards");
            file.mkdirs();
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(file, fileName), state);
        } catch (IOException e) {
            showErrorAlert("Error Saving Pinboard", "Could not save the pinboard state to a file.");
        }
    }

    private void loadPinboardState() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            String caseName = mainController.getSelectedCaseFile().getUniversalTitle();
            String fileName = "pinboard_state_" + caseName.replaceAll("\\s+", "_") + ".json";
            File file = new File(System.getProperty("user.home"), ".detective_game" + File.separator + "pinboards" + File.separator + fileName);
            if (file.exists()) {
                PinboardState state = mapper.readValue(file, PinboardState.class);
                canvas.getChildren().clear();
                loadEvidence();

                List<Region> nodes = new ArrayList<>();
                for (PinboardState.CardState cardState : state.getCards()) {
                    for (javafx.scene.Node node : canvas.getChildren()) {
                        if (node instanceof EvidenceCard && ((EvidenceCard) node).getEvidenceId().toString().equals(cardState.getId())) {
                            node.setLayoutX(cardState.getX());
                            node.setLayoutY(cardState.getY());
                            nodes.add((Region) node);
                            break;
                        }
                    }
                }

                for (PinboardState.NoteState noteState : state.getNotes()) {
                    StickyNote stickyNote = new StickyNote(noteState.getId(), noteState.getText());
                    stickyNote.setLayoutX(noteState.getX());
                    stickyNote.setLayoutY(noteState.getY());
                    makeDraggable(stickyNote);
                    canvas.getChildren().add(stickyNote);
                    nodes.add(stickyNote);
                }

                for (PinboardState.ConnectionState connectionState : state.getConnections()) {
                    Region start = (Region) getNodeById(connectionState.getStartNodeId());
                    Region end = (Region) getNodeById(connectionState.getEndNodeId());
                    if (start != null && end != null) {
                        ConnectionLine line = new ConnectionLine(start, end);
                        canvas.getChildren().add(0, line);
                    }
                }
            }
        } catch (IOException e) {
            showErrorAlert("Error Loading Pinboard", "Could not load the pinboard state from a file.");
        }
    }

    private void showErrorAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private String getNodeId(javafx.scene.Node node) {
        if (node instanceof EvidenceCard) {
            return ((EvidenceCard) node).getEvidenceId().toString();
        } else if (node instanceof StickyNote) {
            return ((StickyNote) node).getStickyNoteId().toString();
        }
        return null;
    }

    private javafx.scene.Node getNodeById(String id) {
        for (javafx.scene.Node node : canvas.getChildren()) {
            if (node instanceof EvidenceCard && ((EvidenceCard) node).getEvidenceId().toString().equals(id)) {
                return node;
            } else if (node instanceof StickyNote && ((StickyNote) node).getStickyNoteId().toString().equals(id)) {
                return node;
            }
        }
        return null;
    }
}
