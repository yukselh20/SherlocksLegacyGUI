package common.dto;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serial;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;
public class JournalEntryDTO implements Serializable {
  @Serial
  private static final long serialVersionUID = 1L;
  private final UUID id;
  private final String text;
  private final String contributorPlayerId;
  private final long timestamp;
  private final List<UUID> sourceIds;

  @JsonCreator
  public JournalEntryDTO(
          @JsonProperty("id") UUID id,
          @JsonProperty("text") String text,
          @JsonProperty("contributorPlayerId") String contributorPlayerId,
          @JsonProperty("timestamp") long timestamp,
          @JsonProperty("sourceIds") List<UUID> sourceIds) {
    this.id = (id == null) ? UUID.randomUUID() : id;
    this.text = Objects.requireNonNull(text, "Text cannot be null");
    this.contributorPlayerId = Objects.requireNonNull(contributorPlayerId, "Contributor ID cannot be null");
    this.timestamp = timestamp;
    this.sourceIds = (sourceIds == null) ? new ArrayList<>() : sourceIds;
  }

  public JournalEntryDTO(String text, String contributorPlayerId, long timestamp, List<UUID> sourceIds) {
    this(null, text, contributorPlayerId, timestamp, sourceIds);
  }

  public JournalEntryDTO(String text, String contributorPlayerId, long timestamp) {
    this(null, text, contributorPlayerId, timestamp, null);
  }

  public UUID getId() {
    return id;
  }

  public String getText() {
    return text;
  }

  public String getContributorPlayerId() {
    return contributorPlayerId;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public List<UUID> getSourceIds() {
    return sourceIds;
  }

  @Override
  public String toString() {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    String prefix = contributorPlayerId.startsWith("Player") ? contributorPlayerId + ":" : contributorPlayerId;
    return "[" + sdf.format(new Date(timestamp)) + "] " + prefix + " " + text;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    JournalEntryDTO that = (JournalEntryDTO) o;
    return Objects.equals(text, that.text) && Objects.equals(contributorPlayerId, that.contributorPlayerId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(text, contributorPlayerId);
  }
}