package com.finance.service;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

import com.finance.core.*;
import com.finance.exception.FinanceException;

public class FinanceManager {
  private Map<String, User> users;
  private User currentUser;
  private AuthService authService;
  private DataStorage dataStorage;
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
  private List<String> notifications;

  public FinanceManager() {
    this.users = new HashMap<>();
    this.authService = new AuthService();
    this.dataStorage = new DataStorage();
    this.notifications = new ArrayList<>();
    loadUsers();
  }

  private void loadUsers() {
    try {
      Map<String, User> loadedUsers = dataStorage.loadUsers();
      if (loadedUsers != null) {
        users = loadedUsers;
        addNotification("Загружено пользователей: " + users.size());
      }
    } catch (Exception e) {
      addNotification("Не удалось загрузить пользователей: " + e.getMessage());
    }
  }

  private void addNotification(String message) {
    notifications.add(message);
  }

  public List<String> getAndClearNotifications() {
    List<String> result = new ArrayList<>(notifications);
    notifications.clear();
    return result;
  }

  public boolean register(String login, String password) {
    if (users.containsKey(login)) {
      throw new FinanceException("Пользователь с таким логином уже существует");
    }
    if (!authService.validateCredentials(login, password)) {
      throw new FinanceException("Некорректные логин или пароль");
    }
    User user = new User(login, password);
    users.put(login, user);
    dataStorage.saveUsers(users);
    addNotification("✅ Регистрация успешна!");
    return true;
  }

  public boolean login(String login, String password) {
    User user = users.get(login);
    if (user == null) {
      throw new FinanceException("Пользователь не найден");
    }
    if (!user.authenticate(password)) {
      throw new FinanceException("Неверный пароль");
    }
    currentUser = user;
    addNotification("✅ Добро пожаловать, " + login + "!");
    currentUser.getWallet().checkFinancialHealth();
    List<String> walletNotifications = currentUser.getWallet().getAndClearNotifications();
    notifications.addAll(walletNotifications);
    return true;
  }

  public void logout() {
    if (currentUser != null) {
      dataStorage.saveUsers(users);
      addNotification("👋 До свидания, " + currentUser.getLogin() + "!");
      currentUser = null;
    }
  }

  public void addIncome(String categoryName, double amount, String description) {
    checkAuth();
    Wallet wallet = currentUser.getWallet();
    if (!wallet.hasCategory(categoryName)) {
      addNotification("⚠️  Категория не найдена. Создана новая категория: " + categoryName);
      wallet.addCategory(new Category(categoryName, ""));
    }
    Income income = new Income(amount, wallet.getCategory(categoryName), description);
    wallet.addOperation(income);
    addNotification("✅ Доход добавлен: " + income);
    notifications.addAll(wallet.getAndClearNotifications());
    dataStorage.saveUsers(users);
  }

  public void addExpense(String categoryName, double amount, String description) {
    checkAuth();
    Wallet wallet = currentUser.getWallet();
    if (!wallet.hasCategory(categoryName)) {
      addNotification("⚠️  Категория не найдена. Создана новая категория: " + categoryName);
      wallet.addCategory(new Category(categoryName, ""));
    }
    Expense expense = new Expense(amount, wallet.getCategory(categoryName), description);
    wallet.addOperation(expense);
    addNotification("✅ Расход добавлен: " + expense);
    notifications.addAll(wallet.getAndClearNotifications());
    dataStorage.saveUsers(users);
  }

  public void setBudget(String categoryName, double limit) {
    checkAuth();
    Wallet wallet = currentUser.getWallet();
    if (!wallet.hasCategory(categoryName)) {
      addNotification("⚠️  Категория не найдена. Создана новая категория: " + categoryName);
      wallet.addCategory(new Category(categoryName, ""));
    }
    wallet.setBudget(categoryName, limit);
    addNotification("✅ Бюджет установлен: " + wallet.getBudget(categoryName));
    dataStorage.saveUsers(users);
  }

  public void editBudget(String categoryName, double newLimit) {
    checkAuth();
    Wallet wallet = currentUser.getWallet();
    Budget budget = wallet.getBudget(categoryName);
    if (budget == null) {
      throw new FinanceException("Бюджет для категории не найден");
    }
    wallet.editBudget(categoryName, newLimit);
    addNotification("✅ Бюджет обновлен: " + wallet.getBudget(categoryName));
    dataStorage.saveUsers(users);
  }

  public void removeBudget(String categoryName) {
    checkAuth();
    Wallet wallet = currentUser.getWallet();
    if (wallet.getBudget(categoryName) == null) {
      throw new FinanceException("Бюджет для категории не найден");
    }
    wallet.removeBudget(categoryName);
    addNotification("✅ Бюджет удален для категории: " + categoryName);
    dataStorage.saveUsers(users);
  }

