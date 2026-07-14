package com.hackathon.hackathon.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class VietnamTime {
  private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
  private static final DateTimeFormatter DATABASE_DATE_TIME =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private VietnamTime() {}

  public static String nowForDatabase() {
    return LocalDateTime.now(ZONE).format(DATABASE_DATE_TIME);
  }
}
