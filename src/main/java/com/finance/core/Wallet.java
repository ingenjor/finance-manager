package com.finance.core;

import java.io.Serializable;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.Locale;
import java.util.stream.Collectors;

public class Wallet implements Serializable {
  private static final long serialVersionUID = 1L;

  private double balance;
  private Map<String, Category> categories;
  private Map<String, Budget> budgets;
  private List<Operation> operations;
  private transient List<String> notifications;

  public Wallet() {
    this.balance = 0.0;
    this.categories = new HashMap<>();
    this.budgets = new HashMap<>();
    this.operations = new ArrayList<>();
    this.notifications = new ArrayList<>();
    initializeDefaultCategories();
  }

  // 🔧 Метод для инициализации notifications после десериализации
  public void initializeNotifications() {
    if (notifications == null) {
      notifications = new ArrayList<>();
    }
  }

  private void initializeDefaultCategories() {
    addCategory(new Category("Еда", "Расходы на продукты питания"));
    addCategory(new Category("Развлечения", "Кино, театры, концерты"));
    addCategory(new Category("Транспорт", "Транспортные расходы"));
    addCategory(new Category("Коммунальные услуги", "Квартплата, электричество"));
    addCategory(new Category("Такси", "Поездки на такси"));
    addCategory(new Category("Зарплата", "Основной доход"));
    addCategory(new Category("Бонус", "Дополнительный доход"));
    addCategory(new Category("Прочее", "Прочие доходы/расходы"));
  }

  public void addCategory(Category category) {
    categories.put(category.getName().toLowerCase(), category);
  }

  public void removeCategory(String categoryName) {
    categories.remove(categoryName.toLowerCase());
  }

  public boolean hasCategory(String categoryName) {
    return categories.containsKey(categoryName.toLowerCase());
  }

  public Category getCategory(String categoryName) {
    return categories.get(categoryName.toLowerCase());
  }

  public List<Category> getCategories() {
    return new ArrayList<>(categories.values());
  }

  public Map<String, Category> getCategoriesMap() {
    return new HashMap<>(categories);
  }

  public void setCategories(Map<String, Category> categories) {
    this.categories = categories != null ? categories : new HashMap<>();
  }

  public void addOperation(Operation operation) {
    operations.add(operation);
    if (operation instanceof Income) {
      balance += operation.getAmount();
    } else if (operation instanceof Expense) {
      balance -= operation.getAmount();
      checkBudgetExceeded((Expense) operation);
    }
    checkFinancialHealth();
  }

  private void checkBudgetExceeded(Expense expense) {
    initializeNotifications();
    String categoryName = expense.getCategory().getName().toLowerCase();
    if (budgets.containsKey(categoryName)) {
      Budget budget = budgets.get(categoryName);
      budget.addExpense(expense.getAmount());
      if (budget.isExceeded()) {
        notifications.add(
            "\n⚠️  ВНИМАНИЕ: Превышен бюджет по категории '"
                + expense.getCategory().getName()
                + "'!");
        notifications.add(
            "   Потрачено: "
                + formatCurrency(budget.getSpent())
                + ", Лимит: "
                + formatCurrency(budget.getLimit()));
      } else if (budget.isNearLimit()) {
        notifications.add(
            "\nℹ️  ИНФО: Бюджет по категории '"
                + expense.getCategory().getName()
                + "' почти исчерпан!");
        notifications.add(
            "   Использовано: "
                + formatCurrency(budget.getSpent())
                + " из "
                + formatCurrency(budget.getLimit())
                + " ("
                + String.format("%.0f", (budget.getSpent() / budget.getLimit() * 100))
                + "%)");
      }
    }
  }

