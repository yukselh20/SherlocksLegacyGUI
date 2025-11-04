package common.commands;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import common.interfaces.GameActionContext;
import java.io.Serial;

public class SubmitExamAnswerCommand extends BaseCommand {
  @Serial
  private static final long serialVersionUID = 1L;
  private final int questionNumber;
  private final String answerText;

  @JsonCreator
  public SubmitExamAnswerCommand(
          @JsonProperty("questionNumber") int questionNumber,
          @JsonProperty("answerText") String answerText) {
    super(true);
    this.questionNumber = questionNumber;
    this.answerText = (answerText != null) ? answerText.trim() : "";
  }

  public int getQuestionNumber() {
    return questionNumber;
  }

  public String getAnswerText() {
    return answerText;
  }

  @Override
  protected void executeCommandLogic(GameActionContext context) {
    context.processExamAnswer(getPlayerId(), this.questionNumber, this.answerText);
  }

  @Override
  public String getDescription() {
    return "Submits an answer to a specific final exam question. (Used during interactive exam)";
  }
}