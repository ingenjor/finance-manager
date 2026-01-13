package com.finance.service;

public class AuthService {
  public boolean validateCredentials(String login, String password) {
    if (login == null || login.trim().isEmpty()) {
      return false;
    }
    if (password == null || password.trim().isEmpty()) {
      return false;
    }
    if (login.length() < 3) {
      return false;
    }
    if (password.length() < 4) {
      return false;
    }
    return true;
  }
}
