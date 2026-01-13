package com.finance.core;

import java.io.Serializable;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.Locale;

public abstract class Operation implements Serializable {
  private static final long serialVersionUID = 1L;

  protected double amount;
  protected Category category;
  protected LocalDateTime dateTime;
  protected String description;

  protected Operation() {
    this.amount = 0.0;
    this.category = new Category();
    this.dateTime = LocalDateTime.now();
    this.description = "";
  }

  protected Operation(
      double amount, Category category, String description, LocalDateTime dateTime) {
    if (amount <= 0) {
      throw new IllegalArgumentException("Сумма должна быть положительной");
    }
    this.amount = amount;
    this.category = category != null ? category : new Category();
    this.dateTime = dateTime != null ? dateTime : LocalDateTime.now();
    this.description = description != null ? description : "";
  }

  public Operation(double amount, Category category, String description) {
    this(amount, category, description, LocalDateTime.now());
  }

  public double getAmount() {
    return amount;
  }

  public Category getCategory() {
    return category;
  }

  public LocalDateTime getDateTime() {
    return dateTime;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  // 🔧 метод для изменения категории без пересоздания операции
  public void setCategory(Category category) {
    if (category == null) {
      throw new IllegalArgumentException("Категория не может быть null");
    }
    this.category = category;
  }

  // Метод для форматирования валюты как в ТЗ
  public String formatCurrency(double amount) {
    NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
    nf.setMinimumFractionDigits(1);
    nf.setMaximumFractionDigits(1);
    nf.setGroupingUsed(true);
    return nf.format(amount);
  }

  @Override
  public String toString() {
    return String.format(
        "%s: %s (%s) - %s", category.getName(), formatCurrency(amount), dateTime, description);
  }
}