  public void transfer(String toUserLogin, double amount, String description) {
    checkAuth();
    if (currentUser.getLogin().equals(toUserLogin)) {
      throw new FinanceException("Нельзя перевести деньги самому себе");
    }
    User toUser = users.get(toUserLogin);
    if (toUser == null) {
      throw new FinanceException("Получатель не найден");
    }
    Wallet fromWallet = currentUser.getWallet();
    Wallet toWallet = toUser.getWallet();
    if (fromWallet.getBalance() < amount) {
      throw new FinanceException("Недостаточно средств на балансе");
    }
    Category transferCategory = fromWallet.getCategory("Прочее");
    Expense expense =
        new Expense(
            amount, transferCategory, "Перевод пользователю " + toUserLogin + ": " + description);
    fromWallet.addOperation(expense);
    Category incomeCategory = toWallet.getCategory("Прочее");
    Income income =
        new Income(
            amount,
            incomeCategory,
            "Перевод от пользователя " + currentUser.getLogin() + ": " + description);
    toWallet.addOperation(income);
    Transfer transfer = new Transfer(currentUser.getLogin(), toUserLogin, amount, description);
    addNotification("✅ Перевод выполнен: " + transfer);
    dataStorage.saveUsers(users);
  }

  public void showBalance() {
    checkAuth();
    Wallet wallet = currentUser.getWallet();
    System.out.println("\n══════════════════════════════════════════════");
    System.out.println("                 БАЛАНС");
    System.out.println("══════════════════════════════════════════════");
    System.out.printf("💰 Текущий баланс: %s\n", wallet.getFormattedBalance());
    System.out.printf("📈 Общий доход: %s\n", wallet.getFormattedTotalIncome());
    System.out.printf("📉 Общий расход: %s\n", wallet.getFormattedTotalExpense());
    System.out.println("══════════════════════════════════════════════");
  }

  public void showExampleFromTZ() {
    checkAuth();
    Wallet wallet = currentUser.getWallet();
    System.out.println("\n" + wallet.getBudgetSummaryAsInTZ());
  }

  public void showStatistics(List<String> categoryNames, LocalDate startDate, LocalDate endDate) {
    checkAuth();
    Wallet wallet = currentUser.getWallet();

    System.out.println("\n══════════════════════════════════════════════");
    if (startDate != null && endDate != null) {
      System.out.println(
          "СТАТИСТИКА за период "
              + startDate.format(DATE_FORMATTER)
              + " - "
              + endDate.format(DATE_FORMATTER));
    } else {
      System.out.println("                СТАТИСТИКА");
    }
    System.out.println("══════════════════════════════════════════════");

    if (categoryNames.isEmpty()) {
      Map<String, Double> incomeByCat = wallet.getIncomeByCategories();
      Map<String, Double> expenseByCat = wallet.getExpenseByCategories();

      if (!incomeByCat.isEmpty()) {
        System.out.println("\n📊 ДОХОДЫ по категориям:");
        incomeByCat.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .forEach(
                entry ->
                    System.out.printf(
                        "   %-20s %15s\n",
                        entry.getKey(), wallet.formatCurrency(entry.getValue())));
      }

      if (!expenseByCat.isEmpty()) {
        System.out.println("\n📊 РАСХОДЫ по категориям:");
        expenseByCat.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .forEach(
                entry ->
                    System.out.printf(
                        "   %-20s %15s\n",
                        entry.getKey(), wallet.formatCurrency(entry.getValue())));
      }

      if (!wallet.getBudgets().isEmpty()) {
        System.out.println("\n🎯 БЮДЖЕТЫ по категориям:");
        wallet.getBudgets().values().stream()
            .sorted(Comparator.comparing(b -> b.getCategory().getName()))
            .forEach(
                budget -> {
                  // 🔧 ИСПРАВЛЕНО: Объявлены обе переменные
                  String statusIcon =
                      budget.isExceeded() ? "🔴" : budget.isNearLimit() ? "🟡" : "🟢";
                  String statusText =
                      budget.isExceeded()
                          ? "ПРЕВЫШЕН"
                          : budget.isNearLimit() ? "ПОЧТИ ИСЧЕРПАН" : "В НОРМЕ";
                  System.out.printf("   %s %s [%s]\n", statusIcon, budget, statusText);
                });
      }
    } else {
      System.out.println("\n📊 СТАТИСТИКА по выбранным категориям:");
      for (String catName : categoryNames) {
        if (!wallet.hasCategory(catName)) {
          System.out.println("   ❌ Категория не найдена: " + catName);
          continue;
        }
        double income = wallet.getIncomeByCategory(catName);
        double expense = wallet.getExpenseByCategory(catName);
        System.out.printf(
            "   %-20s Доходы: %10s, Расходы: %10s\n",
            catName, wallet.formatCurrency(income), wallet.formatCurrency(expense));
      }
    }
    System.out.println("══════════════════════════════════════════════");
  }

