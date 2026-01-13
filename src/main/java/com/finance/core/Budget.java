package com.finance.core;

import java.io.Serializable;
import java.text.NumberFormat;
import java.util.Locale;

public class Budget implements Serializable {
  private static final long serialVersionUID = 1L;

  private Category category;
  private double limit;
  private double spent;

  public Budget() {
    this.category = new Category();
    this.limit = 0.0;
    this.spent = 0.0;
  }

  public Budget(Category category, double limit, double spent) {
    this.category = category != null ? category : new Category();
    this.limit = limit;
    this.spent = spent;
  }

  public Budget(Category category, double limit) {
    this.category = category;
    this.limit = limit;
    this.spent = 0.0;
  }

  public Category getCategory() {
    return category;
  }

  public double getLimit() {
    return limit;
  }

  public double getSpent() {
    return spent;
  }

  public double getRemaining() {
    return limit - spent;
  }

  public void addExpense(double amount) {
    spent += amount;
  }

  public boolean isExceeded() {
    return spent > limit;
  }

  public boolean isNearLimit() {
    return spent >= limit * 0.8 && spent < limit;
  }

  public double getUsagePercentage() {
    return limit > 0 ? (spent / limit) * 100 : 0;
  }

  public void updateLimit(double newLimit) {
    this.limit = newLimit;
  }

  // Публичный метод для форматирования валюты
  public String formatCurrency(double amount) {
    NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
    nf.setMinimumFractionDigits(1);
    nf.setMaximumFractionDigits(1);
    nf.setGroupingUsed(true);
    return nf.format(amount);
  }

  @Override
  public String toString() {
    String remainingStr = formatCurrency(getRemaining());
    if (getRemaining() < 0) {
      remainingStr = "-" + formatCurrency(Math.abs(getRemaining()));
    }

    return String.format(
        "%s: Лимит=%s, Потрачено=%s, Осталось=%s (%.0f%%)",
        category.getName(),
        formatCurrency(limit),
        formatCurrency(spent),
        remainingStr,
        getUsagePercentage());
  }
}
