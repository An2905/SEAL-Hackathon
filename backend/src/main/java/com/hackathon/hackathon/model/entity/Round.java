package com.hackathon.hackathon.model.entity;

import lombok.Data;

@Data
public class Round {
  private String roundId;
  private String eventId;
  private String name;
  private String startDate;
  private String endDate;
  private String submissionDeadline;
  private String roundOrder;
  private Integer winnersPerRound;
  private Integer winnerCount;
}
