package ui.util;

import java.util.UUID;
import javafx.scene.control.TextArea;

public class StickyNote extends TextArea {

    private final UUID id;

    public StickyNote() {
        this.id = UUID.randomUUID();
        setPrefSize(150, 100);
        setStyle("-fx-background-color: #ffff99;");
        setWrapText(true);
    }

    public StickyNote(String id, String text) {
        this.id = UUID.fromString(id);
        setText(text);
        setPrefSize(150, 100);
        setStyle("-fx-background-color: #ffff99;");
        setWrapText(true);
    }

    public UUID getStickyNoteId() {
        return id;
    }
}
