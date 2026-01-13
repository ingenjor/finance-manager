package com.finance.core;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WalletTest {

  private Wallet wallet;

  @BeforeEach
  void setUp() {
    wallet = new Wallet();
    // Добавляем тестовую категорию
    wallet.addCategory(new Category("ТестКатегория", "Для тестов"));
  }

  // ========== ТЕСТЫ ДЛЯ КАТЕГОРИЙ ==========

  @Test
  void testAddCategory() {
    assertTrue(wallet.hasCategory("ТестКатегория"));
    assertEquals("Для тестов", wallet.getCategory("ТестКатегория").getDescription());
  }

  @Test
  void testRemoveCategory() {
    wallet.removeCategory("ТестКатегория");
    assertFalse(wallet.hasCategory("ТестКатегория"));
  }

  @Test
  void testGetCategories() {
    List<Category> categories = wallet.getCategories();
    assertFalse(categories.isEmpty());
    assertTrue(categories.stream().anyMatch(c -> c.getName().equals("ТестКатегория")));
  }

  @Test
  void testGetCategory_NonExistent() {
    assertNull(wallet.getCategory("Несуществующая"));
  }

  // ========== ТЕСТЫ ДЛЯ ОПЕРАЦИЙ ==========

  @Test
  void testAddIncome() {
    Income income = new Income(5000, wallet.getCategory("Зарплата"), "Тест");
    wallet.addOperation(income);

    assertEquals(5000, wallet.getBalance(), 0.01);
    assertEquals(5000, wallet.getTotalIncome(), 0.01);
    assertEquals(0, wallet.getTotalExpense(), 0.01);
  }

  @Test
  void testAddExpense() {
    // Сначала добавляем доход
    wallet.addOperation(new Income(5000, wallet.getCategory("Зарплата"), ""));

    Expense expense = new Expense(2000, wallet.getCategory("ТестКатегория"), "Тест");
    wallet.addOperation(expense);

    assertEquals(3000, wallet.getBalance(), 0.01);
    assertEquals(5000, wallet.getTotalIncome(), 0.01);
    assertEquals(2000, wallet.getTotalExpense(), 0.01);
  }

  @Test
  void testAddMultipleOperations() {
    wallet.addOperation(new Income(10000, wallet.getCategory("Зарплата"), ""));
    wallet.addOperation(new Expense(3000, wallet.getCategory("Еда"), ""));
    wallet.addOperation(new Expense(2000, wallet.getCategory("Транспорт"), ""));
    wallet.addOperation(new Income(5000, wallet.getCategory("Бонус"), ""));

    assertEquals(10000, wallet.getBalance(), 0.01); // 10000+5000-3000-2000
    assertEquals(15000, wallet.getTotalIncome(), 0.01);
    assertEquals(5000, wallet.getTotalExpense(), 0.01);
  }

  // ========== ТЕСТЫ ДЛЯ БЮДЖЕТОВ ==========

  @Test
  void testSetBudget() {
    wallet.setBudget("ТестКатегория", 5000);
    Budget budget = wallet.getBudget("ТестКатегория");

    assertNotNull(budget);
    assertEquals(5000, budget.getLimit(), 0.01);
    assertEquals(0, budget.getSpent(), 0.01);
    assertEquals(5000, budget.getRemaining(), 0.01);
  }

  @Test
  void testSetBudget_WithExistingExpenses() {
    // Сначала добавляем расход
    wallet.addOperation(new Expense(2000, wallet.getCategory("ТестКатегория"), ""));

    // Затем устанавливаем бюджет
    wallet.setBudget("ТестКатегория", 5000);
    Budget budget = wallet.getBudget("ТестКатегория");

    assertEquals(2000, budget.getSpent(), 0.01);
    assertEquals(3000, budget.getRemaining(), 0.01);
  }

  @Test
  void testSetBudget_NonExistentCategory() {
    Exception exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              wallet.setBudget("Несуществующая", 5000);
            });
    assertTrue(exception.getMessage().contains("Категория не найдена"));
  }

  @Test
  void testEditBudget() {
    wallet.setBudget("ТестКатегория", 5000);
    wallet.editBudget("ТестКатегория", 10000);

    Budget budget = wallet.getBudget("ТестКатегория");
    assertEquals(10000, budget.getLimit(), 0.01);
  }

  @Test
  void testRemoveBudget() {
    wallet.setBudget("ТестКатегория", 5000);
    wallet.removeBudget("ТестКатегория");

    assertNull(wallet.getBudget("ТестКатегория"));
  }

  @Test
  void testBudgetExceeded() {
    wallet.setBudget("ТестКатегория", 1000);
    wallet.addOperation(new Expense(1200, wallet.getCategory("ТестКатегория"), ""));

    Budget budget = wallet.getBudget("ТестКатегория");
    assertTrue(budget.isExceeded());
    assertFalse(budget.isNearLimit());
  }

  @Test
  void testBudgetNearLimit() {
    wallet.setBudget("ТестКатегория", 1000);
    wallet.addOperation(new Expense(850, wallet.getCategory("ТестКатегория"), ""));

    Budget budget = wallet.getBudget("ТестКатегория");
    assertFalse(budget.isExceeded());
    assertTrue(budget.isNearLimit());
  }

  @Test
  void testBudgetWithinLimit() {
    wallet.setBudget("ТестКатегория", 1000);
    wallet.addOperation(new Expense(500, wallet.getCategory("ТестКатегория"), ""));

    Budget budget = wallet.getBudget("ТестКатегория");
    assertFalse(budget.isExceeded());
    assertFalse(budget.isNearLimit());
  }

  // ========== ТЕСТЫ ДЛЯ СТАТИСТИКИ ==========

  @Test
  void testGetIncomeByCategory() {
    wallet.addOperation(new Income(5000, wallet.getCategory("Зарплата"), ""));
    wallet.addOperation(new Income(3000, wallet.getCategory("Зарплата"), ""));
    wallet.addOperation(new Income(2000, wallet.getCategory("Бонус"), ""));

    assertEquals(8000, wallet.getIncomeByCategory("Зарплата"), 0.01);
    assertEquals(2000, wallet.getIncomeByCategory("Бонус"), 0.01);
    assertEquals(0, wallet.getIncomeByCategory("Несуществующая"), 0.01);
  }

  @Test
  void testGetExpenseByCategory() {
    wallet.addOperation(new Expense(3000, wallet.getCategory("Еда"), ""));
    wallet.addOperation(new Expense(2000, wallet.getCategory("Еда"), ""));
    wallet.addOperation(new Expense(1000, wallet.getCategory("Транспорт"), ""));

    assertEquals(5000, wallet.getExpenseByCategory("Еда"), 0.01);
    assertEquals(1000, wallet.getExpenseByCategory("Транспорт"), 0.01);
    assertEquals(0, wallet.getExpenseByCategory("Несуществующая"), 0.01);
  }

  @Test
  void testGetIncomeByCategories() {
    wallet.addOperation(new Income(5000, wallet.getCategory("Зарплата"), ""));
    wallet.addOperation(new Income(3000, wallet.getCategory("Зарплата"), ""));
    wallet.addOperation(new Income(2000, wallet.getCategory("Бонус"), ""));

    Map<String, Double> incomeMap = wallet.getIncomeByCategories();
    assertEquals(2, incomeMap.size());
    assertEquals(8000, incomeMap.get("Зарплата"), 0.01);
    assertEquals(2000, incomeMap.get("Бонус"), 0.01);
  }

  @Test
  void testGetExpenseByCategories() {
    wallet.addOperation(new Expense(3000, wallet.getCategory("Еда"), ""));
    wallet.addOperation(new Expense(2000, wallet.getCategory("Еда"), ""));
    wallet.addOperation(new Expense(1000, wallet.getCategory("Транспорт"), ""));

    Map<String, Double> expenseMap = wallet.getExpenseByCategories();
    assertEquals(2, expenseMap.size());
    assertEquals(5000, expenseMap.get("Еда"), 0.01);
    assertEquals(1000, expenseMap.get("Транспорт"), 0.01);
  }

  @Test
  void testGetOperationsByPeriod() {
    LocalDate today = LocalDate.now();
    LocalDate yesterday = today.minusDays(1);
    LocalDate tomorrow = today.plusDays(1);

    // Операции с разными датами
    wallet.addOperation(new Income(1000, wallet.getCategory("Зарплата"), ""));

    // Не должно быть проверки по датам в тесте, так как все операции создаются с текущей датой
    // Просто проверяем, что метод работает
    List<Operation> operations = wallet.getOperationsByPeriod(yesterday, tomorrow);
    assertFalse(operations.isEmpty());
  }

  @Test
  void testGetTotalIncomeByPeriod() {
    LocalDate today = LocalDate.now();
    LocalDate yesterday = today.minusDays(1);
    LocalDate tomorrow = today.plusDays(1);

    wallet.addOperation(new Income(1000, wallet.getCategory("Зарплата"), ""));
    wallet.addOperation(new Expense(500, wallet.getCategory("Еда"), ""));

    double income = wallet.getTotalIncomeByPeriod(yesterday, tomorrow);
    assertEquals(1000, income, 0.01);
  }

  @Test
  void testGetTotalExpenseByPeriod() {
    LocalDate today = LocalDate.now();
    LocalDate yesterday = today.minusDays(1);
    LocalDate tomorrow = today.plusDays(1);

    wallet.addOperation(new Income(1000, wallet.getCategory("Зарплата"), ""));
    wallet.addOperation(new Expense(500, wallet.getCategory("Еда"), ""));

    double expense = wallet.getTotalExpenseByPeriod(yesterday, tomorrow);
    assertEquals(500, expense, 0.01);
  }

  // ========== ТЕСТЫ ДЛЯ УВЕДОМЛЕНИЙ ==========

  @Test
  void testCheckBudgetExceeded_ExactLimit() {
    // Тест: расход в точности равен лимиту (ветка !isExceeded && !isNearLimit)
    wallet.setBudget("ТестКатегория", 1000);
    Expense expense = new Expense(1000, wallet.getCategory("ТестКатегория"), "Тест");
    wallet.addOperation(expense);

    List<String> notifications = wallet.getNotifications();
    assertTrue(
        notifications.isEmpty()
            || notifications.stream()
                .noneMatch(n -> n.contains("Превышен") || n.contains("исчерпан")));
  }

  @Test
  void testCheckBudgetExceeded_Exceeded() {
    // Тест: превышение бюджета (ветка isExceeded)
    wallet.setBudget("ТестКатегория", 1000);
    Expense expense = new Expense(1200, wallet.getCategory("ТестКатегория"), "Тест превышения");
    wallet.addOperation(expense);

    List<String> notifications = wallet.getAndClearNotifications();
    boolean hasExceedNotification =
        notifications.stream().anyMatch(n -> n.contains("Превышен") || n.contains("ВНИМАНИЕ"));
    assertTrue(hasExceedNotification, "Должно быть уведомление о превышении бюджета");
  }

  @Test
  void testCheckBudgetExceeded_NearLimit() {
    // Тест: приближение к лимиту (80%) (ветка isNearLimit)
    wallet.setBudget("ТестКатегория", 1000);
    Expense expense = new Expense(850, wallet.getCategory("ТестКатегория"), "Близко к лимиту");
    wallet.addOperation(expense);

    List<String> notifications = wallet.getAndClearNotifications();
    boolean hasNearLimitNotification =
        notifications.stream()
            .anyMatch(
                n -> n.contains("почти исчерпан") || n.contains("80%") || n.contains("Бюджет"));
    assertTrue(hasNearLimitNotification, "Должно быть уведомление о приближении к лимиту");
  }

  @Test
  void testCheckFinancialHealth_NegativeBalance() {
    // Тест: отрицательный баланс (ветка balance < 0)
    // Сначала добавляем небольшой доход, потом большой расход
    wallet.addOperation(new Income(500, wallet.getCategory("Зарплата"), "Мало"));
    wallet.addOperation(new Expense(1500, wallet.getCategory("ТестКатегория"), "Много"));

    List<String> notifications = wallet.getAndClearNotifications();
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
  void testCheckFinancialHealth_HighExpenseRatio() {
    // Тест: высокое отношение расходов к доходам (>90%) (ветка expensePercentage > 90)
    wallet.addOperation(new Income(1000, wallet.getCategory("Зарплата"), "Доход"));
    wallet.addOperation(new Expense(920, wallet.getCategory("ТестКатегория"), "Расход 92%"));

    List<String> notifications = wallet.getAndClearNotifications();

    // ищем любое уведомление о высоких расходах (содержит "Расходы составляют" и "%")
    boolean hasHighExpenseNotification =
        notifications.stream().anyMatch(n -> n.contains("Расходы составляют") && n.contains("%"));
    assertTrue(
        hasHighExpenseNotification,
        "Должно быть уведомление о высоких расходах (>90%). Получены: " + notifications);
  }

  @Test
  void testCheckFinancialHealth_LowBalanceWarning() {
    // Тест: баланс < 10% от дохода (ветка balance < totalIncome * 0.1)
    // Доход 1000, чтобы 10% = 100. Сделаем расход 950, баланс = 50 (<100)
    wallet.addOperation(new Income(1000, wallet.getCategory("Зарплата"), "Доход"));
    wallet.addOperation(new Expense(950, wallet.getCategory("ТестКатегория"), "Большой расход"));

    List<String> notifications = wallet.getAndClearNotifications();
    boolean hasLowBalanceNotification =
        notifications.stream().anyMatch(n -> n.contains("Баланс составляет менее 10%"));
    assertTrue(
        hasLowBalanceNotification,
        "Должно быть уведомление о низком балансе (<10% от дохода). Получены: " + notifications);
  }

  // ========== ТЕСТЫ ДЛЯ ФОРМАТИРОВАНИЯ ==========

  @Test
  void testFormatCurrency() {
    // Тест форматирования валюты как в ТЗ
    assertEquals("1,000.0", wallet.formatCurrency(1000));
    assertEquals("1,000.5", wallet.formatCurrency(1000.5));
    assertEquals("10,000.0", wallet.formatCurrency(10000));
    assertEquals("100,000.0", wallet.formatCurrency(100000));
  }

  @Test
  void testGetFormattedBalance() {
    wallet.addOperation(new Income(123456, wallet.getCategory("Зарплата"), ""));
    assertEquals("123,456.0", wallet.getFormattedBalance());
  }

  @Test
  void testGetFormattedTotalIncome() {
    wallet.addOperation(new Income(50000, wallet.getCategory("Зарплата"), ""));
    wallet.addOperation(new Income(30000, wallet.getCategory("Бонус"), ""));
    assertEquals("80,000.0", wallet.getFormattedTotalIncome());
  }

  @Test
  void testGetFormattedTotalExpense() {
    wallet.addOperation(new Income(100000, wallet.getCategory("Зарплата"), ""));
    wallet.addOperation(new Expense(30000, wallet.getCategory("Еда"), ""));
    wallet.addOperation(new Expense(20000, wallet.getCategory("Транспорт"), ""));
    assertEquals("50,000.0", wallet.getFormattedTotalExpense());
  }

  // ========== ТЕСТЫ ДЛЯ ПРИМЕРА ИЗ ТЗ ==========

  @Test
  void testGetBudgetSummaryAsInTZ_Order() {
    // Тест: порядок вывода бюджетов как в ТЗ
    wallet.setBudget("Коммунальные услуги", 2500);
    wallet.setBudget("Еда", 4000);
    wallet.setBudget("Развлечения", 3000);

    String summary = wallet.getBudgetSummaryAsInTZ();

    int indexCom = summary.indexOf("Коммунальные услуги:");
    int indexFood = summary.indexOf("Еда:");
    int indexEnt = summary.indexOf("Развлечения:");

    assertTrue(
        indexCom < indexFood && indexFood < indexEnt,
        "Порядок должен быть: Коммунальные услуги, Еда, Развлечения");
  }

  @Test
  void testGetBudgetSummaryAsInTZ_Content() {
    // Тест: содержание вывода как в ТЗ
    wallet.addOperation(new Income(20000, wallet.getCategory("Зарплата"), ""));
    wallet.addOperation(new Income(40000, wallet.getCategory("Зарплата"), ""));
    wallet.addOperation(new Income(3000, wallet.getCategory("Бонус"), ""));

    wallet.addOperation(new Expense(300, wallet.getCategory("Еда"), ""));
    wallet.addOperation(new Expense(500, wallet.getCategory("Еда"), ""));
    wallet.addOperation(new Expense(3000, wallet.getCategory("Развлечения"), ""));
    wallet.addOperation(new Expense(3000, wallet.getCategory("Коммунальные услуги"), ""));
    wallet.addOperation(new Expense(1500, wallet.getCategory("Такси"), ""));

    wallet.setBudget("Еда", 4000);
    wallet.setBudget("Развлечения", 3000);
    wallet.setBudget("Коммунальные услуги", 2500);

    String summary = wallet.getBudgetSummaryAsInTZ();

    // Проверяем ключевые элементы
    assertTrue(summary.contains("Общий доход: 63,000.0"));
    assertTrue(summary.contains("Зарплата: 60,000.0"));
    assertTrue(summary.contains("Общие расходы: 8,300.0"));
    assertTrue(summary.contains("Коммунальные услуги: 2,500.0"));
    assertTrue(summary.contains("Еда: 4,000.0"));
    assertTrue(summary.contains("Развлечения: 3,000.0"));
  }

  // ========== ТЕСТЫ ДЛЯ ПУСТЫХ/НУЛЕВЫХ ЗНАЧЕНИЙ ==========

  @Test
  void testEmptyWallet() {
    // Тест пустого кошелька
    assertEquals(0, wallet.getBalance(), 0.01);
    assertEquals(0, wallet.getTotalIncome(), 0.01);
    assertEquals(0, wallet.getTotalExpense(), 0.01);
    assertTrue(wallet.getOperations().isEmpty());
  }

  @Test
  void testGetBudgets_Empty() {
    assertTrue(wallet.getBudgets().isEmpty());
  }

  @Test
  void testToString() {
    // Тест метода toString
    String str = wallet.toString();
    assertNotNull(str);
    assertTrue(str.contains("Wallet") || str.contains("balance"));
  }

  // ========== ТЕСТЫ ГЕТТЕРОВ И СЕТТЕРОВ ==========

  @Test
  void testSettersAndGetters() {
    // Тест сеттеров и геттеров
    wallet.setBalance(5000);
    assertEquals(5000, wallet.getBalance(), 0.01);

    // Проверяем, что операции можно установить
    List<Operation> operations = List.of(new Income(1000, wallet.getCategory("Зарплата"), ""));
    wallet.setOperations(operations);
    assertEquals(1, wallet.getOperations().size());

    // Проверяем категории - используем правильный ключ
    Category newCategory = new Category("Новая", "");
    Map<String, Category> categories = new HashMap<>();
    categories.put("новая", newCategory); // Ключ в нижнем регистре!
    wallet.setCategories(categories);
    assertTrue(wallet.hasCategory("Новая")); // Теперь должно работать

    // Проверяем бюджеты
    Budget budget = new Budget(wallet.getCategory("ТестКатегория"), 5000);
    Map<String, Budget> budgets = new HashMap<>();
    budgets.put("тесткатегория", budget); // Ключ в нижнем регистре
    wallet.setBudgets(budgets);
    assertNotNull(wallet.getBudget("ТестКатегория"));
  }
}
