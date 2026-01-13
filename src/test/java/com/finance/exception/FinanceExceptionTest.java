package com.finance.exception;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class FinanceExceptionTest {

  @Test
  void testFinanceExceptionWithMessage() {
    // Подготовка
    String errorMessage = "Тестовое сообщение об ошибке";

    // Выполнение
    FinanceException exception = new FinanceException(errorMessage);

    // Проверка
    assertNotNull(exception);
    assertEquals(errorMessage, exception.getMessage());
    assertNull(exception.getCause());
  }

  @Test
  void testFinanceExceptionWithMessageAndCause() {
    // Подготовка
    String errorMessage = "Тестовое сообщение";
    Throwable cause = new IllegalArgumentException("Причина ошибки");

    // Выполнение
    FinanceException exception = new FinanceException(errorMessage, cause);

    // Проверка
    assertNotNull(exception);
    assertEquals(errorMessage, exception.getMessage());
    assertNotNull(exception.getCause());
    assertEquals(cause, exception.getCause());
    assertEquals("Причина ошибки", exception.getCause().getMessage());
  }

  @Test
  void testFinanceExceptionInheritance() {
    // Проверяем, что исключение правильно наследуется от RuntimeException
    FinanceException exception = new FinanceException("Тест");

    assertTrue(exception instanceof RuntimeException);
    assertTrue(exception instanceof Exception);
    assertTrue(exception instanceof Throwable);
  }

  @Test
  void testFinanceExceptionEmptyMessage() {
    // Проверяем работу с пустым сообщением
    FinanceException exception = new FinanceException("");

    assertNotNull(exception);
    assertEquals("", exception.getMessage());
  }

  @Test
  void testFinanceExceptionNullMessage() {
    // Проверяем работу с null сообщением
    FinanceException exception = new FinanceException(null);

    assertNotNull(exception);
    assertNull(exception.getMessage());
  }

  @Test
  void testFinanceExceptionChaining() {
    // Тестирование цепочки исключений
    Exception rootCause = new NullPointerException("Корневая причина");
    FinanceException middle = new FinanceException("Промежуточная ошибка", rootCause);
    FinanceException top = new FinanceException("Верхний уровень", middle);

    assertSame(middle, top.getCause());
    assertSame(rootCause, middle.getCause());
  }
}
