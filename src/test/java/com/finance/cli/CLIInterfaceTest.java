package com.finance.cli;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.finance.service.FinanceManager;

@ExtendWith(MockitoExtension.class)
class CLIInterfaceTest {

  private FinanceManager financeManager;
  private CLIInterface cli;
  private ByteArrayOutputStream outputStream;
  private PrintStream originalOut;

  @BeforeEach
  void setUp() {
    originalOut = System.out;
    outputStream = new ByteArrayOutputStream();
    System.setOut(new PrintStream(outputStream));
    financeManager = new FinanceManager();
    cli = new CLIInterface(financeManager);
  }

  @AfterEach
  void tearDown() {
    System.setOut(originalOut);
  }

  private String getUniqueUsername(String base) {
    return base
        + "_"
        + System.currentTimeMillis()
        + "_"
        + Thread.currentThread().getId()
        + "_"
        + (int) (Math.random() * 10000);
  }

  @Test
  void testProcessCommand_RegisterSuccess() {
    String uniqueUser = getUniqueUsername("testuser");
    cli.processCommand("register", uniqueUser + " password123");
    String output = outputStream.toString();
    assertTrue(output.contains("Регистрация") || output.contains("✅") || output.contains("успеш"));
  }

  @Test
  void testProcessCommand_RegisterInsufficientArgs() {
    cli.processCommand("register", "short");
    String output = outputStream.toString();
    assertTrue(output.contains("Использование") || output.contains("register"));
  }

  @Test
  void testProcessCommand_LoginSuccess() {
    String uniqueUser = getUniqueUsername("loginuser");
    cli.processCommand("register", uniqueUser + " password123");
    outputStream.reset();
    cli.processCommand("login", uniqueUser + " password123");
    String output = outputStream.toString();
    assertTrue(
        output.contains("Добро пожаловать") || output.contains(uniqueUser) || output.contains("✅"));
  }

  @Test
  void testProcessCommand_LoginWrongArgs() {
    cli.processCommand("login", "alex");
    String output = outputStream.toString();
    assertTrue(output.contains("Использование") || output.contains("login"));
  }

  @Test
  void testProcessCommand_AddIncomeSuccess() {
    String uniqueUser = getUniqueUsername("addincome");
    cli.processCommand("register", uniqueUser + " password123");
    outputStream.reset();
    cli.processCommand("login", uniqueUser + " password123");
    outputStream.reset();
    cli.processCommand("add_income", "Еда 1500 Ужин в ресторане");
    assertEquals(1500, financeManager.getCurrentUser().getWallet().getBalance(), 0.01);
  }

  @Test
  void testProcessCommand_AddExpenseSuccess() {
    String uniqueUser = getUniqueUsername("addexpense");
    cli.processCommand("register", uniqueUser + " password123");
    outputStream.reset();
    cli.processCommand("login", uniqueUser + " password123");
    outputStream.reset();
    cli.processCommand("add_income", "Зарплата 5000");
    outputStream.reset();
    cli.processCommand("add_expense", "Транспорт 500 Такси");
    assertEquals(4500, financeManager.getCurrentUser().getWallet().getBalance(), 0.01);
  }

  @Test
  void testProcessCommand_Help() {
    cli.processCommand("help", "");
    String output = outputStream.toString();
    assertTrue(output.contains("СПРАВКА") || output.contains("КОМАНДЫ") || output.contains("help"));
  }

  @Test
  void testProcessCommand_UnknownCommand() {
    cli.processCommand("unknown_command", "some args");
    String output = outputStream.toString();
    assertTrue(
        output.contains("Неизвестная команда") || output.contains("help") || output.contains("❌"));
  }

  @Test
  void testIsRunning() {
    assertTrue(cli.isRunning());
  }
}