  public void showBudgets() {
    checkAuth();
    Wallet wallet = currentUser.getWallet();
    System.out.println("\n══════════════════════════════════════════════");
    System.out.println("                 БЮДЖЕТЫ");
    System.out.println("══════════════════════════════════════════════");
    if (wallet.getBudgets().isEmpty()) {
      System.out.println("ℹ️  Бюджеты не установлены");
    } else {
      wallet.getBudgets().values().stream()
          .sorted(Comparator.comparing(b -> b.getCategory().getName()))
          .forEach(
              budget -> {
                String statusIcon = budget.isExceeded() ? "🔴" : budget.isNearLimit() ? "🟡" : "🟢";
                String statusText =
                    budget.isExceeded()
                        ? "ПРЕВЫШЕН"
                        : budget.isNearLimit() ? "ПОЧТИ ИСЧЕРПАН" : "В НОРМЕ";
                System.out.printf("%s %s [%s]\n", statusIcon, budget, statusText);
              });
    }
    System.out.println("══════════════════════════════════════════════");
  }

  public void addCategory(String name, String description) {
    checkAuth();
    Wallet wallet = currentUser.getWallet();
    if (wallet.hasCategory(name)) {
      throw new FinanceException("Категория уже существует");
    }
    wallet.addCategory(new Category(name, description));
    addNotification("✅ Категория добавлена: " + name);
    dataStorage.saveUsers(users);
  }

  public void editCategory(String oldName, String newName, String newDescription) {
    checkAuth();
    Wallet wallet = currentUser.getWallet();

    if (!wallet.hasCategory(oldName)) {
      throw new FinanceException("Категория не найдена: " + oldName);
    }

    Category oldCategory = wallet.getCategory(oldName);

    if (oldName.equalsIgnoreCase(newName)) {
      // Только обновляем описание
      oldCategory.setDescription(newDescription);
      addNotification("✅ Категория обновлена: " + oldName);
      dataStorage.saveUsers(users);
      return;
    }

    // Проверяем, не существует ли уже категория с новым именем
    if (wallet.hasCategory(newName)) {
      throw new FinanceException("Категория с именем '" + newName + "' уже существует");
    }

    // Создаем новую категорию
    Category newCategory = new Category(newName, newDescription);
    wallet.addCategory(newCategory);

    // Изменяем категорию во всех операциях (без пересоздания операций!)
    for (Operation op : wallet.getOperations()) {
      if (op.getCategory().equals(oldCategory)) {
        op.setCategory(newCategory);
      }
    }

    // Переносим бюджет
    Budget oldBudget = wallet.getBudget(oldName);
    if (oldBudget != null) {
      wallet.removeBudget(oldName);
      wallet.setBudget(newName, oldBudget.getLimit());
      double spent = oldBudget.getSpent();
      if (spent > 0) {
        wallet.getBudget(newName).addExpense(spent);
      }
    }

    // Удаляем старую категорию
    wallet.removeCategory(oldName);

    addNotification("✅ Категория обновлена: " + newName);
    dataStorage.saveUsers(users);
  }

  public void exportToFile(String filename, String format) {
    checkAuth();
    if ("csv".equalsIgnoreCase(format)) {
      dataStorage.exportToCSV(currentUser.getWallet(), filename);
      dataStorage.exportBudgetsToCSV(currentUser.getWallet(), filename);
    } else if ("json".equalsIgnoreCase(format)) {
      dataStorage.exportToJSON(currentUser.getWallet(), filename);
    } else {
      dataStorage.exportWallet(currentUser.getWallet(), filename);
    }
  }

  public void importFromFile(String filename, String format) {
    checkAuth();
    try {
      String fullPath = filename;
      if (!filename.startsWith("exports/") && !new File(filename).exists()) {
        if ("json".equalsIgnoreCase(format)) {
          fullPath = "exports/" + (filename.endsWith(".json") ? filename : filename + ".json");
        } else {
          fullPath = "exports/" + (filename.endsWith(".dat") ? filename : filename + ".dat");
        }
      }
      Wallet importedWallet;
      if ("json".equalsIgnoreCase(format)) {
        importedWallet = dataStorage.importFromJSON(fullPath);
      } else {
        importedWallet = dataStorage.importWallet(fullPath);
      }
      currentUser.setWallet(importedWallet);
      addNotification("✅ Данные успешно импортированы из файла: " + fullPath);
      dataStorage.saveUsers(users);
    } catch (Exception e) {
      throw new FinanceException("Ошибка при импорте: " + e.getMessage());
    }
  }