  public void checkFinancialHealth() {
    initializeNotifications();
    if (balance < 0) {
      notifications.add(
          "\n🚨 КРИТИЧЕСКОЕ ПРЕДУПРЕЖДЕНИЕ: Отрицательный баланс! Расходы превысили доходы!");
      notifications.add("   Текущий баланс: " + formatCurrency(balance));
    }
    double totalExpense = getTotalExpense();
    double totalIncome = getTotalIncome();
    if (totalIncome > 0) {
      double expensePercentage = (totalExpense / totalIncome) * 100;
      if (expensePercentage > 90) {
        notifications.add(
            "\n⚠️  ВНИМАНИЕ: Расходы составляют "
                + String.format("%.1f", expensePercentage)
                + "% от доходов!");
      }
      if (balance < totalIncome * 0.1) {
        notifications.add("\nℹ️  ИНФО: Баланс составляет менее 10% от общего дохода");
      }
    }
  }

  public List<String> getAndClearNotifications() {
    initializeNotifications();
    List<String> result = new ArrayList<>(notifications);
    notifications.clear();
    return result;
  }

  public List<String> getNotifications() {
    initializeNotifications();
    return new ArrayList<>(notifications);
  }

  public void setBudget(String categoryName, double limit) {
    if (!hasCategory(categoryName)) {
      throw new IllegalArgumentException("Категория не найдена: " + categoryName);
    }
    double spent =
        operations.stream()
            .filter(op -> op instanceof Expense)
            .filter(op -> op.getCategory().getName().equalsIgnoreCase(categoryName))
            .mapToDouble(Operation::getAmount)
            .sum();
    budgets.put(categoryName.toLowerCase(), new Budget(getCategory(categoryName), limit, spent));
  }

  public void editBudget(String categoryName, double newLimit) {
    if (!budgets.containsKey(categoryName.toLowerCase())) {
      throw new IllegalArgumentException("Бюджет для категории не установлен: " + categoryName);
    }
    budgets.get(categoryName.toLowerCase()).updateLimit(newLimit);
  }

  public void removeBudget(String categoryName) {
    budgets.remove(categoryName.toLowerCase());
  }

  public Budget getBudget(String categoryName) {
    return budgets.get(categoryName.toLowerCase());
  }

  public Map<String, Budget> getBudgets() {
    return new HashMap<>(budgets);
  }

  public void setBudgets(Map<String, Budget> budgets) {
    this.budgets = budgets != null ? budgets : new HashMap<>();
  }

  public double getBalance() {
    return balance;
  }

  public void setBalance(double balance) {
    this.balance = balance;
  }

  public List<Operation> getOperations() {
    return operations;
  }

  public void setOperations(List<Operation> operations) {
    this.operations = operations != null ? operations : new ArrayList<>();
  }

  public double getTotalIncome() {
    return operations.stream()
        .filter(op -> op instanceof Income)
        .mapToDouble(Operation::getAmount)
        .sum();
  }

  public double getTotalExpense() {
    return operations.stream()
        .filter(op -> op instanceof Expense)
        .mapToDouble(Operation::getAmount)
        .sum();
  }

  public double getIncomeByCategory(String categoryName) {
    return operations.stream()
        .filter(
            op -> op instanceof Income && op.getCategory().getName().equalsIgnoreCase(categoryName))
        .mapToDouble(Operation::getAmount)
        .sum();
  }

  public double getExpenseByCategory(String categoryName) {
    return operations.stream()
        .filter(
            op ->
                op instanceof Expense && op.getCategory().getName().equalsIgnoreCase(categoryName))
        .mapToDouble(Operation::getAmount)
        .sum();
  }

  public Map<String, Double> getIncomeByCategories() {
    return operations.stream()
        .filter(op -> op instanceof Income)
        .collect(
            Collectors.groupingBy(
                op -> op.getCategory().getName(), Collectors.summingDouble(Operation::getAmount)));
  }

  public Map<String, Double> getExpenseByCategories() {
    return operations.stream()
        .filter(op -> op instanceof Expense)
        .collect(
            Collectors.groupingBy(
                op -> op.getCategory().getName(), Collectors.summingDouble(Operation::getAmount)));
  }

  public List<Operation> getOperationsByPeriod(LocalDate startDate, LocalDate endDate) {
    return operations.stream()
        .filter(
            op ->
                !op.getDateTime().toLocalDate().isBefore(startDate)
                    && !op.getDateTime().toLocalDate().isAfter(endDate))
        .collect(Collectors.toList());
  }

