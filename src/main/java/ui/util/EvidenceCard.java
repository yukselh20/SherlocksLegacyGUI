package ui.util;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
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
        setMaxWidth(200); // Set a max width to encourage vertical growth

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
    }

    public String getTitle() {
        return title;
    }

    public UUID getEvidenceId() {
        return id;
    }
}
