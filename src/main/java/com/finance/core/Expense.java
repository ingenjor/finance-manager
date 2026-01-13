package com.finance.core;

import java.time.LocalDateTime;

public class Expense extends Operation {
  private static final long serialVersionUID = 1L;

  public Expense() {
    super();
  }

  public Expense(double amount, Category category, String description, LocalDateTime dateTime) {
    super(amount, category, description, dateTime);
  }

  public Expense(double amount, Category category, String description) {
    super(amount, category, description);
  }

  public Expense(double amount, Category category) {
    this(amount, category, "");
  }

  @Override
  public String toString() {
    return "[РАСХОД] " + super.toString();
  }
}
