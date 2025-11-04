package common.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ExamResultDTO implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;
  private final int score;
  private final int totalQuestions;
  private final String feedbackMessage;
  private final String finalRank;
  private final List<String> reviewableAnswersInfo;

  @JsonCreator
  public ExamResultDTO(
          @JsonProperty("score") int score,
          @JsonProperty("totalQuestions") int totalQuestions,
          @JsonProperty("feedbackMessage") String feedbackMessage,
          @JsonProperty("finalRank") String finalRank,
          @JsonProperty("reviewableAnswersInfo") List<String> reviewableAnswersInfo) {
    this.score = score;
    this.totalQuestions = totalQuestions;
    this.feedbackMessage = feedbackMessage;
    this.finalRank = finalRank;
    this.reviewableAnswersInfo =
            reviewableAnswersInfo != null ? new ArrayList<>(reviewableAnswersInfo) : new ArrayList<>();
  }

  // --- GETTERS ADDED ---
  public int getScore() {
    return score;
  }

  public int getTotalQuestions() {
    return totalQuestions;
  }

  public String getFeedbackMessage() {
    return feedbackMessage;
  }

  public String getFinalRank() {
    return finalRank;
  }

  public List<String> getReviewableAnswersInfo() {
    return reviewableAnswersInfo;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(feedbackMessage).append("\n");
    sb.append("Score: ").append(score).append("/").append(totalQuestions).append("\n");
    sb.append("Final Rank: ").append(finalRank);
    if (!reviewableAnswersInfo.isEmpty()) {
      sb.append("\n\n--- Review of Incorrect/Unanswered Questions ---");
      for (String reviewInfo : reviewableAnswersInfo) {
        sb.append("\n").append(reviewInfo);
      }
    }
    return sb.toString();
  }
}