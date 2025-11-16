package ui.util;

import java.util.UUID;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.Cursor;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class StickyNote extends VBox {

    private final UUID id;
    private final TextArea textArea;

    public StickyNote() {
        this.id = UUID.randomUUID();
        this.textArea = new TextArea();
        textArea.setWrapText(true);

        Label dragHandle = new Label("Drag here");
        dragHandle.setStyle("-fx-background-color: #f0f0f0; -fx-padding: 2 5;");
        dragHandle.setPrefWidth(Double.MAX_VALUE);

        setPadding(new Insets(5));
        setStyle("-fx-background-color: #ffff99; -fx-border-color: #000; -fx-border-width: 1;");
        setPrefSize(250, 200);

        // Delete button
        Button deleteButton = new Button("X");
        deleteButton.setStyle("-fx-background-color: transparent; -fx-text-fill: red; -fx-font-weight: bold;");
        deleteButton.setOnAction(event -> {
            Pane parent = (Pane) this.getParent();
            if (parent != null) {
                parent.getChildren().remove(this);
            }
        });

        StackPane header = new StackPane(dragHandle, deleteButton);
        StackPane.setAlignment(deleteButton, Pos.TOP_RIGHT);

        // Resize handle
        Rectangle resizeHandle = new Rectangle(10, 10, Color.BLACK);
        resizeHandle.setCursor(Cursor.SE_RESIZE);

        double[] startDrag = new double[2];

        resizeHandle.setOnMousePressed(event -> {
            startDrag[0] = getPrefWidth() - event.getX();
            startDrag[1] = getPrefHeight() - event.getY();
        });

        resizeHandle.setOnMouseDragged(event -> {
            setPrefWidth(event.getX() + startDrag[0]);
            setPrefHeight(event.getY() + startDrag[1]);
        });

        StackPane footer = new StackPane(resizeHandle);
        footer.setAlignment(Pos.BOTTOM_RIGHT);

        getChildren().addAll(header, textArea, footer);
    }

    public StickyNote(String id, String text) {
        this.id = UUID.fromString(id);
        this.textArea = new TextArea(text);
        textArea.setWrapText(true);

        Label dragHandle = new Label("Drag here");
        dragHandle.setStyle("-fx-background-color: #f0f0f0; -fx-padding: 2 5;");
        dragHandle.setPrefWidth(Double.MAX_VALUE);

        setPadding(new Insets(5));
        setStyle("-fx-background-color: #ffff99; -fx-border-color: #000; -fx-border-width: 1;");
        setPrefSize(250, 200);

        // Delete button
        Button deleteButton = new Button("X");
        deleteButton.setStyle("-fx-background-color: transparent; -fx-text-fill: red; -fx-font-weight: bold;");
        deleteButton.setOnAction(event -> {
            Pane parent = (Pane) this.getParent();
            if (parent != null) {
                parent.getChildren().remove(this);
            }
        });

        StackPane header = new StackPane(dragHandle, deleteButton);
        StackPane.setAlignment(deleteButton, Pos.TOP_RIGHT);

        // Resize handle
        Rectangle resizeHandle = new Rectangle(10, 10, Color.BLACK);
        resizeHandle.setCursor(Cursor.SE_RESIZE);

        double[] startDrag = new double[2];

        resizeHandle.setOnMousePressed(event -> {
            startDrag[0] = getPrefWidth() - event.getX();
            startDrag[1] = getPrefHeight() - event.getY();
        });

        resizeHandle.setOnMouseDragged(event -> {
            setPrefWidth(event.getX() + startDrag[0]);
            setPrefHeight(event.getY() + startDrag[1]);
        });

        StackPane footer = new StackPane(resizeHandle);
        footer.setAlignment(Pos.BOTTOM_RIGHT);

        getChildren().addAll(header, textArea, footer);
    }

    public UUID getStickyNoteId() {
        return id;
    }

    public String getText() {
        return textArea.getText();
    }
}
