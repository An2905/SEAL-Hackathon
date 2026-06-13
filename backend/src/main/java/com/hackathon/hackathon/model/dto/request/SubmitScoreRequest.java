package com.hackathon.hackathon.model.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

@Data
public class SubmitScoreRequest {
  @NotBlank(message = "Event ID is required.")
  private String eventId;

  @NotBlank(message = "Round ID is required.")
  private String roundId;

  @NotBlank(message = "Group ID is required.")
  private String groupId;

  @NotBlank(message = "Submission ID is required.")
  private String submissionId;

  @NotEmpty(message = "Score details must not be empty.")
  @Valid
  private List<ScoreDetailItemRequest> details;
}
