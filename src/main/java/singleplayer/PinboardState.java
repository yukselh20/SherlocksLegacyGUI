package singleplayer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class PinboardState {

    private final List<CardState> cards;
    private final List<NoteState> notes;
    private final List<ConnectionState> connections;

    @JsonCreator
    public PinboardState(
            @JsonProperty("cards") List<CardState> cards,
            @JsonProperty("notes") List<NoteState> notes,
            @JsonProperty("connections") List<ConnectionState> connections) {
        this.cards = cards;
        this.notes = notes;
        this.connections = connections;
    }

    public List<CardState> getCards() {
        return cards;
    }

    public List<NoteState> getNotes() {
        return notes;
    }

    public List<ConnectionState> getConnections() {
        return connections;
    }

    public static class CardState {
        private final String id;
        private final double x;
        private final double y;

        @JsonCreator
        public CardState(
                @JsonProperty("id") String id,
                @JsonProperty("x") double x,
                @JsonProperty("y") double y) {
            this.id = id;
            this.x = x;
            this.y = y;
        }

        public String getId() {
            return id;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }
    }

    public static class NoteState {
        private final String id;
        private final String text;
        private final double x;
        private final double y;

        @JsonCreator
        public NoteState(
                @JsonProperty("id") String id,
                @JsonProperty("text") String text,
                @JsonProperty("x") double x,
                @JsonProperty("y") double y) {
            this.id = id;
            this.text = text;
            this.x = x;
            this.y = y;
        }

        public String getId() {
            return id;
        }

        public String getText() {
            return text;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }
    }

    public static class ConnectionState {
        private final String startNodeId;
        private final String endNodeId;

        @JsonCreator
        public ConnectionState(
                @JsonProperty("startNodeId") String startNodeId,
                @JsonProperty("endNodeId") String endNodeId) {
            this.startNodeId = startNodeId;
            this.endNodeId = endNodeId;
        }

        public String getStartNodeId() {
            return startNodeId;
        }

        public String getEndNodeId() {
            return endNodeId;
        }
    }
}
