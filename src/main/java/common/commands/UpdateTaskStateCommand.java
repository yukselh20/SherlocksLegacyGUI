package common.commands;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import common.interfaces.GameActionContext;
import java.io.Serial;

public class UpdateTaskStateCommand extends BaseCommand {
    @Serial
    private static final long serialVersionUID = 1L;

    private final int taskIndex;
    private final boolean isCompleted;

    // This is the primary constructor for internal use and for serialization
    public UpdateTaskStateCommand(int taskIndex, boolean isCompleted) {
        super(false);
        this.taskIndex = taskIndex;
        this.isCompleted = isCompleted;
    }

    // This secondary constructor is for Jackson deserialization ONLY.
    // It accepts both "isCompleted" (the correct, new format) and "completed" (the old/buggy format)
    // to prevent server crashes if an older client sends a misnamed property.
    @JsonCreator
    public static UpdateTaskStateCommand fromJson(
        @JsonProperty("taskIndex") int taskIndex,
        @JsonProperty("isCompleted") Boolean isCompleted,
        @JsonProperty("completed") Boolean completed) {
        // Prioritize the correct "isCompleted" field, but fall back to "completed".
        // If neither is present, default to false.
        boolean finalCompleted = (isCompleted != null) ? isCompleted : (completed != null ? completed : false);
        return new UpdateTaskStateCommand(taskIndex, finalCompleted);
    }

    @Override
    protected void executeCommandLogic(GameActionContext context) {
        // This command is handled specially in GameContextServer and does not use this logic.
    }

    public int getTaskIndex() {
        return taskIndex;
    }

    @JsonProperty("isCompleted")
    public boolean isCompleted() {
        return isCompleted;
    }

    @Override
    public String getDescription() {
        return "Updates the state of a task.";
    }
}
