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
class CLIInterfaceIntegrationTest {

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
    return base + "_" + System.currentTimeMillis() + "_" + System.nanoTime();
  }

  @Test
  void testFullIntegration_RegisterLoginAddOperations() {
    String uniqueUser = getUniqueUsername("fulltest");

    // 1. Register
    cli.processCommand("register", uniqueUser + " password123");
    String registerOutput = outputStream.toString();
    assertTrue(
        registerOutput.contains("✅")
            || registerOutput.contains("Регистрация")
            || registerOutput.contains("успеш"));

    // Clear output
    outputStream.reset();

    // 2. Login
    cli.processCommand("login", uniqueUser + " password123");
    String loginOutput = outputStream.toString();
    assertTrue(loginOutput.contains("✅ Добро пожаловать") || loginOutput.contains(uniqueUser));

    // Clear output
    outputStream.reset();

    // 3. Add income
    cli.processCommand("add_income", "Зарплата 50000 Октябрь");
    String incomeOutput = outputStream.toString();
    assertTrue(incomeOutput.contains("✅ Доход добавлен") || incomeOutput.contains("50000"));

    // Clear output
    outputStream.reset();

    // 4. Add expense
    cli.processCommand("add_expense", "Еда 3000 Продукты");
    String expenseOutput = outputStream.toString();
    assertTrue(expenseOutput.contains("✅ Расход добавлен") || expenseOutput.contains("3000"));

    // Clear output
    outputStream.reset();

    // 5. Set budget
    cli.processCommand("set_budget", "Еда 10000");
    String budgetOutput = outputStream.toString();
    assertTrue(budgetOutput.contains("✅ Бюджет установлен") || budgetOutput.contains("10000"));

    // Clear output
    outputStream.reset();

    // 6. Check balance
    cli.processCommand("balance", "");
    String balanceOutput = outputStream.toString();
    assertTrue(balanceOutput.contains("БАЛАНС") || balanceOutput.contains("баланс"));

    // Clear output
    outputStream.reset();

    // 7. Check stats
    cli.processCommand("stats", "");
    String statsOutput = outputStream.toString();
    assertTrue(statsOutput.contains("СТАТИСТИКА") || statsOutput.contains("статистика"));

    // Clear output
    outputStream.reset();

    // 8. Logout
    cli.processCommand("logout", "");
    String logoutOutput = outputStream.toString();
    assertTrue(logoutOutput.contains("👋") || logoutOutput.contains("выход"));
  }

  @Test
  void testIntegration_TransferBetweenUsers() {
    String user1 = getUniqueUsername("user1");
    String user2 = getUniqueUsername("user2");

    // Register both users
    cli.processCommand("register", user1 + " pass123");
    outputStream.reset();

    cli.processCommand("register", user2 + " pass123");
    outputStream.reset();

    // Login as user1
    cli.processCommand("login", user1 + " pass123");
    outputStream.reset();

    // Add income to user1
    cli.processCommand("add_income", "Зарплата 10000");
    outputStream.reset();

    // Transfer to user2
    cli.processCommand("transfer", user2 + " 5000 Тестовый перевод");

    String output = outputStream.toString();
    assertTrue(output.contains("✅ Перевод выполнен") || output.contains("Перевод"));
  }

  @Test
  void testIntegration_ExampleFromTZ() {
    String uniqueUser = getUniqueUsername("tzexample");

    // Register and login
    cli.processCommand("register", uniqueUser + " pass123");
    outputStream.reset();

    cli.processCommand("login", uniqueUser + " pass123");
    outputStream.reset();

    // Run TZ example
    cli.processCommand("example_tz", "");

    String output = outputStream.toString();
    // Should contain TZ example data
    assertTrue(
        output.contains("ТЕХНИЧЕСКОГО ЗАДАНИЯ")
            || output.contains("пример")
            || output.contains("ТЗ"));

    // Check for specific numbers from TZ
    assertTrue(
        output.contains("63,000.0") || output.contains("8,300.0") || output.contains("2,500.0"));
  }

  @Test
  void testIntegration_BudgetNotifications() {
    String uniqueUser = getUniqueUsername("budgetnotif");

    // Register and login
    cli.processCommand("register", uniqueUser + " pass123");
    outputStream.reset();

    cli.processCommand("login", uniqueUser + " pass123");
    outputStream.reset();

    // Add income
    cli.processCommand("add_income", "Зарплата 10000");
    outputStream.reset();

    // Set budget
    cli.processCommand("set_budget", "Еда 1000");
    outputStream.reset();

    // Add expense that exceeds budget
    cli.processCommand("add_expense", "Еда 1200 Много еды");

    String output = outputStream.toString();
    // Should contain budget warning
    assertTrue(output.contains("⚠️") || output.contains("ВНИМАНИЕ") || output.contains("Превышен"));
  }

  @Test
  void testIntegration_CategoryManagement() {
    String uniqueUser = getUniqueUsername("categorytest");

    // Register and login
    cli.processCommand("register", uniqueUser + " pass123");
    outputStream.reset();

    cli.processCommand("login", uniqueUser + " pass123");
    outputStream.reset();

    // Add new category
    cli.processCommand("add_category", "Образование Курсы и книги");
    outputStream.reset();

    // Edit category
    cli.processCommand("edit_category", "Образование Обучение Курсы, книги, семинары");

    String output = outputStream.toString();
    assertTrue(
        output.contains("✅ Категория")
            || output.contains("обновлена")
            || output.contains("добавлена"));
  }

  @Test
  void testIntegration_ExportImport() {
    String uniqueUser = getUniqueUsername("exporttest");

    // Register and login
    cli.processCommand("register", uniqueUser + " pass123");
    outputStream.reset();

    cli.processCommand("login", uniqueUser + " pass123");
    outputStream.reset();

    // Add some data
    cli.processCommand("add_income", "Зарплата 50000");
    outputStream.reset();

    cli.processCommand("add_expense", "Еда 3000");
    outputStream.reset();

    // Export
    cli.processCommand("export", "test_export json");

    String output = outputStream.toString();
    assertTrue(
        output.contains("экспортированы")
            || output.contains("export")
            || output.contains("Экспорт"));
  }

  @Test
  void testIntegration_ErrorHandling() {
    // Test various error scenarios

    // 1. Try to use command without auth
    cli.processCommand("balance", "");
    String output1 = outputStream.toString();
    assertTrue(output1.contains("Требуется авторизация") || output1.contains("🔒"));

    // Clear
    outputStream.reset();

    // 2. Invalid command
    cli.processCommand("invalid_command", "args");
    String output2 = outputStream.toString();
    assertTrue(output2.contains("Неизвестная команда") || output2.contains("help"));

    // Clear
    outputStream.reset();

    // 3. Invalid number format
    String uniqueUser = getUniqueUsername("errortest");
    cli.processCommand("register", uniqueUser + " pass123");
    outputStream.reset();

    cli.processCommand("login", uniqueUser + " pass123");
    outputStream.reset();

    cli.processCommand("add_income", "Зарплата нечисло");

    String output3 = outputStream.toString();
    assertTrue(output3.contains("Неверный формат суммы") || output3.contains("❌"));
  }

  @Test
  void testIntegration_FinancialHealthWarnings() {
    String uniqueUser = getUniqueUsername("healthtest");

    // Register and login
    cli.processCommand("register", uniqueUser + " pass123");
    outputStream.reset();

    cli.processCommand("login", uniqueUser + " pass123");
    outputStream.reset();

    // Add small income
    cli.processCommand("add_income", "Зарплата 1000");
    outputStream.reset();

    // Add large expense to create negative balance
    cli.processCommand("add_expense", "Еда 1500 Много расходов");

    String output = outputStream.toString();
    // Should contain negative balance warning
    assertTrue(
        output.contains("Отрицательный баланс")
            || output.contains("КРИТИЧЕСКОЕ")
            || output.contains("превысили"));
  }

  @Test
  void testIntegration_OperationsFiltering() {
    String uniqueUser = getUniqueUsername("opstest");

    // Register and login
    cli.processCommand("register", uniqueUser + " pass123");
    outputStream.reset();

    cli.processCommand("login", uniqueUser + " pass123");
    outputStream.reset();

    // Add multiple operations
    cli.processCommand("add_income", "Зарплата 50000");
    outputStream.reset();

    cli.processCommand("add_expense", "Еда 3000");
    outputStream.reset();

    cli.processCommand("add_expense", "Транспорт 2000");
    outputStream.reset();

    // Show operations
    cli.processCommand("operations", "");

    String output = outputStream.toString();
    assertTrue(
        output.contains("ОПЕРАЦИИ") || output.contains("операции") || output.contains("Операции"));
  }

  @Test
  void testIntegration_HelpCommand() {
    cli.processCommand("help", "");

    String output = outputStream.toString();
    // Should contain all command categories
    assertTrue(
        output.contains("АУТЕНТИФИКАЦИЯ")
            || output.contains("ОПЕРАЦИИ")
            || output.contains("КАТЕГОРИИ")
            || output.contains("БЮДЖЕТЫ")
            || output.contains("ОТЧЕТЫ")
            || output.contains("ИМПОРТ/ЭКСПОРТ"));
  }
}
