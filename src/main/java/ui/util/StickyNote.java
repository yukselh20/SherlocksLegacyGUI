package ui.util;

import java.util.UUID;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

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
        setPrefSize(150, 100);

        getChildren().addAll(dragHandle, textArea);
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
        setPrefSize(150, 100);

        getChildren().addAll(dragHandle, textArea);
    }

    public UUID getStickyNoteId() {
        return id;
    }

    public String getText() {
        return textArea.getText();
    }
}
