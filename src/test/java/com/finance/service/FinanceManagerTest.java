package com.finance.service;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.*;

import com.finance.core.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FinanceManagerTest {
  private FinanceManager financeManager;
  private static final String TEST_PASS = "password123";

  @BeforeEach
  void setUp() {
    cleanupFiles();
    financeManager = new FinanceManager();
  }

  @AfterEach
  void tearDown() {
    if (financeManager.isAuthenticated()) {
      financeManager.logout();
    }
    cleanupFiles();
  }

  private void cleanupFiles() {
    try {
      Files.deleteIfExists(Paths.get("users_data.dat"));
      File exportsDir = new File("exports");
      if (exportsDir.exists() && exportsDir.isDirectory()) {
        for (File file : exportsDir.listFiles()) {
          if (!file.isDirectory()) {
            file.delete();
          }
        }
      }
    } catch (Exception e) {
      // Игнорируем ошибки удаления
    }
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
  void testRegistrationSuccess() {
    String uniqueUser = getUniqueUsername("u");
    assertDoesNotThrow(() -> financeManager.register(uniqueUser, TEST_PASS));
  }

  @Test
  void testLoginSuccess() {
    String user = getUniqueUsername("lu");
    financeManager.register(user, TEST_PASS);
    assertDoesNotThrow(() -> financeManager.login(user, TEST_PASS));
    assertTrue(financeManager.isAuthenticated());
  }

  @Test
  void testLogout() {
    String user = getUniqueUsername("logout");
    financeManager.register(user, TEST_PASS);
    financeManager.login(user, TEST_PASS);
    assertTrue(financeManager.isAuthenticated());
    financeManager.logout();
    assertFalse(financeManager.isAuthenticated());
  }

  @Test
  void testAddIncomeSuccess() {
    String user = getUniqueUsername("inc");
    financeManager.register(user, TEST_PASS);
    financeManager.login(user, TEST_PASS);
    assertDoesNotThrow(() -> financeManager.addIncome("Зарплата", 50000, "Октябрь"));
    User currentUser = financeManager.getCurrentUser();
    assertEquals(50000, currentUser.getWallet().getTotalIncome(), 0.01);
  }

  @Test
  void testAddIncomeCreatesNewCategory() {
    String user = getUniqueUsername("newcat");
    financeManager.register(user, TEST_PASS);
    financeManager.login(user, TEST_PASS);
    financeManager.addIncome("НоваяКатегория", 1000, "");
    User currentUser = financeManager.getCurrentUser();
    assertTrue(currentUser.getWallet().hasCategory("НоваяКатегория"));
  }

  @Test
  void testAddExpenseSuccess() {
    String user = getUniqueUsername("exp");
    financeManager.register(user, TEST_PASS);
    financeManager.login(user, TEST_PASS);
    financeManager.addIncome("Зарплата", 50000, "");
    assertDoesNotThrow(() -> financeManager.addExpense("Еда", 3000, "Продукты"));
    User currentUser = financeManager.getCurrentUser();
    assertEquals(3000, currentUser.getWallet().getTotalExpense(), 0.01);
    assertEquals(47000, currentUser.getWallet().getBalance(), 0.01);
  }

  @Test
  void testAddExpenseWithoutIncome() {
    String user = getUniqueUsername("noinc");
    financeManager.register(user, TEST_PASS);
    financeManager.login(user, TEST_PASS);
    assertDoesNotThrow(() -> financeManager.addExpense("Еда", 1000, ""));
    User currentUser = financeManager.getCurrentUser();
    assertEquals(-1000, currentUser.getWallet().getBalance(), 0.01);
  }

  @Test
  void testTransferSuccess() {
    String user1 = getUniqueUsername("t1");
    String user2 = getUniqueUsername("t2");
    financeManager.register(user1, TEST_PASS);
    financeManager.register(user2, TEST_PASS);
    financeManager.login(user1, TEST_PASS);
    financeManager.addIncome("Зарплата", 10000, "");
    assertDoesNotThrow(() -> financeManager.transfer(user2, 5000, "Тестовый перевод"));
    assertEquals(5000, financeManager.getCurrentUser().getWallet().getBalance(), 0.01);
    financeManager.logout();
    financeManager.login(user2, TEST_PASS);
    assertEquals(5000, financeManager.getCurrentUser().getWallet().getBalance(), 0.01);
  }

  @Test
  void testSetBudgetSuccess() {
    String user = getUniqueUsername("bud");
    financeManager.register(user, TEST_PASS);
    financeManager.login(user, TEST_PASS);
    assertDoesNotThrow(() -> financeManager.setBudget("Еда", 10000));
    Budget budget = financeManager.getCurrentUser().getWallet().getBudget("Еда");
    assertNotNull(budget);
    assertEquals(10000, budget.getLimit(), 0.01);
  }

  @Test
  void testSetBudgetForNonExistentCategory() {
    String user = getUniqueUsername("ncat");
    financeManager.register(user, TEST_PASS);
    financeManager.login(user, TEST_PASS);
    assertDoesNotThrow(() -> financeManager.setBudget("Несуществующая", 5000));
    assertTrue(financeManager.getCurrentUser().getWallet().hasCategory("Несуществующая"));
  }

  @Test
  void testEditBudgetSuccess() {
    String user = getUniqueUsername("edb");
    financeManager.register(user, TEST_PASS);
    financeManager.login(user, TEST_PASS);
    financeManager.setBudget("Еда", 10000);
    assertDoesNotThrow(() -> financeManager.editBudget("Еда", 15000));
    Budget budget = financeManager.getCurrentUser().getWallet().getBudget("Еда");
    assertEquals(15000, budget.getLimit(), 0.01);
  }

  @Test
  void testRemoveBudgetSuccess() {
    String user = getUniqueUsername("rmb");
    financeManager.register(user, TEST_PASS);
    financeManager.login(user, TEST_PASS);
    financeManager.setBudget("Еда", 10000);
    assertDoesNotThrow(() -> financeManager.removeBudget("Еда"));
    assertNull(financeManager.getCurrentUser().getWallet().getBudget("Еда"));
  }

  @Test
  void testBudgetExceededNotification() {
    String user = getUniqueUsername("exceed");
    financeManager.register(user, TEST_PASS);
    financeManager.login(user, TEST_PASS);
    financeManager.setBudget("Еда", 1000);
    financeManager.addIncome("Зарплата", 5000, "");
    financeManager.addExpense("Еда", 1200, "Много еды");
    List<String> notifications = financeManager.getAndClearNotifications();
    assertFalse(notifications.isEmpty());
    boolean hasExceedNotification =
        notifications.stream()
            .anyMatch(
                n -> n.contains("Превышен") || n.contains("ПРЕВЫШЕН") || n.contains("ВНИМАНИЕ"));
    assertTrue(hasExceedNotification, "Должно быть уведомление о превышении бюджета");
  }

  @Test
  void testAddCategorySuccess() {
    String user = getUniqueUsername("cat");
    financeManager.register(user, TEST_PASS);
    financeManager.login(user, TEST_PASS);
    assertDoesNotThrow(() -> financeManager.addCategory("Образование", "Курсы и книги"));
    assertTrue(financeManager.getCurrentUser().getWallet().hasCategory("Образование"));
  }

  @Test
  void testEditCategorySuccess() {
    String user = getUniqueUsername("editcat");
    financeManager.register(user, TEST_PASS);
    financeManager.login(user, TEST_PASS);
    financeManager.addCategory("Старая", "Старое описание");
    financeManager.addIncome("Старая", 1000, "");
    financeManager.editCategory("Старая", "Новая", "Новое описание");
    Wallet wallet = financeManager.getCurrentUser().getWallet();
    assertFalse(wallet.hasCategory("Старая"), "Старая категория должна быть удалена");
    assertTrue(wallet.hasCategory("Новая"), "Новая категория должна существовать");
    assertEquals(1000, wallet.getIncomeByCategory("Новая"), 0.01);
  }

  // 🔧 Критический тест для проверки баланса после редактирования
  @Test
  void testEditCategoryPreservesBalance() {
    String user = getUniqueUsername("balance_check");
    financeManager.register(user, TEST_PASS);
    financeManager.login(user, TEST_PASS);

    // Добавляем операции
    financeManager.addIncome("Зарплата", 50000, "");
    financeManager.addExpense("Еда", 3000, "");

    // Запоминаем баланс
    double initialBalance = financeManager.getCurrentUser().getWallet().getBalance();

    // Редактируем категорию
    financeManager.editCategory("Еда", "Продукты", "Новое описание");

    // Проверяем, что баланс не изменился
    double finalBalance = financeManager.getCurrentUser().getWallet().getBalance();
    assertEquals(
        initialBalance,
        finalBalance,
        0.01,
        "Баланс должен оставаться неизменным после редактирования категории");

    // Проверяем, что операция перенесена
    Wallet wallet = financeManager.getCurrentUser().getWallet();
    assertEquals(3000, wallet.getExpenseByCategory("Продукты"), 0.01);
    assertEquals(0, wallet.getExpenseByCategory("Еда"), 0.01);
  }

  @Test
  void testShowBalanceAfterOperations() {
    String user = getUniqueUsername("bal");
    financeManager.register(user, TEST_PASS);
    financeManager.login(user, TEST_PASS);
    financeManager.addIncome("Зарплата", 50000, "");
    financeManager.addExpense("Еда", 15000, "");
    assertDoesNotThrow(() -> financeManager.showBalance());
  }

  @Test
  void testShowStatistics() {
    String user = getUniqueUsername("sts");
    financeManager.register(user, TEST_PASS);
    financeManager.login(user, TEST_PASS);
    financeManager.addIncome("Зарплата", 50000, "");
    financeManager.addExpense("Еда", 3000, "");
    assertDoesNotThrow(() -> financeManager.showStatistics(List.of(), null, null));
  }

  @Test
  void testShowBudgets() {
    String user = getUniqueUsername("bgs");
    financeManager.register(user, TEST_PASS);
    financeManager.login(user, TEST_PASS);
    financeManager.setBudget("Еда", 10000);
    assertDoesNotThrow(() -> financeManager.showBudgets());
  }

  @Test
  void testGetIncomeByCategories() {
    String user = getUniqueUsername("inctot");
    financeManager.register(user, TEST_PASS);
    financeManager.login(user, TEST_PASS);
    financeManager.addIncome("Зарплата", 30000, "");
    financeManager.addIncome("Зарплата", 20000, "");
    financeManager.addIncome("Бонус", 5000, "");
    Map<String, Double> incomeByCat =
        financeManager.getCurrentUser().getWallet().getIncomeByCategories();
    assertEquals(2, incomeByCat.size());
    assertEquals(50000, incomeByCat.get("Зарплата"), 0.01);
    assertEquals(5000, incomeByCat.get("Бонус"), 0.01);
  }

  @Test
  void testGetExpenseByCategories() {
    String user = getUniqueUsername("exptot");
    financeManager.register(user, TEST_PASS);
    financeManager.login(user, TEST_PASS);
    financeManager.addIncome("Зарплата", 50000, "");
    financeManager.addExpense("Еда", 3000, "");
    financeManager.addExpense("Еда", 2000, "");
    financeManager.addExpense("Транспорт", 1000, "");
    Map<String, Double> expenseByCat =
        financeManager.getCurrentUser().getWallet().getExpenseByCategories();
    assertEquals(2, expenseByCat.size());
    assertEquals(5000, expenseByCat.get("Еда"), 0.01);
    assertEquals(1000, expenseByCat.get("Транспорт"), 0.01);
  }

  @Test
  void testExportCSVSuccess() {
    String user = getUniqueUsername("csv");
    financeManager.register(user, TEST_PASS);
    financeManager.login(user, TEST_PASS);
    financeManager.addIncome("Зарплата", 50000, "Тест CSV");
    assertDoesNotThrow(() -> financeManager.exportToFile("test_export", "csv"));
    File csvFile = new File("exports/test_export.csv");
    assertTrue(csvFile.exists() || csvFile.getParentFile().exists());
  }

  @Test
  void testShowDetailedReport() {
    String user = getUniqueUsername("rep");
    financeManager.register(user, TEST_PASS);
    financeManager.login(user, TEST_PASS);
    financeManager.addIncome("Зарплата", 50000, "");
    financeManager.addExpense("Еда", 3000, "");
    financeManager.setBudget("Еда", 10000);
    assertDoesNotThrow(() -> financeManager.showDetailedReport());
  }

  @Test
  void testParseDateValid() {
    String user = getUniqueUsername("dat");
    financeManager.register(user, TEST_PASS);
    financeManager.login(user, TEST_PASS);
    LocalDate date = financeManager.parseDate("01.10.2023");
    assertEquals(2023, date.getYear());
    assertEquals(10, date.getMonthValue());
    assertEquals(1, date.getDayOfMonth());
  }

  @Test
  void testWalletNotifications() {
    String user = getUniqueUsername("notif");
    financeManager.register(user, TEST_PASS);
    financeManager.login(user, TEST_PASS);
    financeManager.setBudget("Еда", 1000);
    financeManager.addIncome("Зарплата", 5000, "");
    financeManager.addExpense("Еда", 900, "");
    financeManager.addExpense("Еда", 200, "");
    List<String> notifications = financeManager.getAndClearNotifications();
    assertNotNull(notifications);
    assertFalse(notifications.isEmpty());
  }

  @Test
  void testNegativeBalanceNotification() {
    String user = getUniqueUsername("negbal");
    financeManager.register(user, TEST_PASS);
    financeManager.login(user, TEST_PASS);
    financeManager.addExpense("Еда", 1000, "");
    List<String> notifications = financeManager.getAndClearNotifications();
    boolean hasNegativeBalanceNotification =
        notifications.stream()
            .anyMatch(
                n ->
                    n.contains("Отрицательный баланс")
                        || n.contains("Расходы превысили доходы")
                        || n.contains("КРИТИЧЕСКОЕ"));
    assertTrue(hasNegativeBalanceNotification, "Должно быть уведомление об отрицательном балансе");
  }

  @Test
  void testExampleFromTZ() {
    String uniqueUser = getUniqueUsername("tz");
    financeManager.register(uniqueUser, TEST_PASS);
    financeManager.login(uniqueUser, TEST_PASS);
    financeManager.addIncome("Зарплата", 20000, "");
    financeManager.addIncome("Зарплата", 40000, "");
    financeManager.addIncome("Бонус", 3000, "");
    financeManager.addExpense("Еда", 300, "");
    financeManager.addExpense("Еда", 500, "");
    financeManager.addExpense("Развлечения", 3000, "");
    financeManager.addExpense("Коммунальные услуги", 3000, "");
    financeManager.addExpense("Такси", 1500, "");
    financeManager.setBudget("Еда", 4000);
    financeManager.setBudget("Развлечения", 3000);
    financeManager.setBudget("Коммунальные услуги", 2500);
    assertEquals(63000.0, financeManager.getCurrentUser().getWallet().getTotalIncome(), 0.01);
    assertEquals(8300.0, financeManager.getCurrentUser().getWallet().getTotalExpense(), 0.01);
    Budget foodBudget = financeManager.getCurrentUser().getWallet().getBudget("Еда");
    assertNotNull(foodBudget);
    assertEquals(800, foodBudget.getSpent(), 0.01);
    assertEquals(3200, foodBudget.getRemaining(), 0.01);
    Budget utilBudget =
        financeManager.getCurrentUser().getWallet().getBudget("Коммунальные услуги");
    assertNotNull(utilBudget);
    assertEquals(3000, utilBudget.getSpent(), 0.01);
    assertEquals(-500, utilBudget.getRemaining(), 0.01);
  }

  @Test
  void testOutputOrderAsInTZ() {
    String user = getUniqueUsername("order");
    financeManager.register(user, TEST_PASS);
    financeManager.login(user, TEST_PASS);
    financeManager.setBudget("Еда", 4000);
    financeManager.setBudget("Развлечения", 3000);
    financeManager.setBudget("Коммунальные услуги", 2500);
    String output = financeManager.getCurrentUser().getWallet().getBudgetSummaryAsInTZ();
    int indexCom = output.indexOf("Коммунальные услуги:");
    int indexFood = output.indexOf("Еда:");
    int indexEnt = output.indexOf("Развлечения:");
    assertTrue(
        indexCom < indexFood && indexFood < indexEnt,
        "Порядок должен быть: Коммунальные услуги, Еда, Развлечения");
  }
}
