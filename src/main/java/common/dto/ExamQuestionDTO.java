package common.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

public class ExamQuestionDTO implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;
  private final int questionNumber;
  private final String questionText;

  @JsonCreator
  public ExamQuestionDTO(
          @JsonProperty("questionNumber") int questionNumber,
          @JsonProperty("questionText") String questionText) {
    this.questionNumber = questionNumber;
    this.questionText = Objects.requireNonNull(questionText, "Question text cannot be null");
  }

  public int getQuestionNumber() {
    return questionNumber;
  }

  public String getQuestionText() {
    return questionText;
  }

  @Override
  public String toString() {
    return questionNumber + ". " + questionText;
  }
}