  public void showOperations(LocalDate startDate, LocalDate endDate, String category) {
    checkAuth();
    Wallet wallet = currentUser.getWallet();
    List<Operation> operations;
    if (startDate != null && endDate != null) {
      operations = wallet.getOperationsByPeriod(startDate, endDate);
    } else {
      operations = wallet.getOperations();
    }
    if (category != null && !category.isEmpty()) {
      operations =
          operations.stream()
              .filter(op -> op.getCategory().getName().equalsIgnoreCase(category))
              .toList();
    }
    System.out.println("\n══════════════════════════════════════════════");
    System.out.println("               ОПЕРАЦИИ");
    if (startDate != null && endDate != null) {
      System.out.println(
          "За период: "
              + startDate.format(DATE_FORMATTER)
              + " - "
              + endDate.format(DATE_FORMATTER));
    }
    if (category != null && !category.isEmpty()) {
      System.out.println("Категория: " + category);
    }
    System.out.println("══════════════════════════════════════════════");
    if (operations.isEmpty()) {
      System.out.println("ℹ️  Операции не найдены");
    } else {
      operations.stream()
          .sorted((a, b) -> b.getDateTime().compareTo(a.getDateTime()))
          .forEach(
              op -> {
                String type = op instanceof Income ? "📈 ДОХОД" : "📉 РАСХОД";
                System.out.printf(
                    "%s: %-15s %10s - %s\n",
                    type,
                    op.getCategory().getName(),
                    wallet.formatCurrency(op.getAmount()),
                    op.getDescription());
              });
    }
    System.out.println("══════════════════════════════════════════════");
    System.out.println("Всего операций: " + operations.size());
  }

  public void showDetailedReport() {
    checkAuth();
    Wallet wallet = currentUser.getWallet();
    System.out.println("\n══════════════════════════════════════════════════════════════");
    System.out.println("               ДЕТАЛЬНЫЙ ОТЧЕТ");
    System.out.println("══════════════════════════════════════════════════════════════");
    System.out.println("\n📊 ОБЩАЯ ИНФОРМАЦИЯ:");
    System.out.printf("   Баланс: %s\n", wallet.getFormattedBalance());
    System.out.printf("   Всего доходов: %s\n", wallet.getFormattedTotalIncome());
    System.out.printf("   Всего расходов: %s\n", wallet.getFormattedTotalExpense());
    System.out.printf("   Всего операций: %d\n", wallet.getOperations().size());
    Map<String, Double> expenses = wallet.getExpenseByCategories();
    if (!expenses.isEmpty()) {
      System.out.println("\n📉 ТОП-5 КАТЕГОРИЙ ПО РАСХОДАМ:");
      expenses.entrySet().stream()
          .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
          .limit(5)
          .forEach(
              entry ->
                  System.out.printf(
                      "   %-20s %15s\n", entry.getKey(), wallet.formatCurrency(entry.getValue())));
    }
    if (!wallet.getBudgets().isEmpty()) {
      System.out.println("\n🎯 СТАТУС БЮДЖЕТОВ:");
      wallet.getBudgets().values().stream()
          .sorted(Comparator.comparing(Budget::getUsagePercentage).reversed())
          .forEach(
              budget -> {
                String status =
                    budget.isExceeded()
                        ? "🔴 ПРЕВЫШЕН"
                        : budget.isNearLimit() ? "🟡 ВНИМАНИЕ" : "🟢 НОРМА";
                System.out.printf(
                    "   %-20s %6.0f%% %s\n",
                    budget.getCategory().getName(), budget.getUsagePercentage(), status);
              });
    }
    System.out.println("\n❤️  ФИНАНСОВОЕ ЗДОРОВЬЕ:");
    double expenseRatio =
        wallet.getTotalIncome() > 0
            ? (wallet.getTotalExpense() / wallet.getTotalIncome()) * 100
            : 0;
    System.out.printf("   Соотношение расходов к доходам: %.1f%%\n", expenseRatio);
    if (expenseRatio > 80) {
      System.out.println("   ⚠️  Высокий уровень расходов (более 80% от доходов)");
    } else if (expenseRatio < 50) {
      System.out.println("   ✅ Хороший уровень сбережений");
    }
    System.out.println("══════════════════════════════════════════════════════════════");
  }

  private void checkAuth() {
    if (currentUser == null) {
      throw new FinanceException("Требуется авторизация. Используйте команду 'login'");
    }
  }

  public LocalDate parseDate(String dateStr) {
    try {
      return LocalDate.parse(dateStr, DATE_FORMATTER);
    } catch (DateTimeParseException e) {
      throw new FinanceException("Неверный формат даты. Используйте ДД.ММ.ГГГГ");
    }
  }

  public User getCurrentUser() {
    return currentUser;
  }

  public boolean isAuthenticated() {
    return currentUser != null;
  }
}
