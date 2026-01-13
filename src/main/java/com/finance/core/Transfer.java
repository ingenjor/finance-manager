package com.finance.core;

import java.time.LocalDateTime;

public class Transfer implements java.io.Serializable {
  private static final long serialVersionUID = 1L;

  private String fromUser;
  private String toUser;
  private double amount;
  private LocalDateTime dateTime;
  private String description;

  public Transfer(String fromUser, String toUser, double amount, String description) {
    if (amount <= 0) {
      throw new IllegalArgumentException("Сумма перевода должна быть положительной");
    }
    this.fromUser = fromUser;
    this.toUser = toUser;
    this.amount = amount;
    this.dateTime = LocalDateTime.now();
    this.description = description;
  }

  public String getFromUser() {
    return fromUser;
  }

  public String getToUser() {
    return toUser;
  }

  public double getAmount() {
    return amount;
  }

  public LocalDateTime getDateTime() {
    return dateTime;
  }

  public String getDescription() {
    return description;
  }

  @Override
  public String toString() {
    return String.format(
        "Перевод от %s к %s: %.2f (%s) - %s", fromUser, toUser, amount, dateTime, description);
  }
}
