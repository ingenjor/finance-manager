package com.finance.core;

import java.time.LocalDateTime;

public class Income extends Operation {
  private static final long serialVersionUID = 1L;

  public Income() {
    super();
  }

  public Income(double amount, Category category, String description, LocalDateTime dateTime) {
    super(amount, category, description, dateTime);
  }

  public Income(double amount, Category category, String description) {
    super(amount, category, description);
  }

  public Income(double amount, Category category) {
    this(amount, category, "");
  }

  @Override
  public String toString() {
    return "[ДОХОД] " + super.toString();
  }
}
