package ui.util;

import javafx.scene.layout.Region;
import javafx.scene.shape.Line;

public class ConnectionLine extends Line {

    private final Region startNode;
    private final Region endNode;
    private final boolean isAutomatic;

    public ConnectionLine(Region startNode, Region endNode) {
        this(startNode, endNode, false);
    }

    public ConnectionLine(Region startNode, Region endNode, boolean isAutomatic) {
        this.startNode = startNode;
        this.endNode = endNode;
        this.isAutomatic = isAutomatic;

        startXProperty().bind(startNode.layoutXProperty().add(startNode.widthProperty().divide(2)));
        startYProperty().bind(startNode.layoutYProperty().add(startNode.heightProperty().divide(2)));
        endXProperty().bind(endNode.layoutXProperty().add(endNode.widthProperty().divide(2)));
        endYProperty().bind(endNode.layoutYProperty().add(endNode.heightProperty().divide(2)));
        setStrokeWidth(2);

        if (isAutomatic) {
            getStrokeDashArray().addAll(10d, 10d);
        }
    }

    public Region getStartNode() {
        return startNode;
    }

    public Region getEndNode() {
        return endNode;
    }

    public boolean isAutomatic() {
        return isAutomatic;
    }
}
