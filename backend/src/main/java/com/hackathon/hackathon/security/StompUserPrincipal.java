package com.hackathon.hackathon.security;

import java.security.Principal;

public class StompUserPrincipal implements Principal {

  private final String userId;
  private final String name;

  public StompUserPrincipal(String userId, String name) {
    this.userId = userId;
    this.name = name;
  }

  public String getUserId() {
    return userId;
  }

  @Override
  public String getName() {
    return name;
  }
}
