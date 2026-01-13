package com.finance.exception;

public class FinanceException extends RuntimeException {
  public FinanceException(String message) {
    super(message);
  }

  // конструктор с причиной для тестирования
  public FinanceException(String message, Throwable cause) {
    super(message, cause);
  }
}
