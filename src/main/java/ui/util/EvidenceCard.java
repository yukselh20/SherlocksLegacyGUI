package ui.util;

import javafx.scene.control.Label;
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

        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        Label summaryLabel = new Label(summary);
        summaryLabel.setWrapText(true);

        getChildren().addAll(titleLabel, summaryLabel);
    }

    public String getTitle() {
        return title;
    }

    public UUID getEvidenceId() {
        return id;
    }
}
