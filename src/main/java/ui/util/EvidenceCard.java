package ui.util;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.Cursor;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import java.util.UUID;
import javafx.scene.text.FontWeight;

public class EvidenceCard extends VBox {

    private final UUID id;
    private String title;

    public EvidenceCard(UUID id, String title, String summary) {
        this.id = id;
        this.title = title;
        setPadding(new javafx.geometry.Insets(10));
        setSpacing(5);
        setStyle("-fx-background-color: #ffffff; -fx-border-color: #000000; -fx-border-width: 1;");
        setMaxWidth(300); // Set a max width to control wrapping

        // Delete button
        Button deleteButton = new Button("X");
        deleteButton.setStyle("-fx-background-color: transparent; -fx-text-fill: red; -fx-font-weight: bold;");
        deleteButton.setOnAction(event -> {
            Pane parent = (Pane) this.getParent();
            if (parent != null) {
                parent.getChildren().remove(this);
            }
        });

        StackPane header = new StackPane();
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        header.getChildren().addAll(titleLabel, deleteButton);
        StackPane.setAlignment(deleteButton, Pos.TOP_RIGHT);

        Label summaryLabel = new Label(summary);
        summaryLabel.setWrapText(true);

        getChildren().addAll(header, summaryLabel);
        setupResizing();
    }

    private void setupResizing() {
        final double resizeMargin = 5;
        final double[] startPos = new double[2];
        final boolean[] isResizing = {false};
        final Cursor[] cursor = {Cursor.DEFAULT};

        setOnMouseMoved(event -> {
            double x = event.getX();
            double y = event.getY();
            double width = getWidth();
            double height = getHeight();

            if (x < resizeMargin || x > width - resizeMargin || y < resizeMargin || y > height - resizeMargin) {
                if (x < resizeMargin) {
                    cursor[0] = Cursor.W_RESIZE;
                } else if (x > width - resizeMargin) {
                    cursor[0] = Cursor.E_RESIZE;
                } else if (y < resizeMargin) {
                    cursor[0] = Cursor.N_RESIZE;
                } else {
                    cursor[0] = Cursor.S_RESIZE;
                }
                setCursor(cursor[0]);
            } else {
                setCursor(Cursor.DEFAULT);
            }
        });

        setOnMousePressed(event -> {
            if (getCursor() != Cursor.DEFAULT) {
                isResizing[0] = true;
                startPos[0] = event.getSceneX();
                startPos[1] = event.getSceneY();
            }
        });

        setOnMouseDragged(event -> {
            if (isResizing[0]) {
                double dx = event.getSceneX() - startPos[0];
                double dy = event.getSceneY() - startPos[1];

                if (cursor[0] == Cursor.W_RESIZE) {
                    setPrefWidth(getPrefWidth() - dx);
                    setLayoutX(getLayoutX() + dx);
                } else if (cursor[0] == Cursor.E_RESIZE) {
                    setPrefWidth(getPrefWidth() + dx);
                } else if (cursor[0] == Cursor.N_RESIZE) {
                    setPrefHeight(getPrefHeight() - dy);
                    setLayoutY(getLayoutY() + dy);
                } else if (cursor[0] == Cursor.S_RESIZE) {
                    setPrefHeight(getPrefHeight() + dy);
                }
                startPos[0] = event.getSceneX();
                startPos[1] = event.getSceneY();
            }
        });

        setOnMouseReleased(event -> {
            isResizing[0] = false;
        });
    }

    public String getTitle() {
        return title;
    }

    public UUID getEvidenceId() {
        return id;
    }
}