  public double getTotalIncomeByPeriod(LocalDate startDate, LocalDate endDate) {
    return getOperationsByPeriod(startDate, endDate).stream()
        .filter(op -> op instanceof Income)
        .mapToDouble(Operation::getAmount)
        .sum();
  }

  public double getTotalExpenseByPeriod(LocalDate startDate, LocalDate endDate) {
    return getOperationsByPeriod(startDate, endDate).stream()
        .filter(op -> op instanceof Expense)
        .mapToDouble(Operation::getAmount)
        .sum();
  }

  // 🔧 Метод для пересчета баланса при необходимости
  public void recalculateBalance() {
    this.balance = 0.0;
    for (Operation op : operations) {
      if (op instanceof Income) {
        this.balance += op.getAmount();
      } else if (op instanceof Expense) {
        this.balance -= op.getAmount();
      }
    }
  }

  public String formatCurrency(double amount) {
    NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
    nf.setMinimumFractionDigits(1);
    nf.setMaximumFractionDigits(1);
    nf.setGroupingUsed(true);
    return nf.format(amount);
  }

  public String getFormattedBalance() {
    return formatCurrency(balance);
  }

  public String getFormattedTotalIncome() {
    return formatCurrency(getTotalIncome());
  }

  public String getFormattedTotalExpense() {
    return formatCurrency(getTotalExpense());
  }

  public String getBudgetSummaryAsInTZ() {
    StringBuilder sb = new StringBuilder();
    sb.append("Общий доход: ").append(formatCurrency(getTotalIncome())).append("\n");
    Map<String, Double> incomeByCat = getIncomeByCategories();
    if (!incomeByCat.isEmpty()) {
      sb.append("Доходы по категориям:\n");
      incomeByCat.entrySet().stream()
          .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
          .forEach(
              entry ->
                  sb.append(entry.getKey())
                      .append(": ")
                      .append(formatCurrency(entry.getValue()))
                      .append("\n"));
    }
    sb.append("Общие расходы: ").append(formatCurrency(getTotalExpense())).append("\n");
    if (!budgets.isEmpty()) {
      sb.append("Бюджет по категориям:\n");
      List<String> orderFromTZ = Arrays.asList("Коммунальные услуги", "Еда", "Развлечения");
      orderFromTZ.forEach(
          categoryName -> {
            String key = categoryName.toLowerCase();
            if (budgets.containsKey(key)) {
              Budget budget = budgets.get(key);
              String remainingStr = formatCurrency(budget.getRemaining());
              if (budget.getRemaining() < 0) {
                remainingStr = "-" + formatCurrency(Math.abs(budget.getRemaining()));
              }
              sb.append(budget.getCategory().getName())
                  .append(": ")
                  .append(formatCurrency(budget.getLimit()))
                  .append(", Оставшийся бюджет: ")
                  .append(remainingStr)
                  .append("\n");
            }
          });
      budgets.entrySet().stream()
          .filter(entry -> !orderFromTZ.contains(entry.getKey()))
          .sorted((a, b) -> a.getKey().compareTo(b.getKey()))
          .forEach(
              entry -> {
                Budget budget = entry.getValue();
                String remainingStr = formatCurrency(budget.getRemaining());
                if (budget.getRemaining() < 0) {
                  remainingStr = "-" + formatCurrency(Math.abs(budget.getRemaining()));
                }
                sb.append(budget.getCategory().getName())
                    .append(": ")
                    .append(formatCurrency(budget.getLimit()))
                    .append(", Оставшийся бюджет: ")
                    .append(remainingStr)
                    .append("\n");
              });
    }
    return sb.toString();
  }

  @Override
  public String toString() {
    return String.format(
        "Wallet{balance=%s, operations=%d, categories=%d, budgets=%d}",
        formatCurrency(balance), operations.size(), categories.size(), budgets.size());
  }
}
