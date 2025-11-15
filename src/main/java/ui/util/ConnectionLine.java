package ui.util;

import javafx.scene.layout.Region;
import javafx.scene.shape.Line;

public class ConnectionLine extends Line {

    private final Region startNode;
    private final Region endNode;

    public ConnectionLine(Region startNode, Region endNode) {
        this.startNode = startNode;
        this.endNode = endNode;
        startXProperty().bind(startNode.layoutXProperty().add(startNode.widthProperty().divide(2)));
        startYProperty().bind(startNode.layoutYProperty().add(startNode.heightProperty().divide(2)));
        endXProperty().bind(endNode.layoutXProperty().add(endNode.widthProperty().divide(2)));
        endYProperty().bind(endNode.layoutYProperty().add(endNode.heightProperty().divide(2)));
        setStrokeWidth(2);
    }

    public Region getStartNode() {
        return startNode;
    }

    public Region getEndNode() {
        return endNode;
    }
}
