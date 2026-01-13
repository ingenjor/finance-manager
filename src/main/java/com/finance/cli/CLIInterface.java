package com.finance.cli;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import com.finance.exception.FinanceException;
import com.finance.service.FinanceManager;

public class CLIInterface {
  private FinanceManager financeManager;
  private Scanner scanner;
  private boolean running;
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

  public CLIInterface(FinanceManager financeManager) {
    this.financeManager = financeManager;
    this.running = true;
    this.scanner = new Scanner(new InputStreamReader(System.in, StandardCharsets.UTF_8));
  }

  // Метод для тестирования - позволяет установить сканер
  void setScanner(Scanner scanner) {
    this.scanner = scanner;
  }

  // Метод для тестирования - позволяет проверить состояние running
  boolean isRunning() {
    return running;
  }

  public void start() {
    printWelcome();
    printHelp();

    while (running) {
      try {
        showNotifications();

        System.out.print("\n> ");
        System.out.flush();

        if (!scanner.hasNextLine()) {
          try {
            Thread.sleep(100);
          } catch (InterruptedException e) {
            // Игнорируем
          }
          continue;
        }

        String input = scanner.nextLine().trim();

        if (input.isEmpty()) {
          continue;
        }

        String[] parts = input.split("\\s+", 2);
        String command = parts[0].toLowerCase();
        String args = parts.length > 1 ? parts[1] : "";

        processCommand(command, args);
      } catch (FinanceException e) {
        System.out.println("❌ Ошибка: " + e.getMessage());
      } catch (NumberFormatException e) {
        System.out.println("❌ Неверный формат числа");
      } catch (NoSuchElementException e) {
        System.out.println("Ввод завершен.");
        running = false;
      } catch (Exception e) {
        System.out.println("❌ Неизвестная ошибка: " + e.getMessage());
        if (e.getCause() != null) {
          System.out.println("   Причина: " + e.getCause().getMessage());
        }
      }
    }

    scanner.close();
  }

  // public для тестирования
  public void processCommand(String command, String args) {
    switch (command) {
      case "register":
        handleRegister(args);
        break;
      case "login":
        handleLogin(args);
        break;
      case "logout":
        handleLogout();
        break;
      case "add_income":
        handleAddIncome(args);
        break;
      case "add_expense":
        handleAddExpense(args);
        break;
      case "set_budget":
        handleSetBudget(args);
        break;
      case "edit_budget":
        handleEditBudget(args);
        break;
      case "remove_budget":
        handleRemoveBudget(args);
        break;
      case "balance":
        handleBalance();
        break;
      case "stats":
        handleStats(args);
        break;
      case "example_tz":
        handleExampleTZ();
        break;
      case "budgets":
        handleBudgets();
        break;
      case "transfer":
        handleTransfer(args);
        break;
      case "add_category":
        handleAddCategory(args);
        break;
      case "edit_category":
        handleEditCategory(args);
        break;
      case "export":
        handleExport(args);
        break;
      case "import":
        handleImport(args);
        break;
      case "operations":
        handleOperations(args);
        break;
      case "report":
        handleReport();
        break;
      case "clear":
        clearScreen();
        break;
      case "help":
        printHelp();
        break;
      case "exit":
        handleExit();
        break;
      default:
        System.out.println("❌ Неизвестная команда. Введите 'help' для списка команд.");
    }

    showNotifications();
  }

  // public для тестирования
  public void showNotifications() {
    List<String> notifications = financeManager.getAndClearNotifications();
    if (!notifications.isEmpty()) {
      System.out.println();
      for (String notification : notifications) {
        System.out.println(notification);
      }
    }
  }

  private void handleLogout() {
    financeManager.logout();
    System.out.println("✅ Вы вышли из системы");
  }

  private void handleBalance() {
    if (!financeManager.isAuthenticated()) {
      System.out.println("🔒 Требуется авторизация");
      return;
    }
    financeManager.showBalance();
  }

  private void handleBudgets() {
    if (!financeManager.isAuthenticated()) {
      System.out.println("🔒 Требуется авторизация");
      return;
    }
    financeManager.showBudgets();
  }

  private void handleReport() {
    if (!financeManager.isAuthenticated()) {
      System.out.println("🔒 Требуется авторизация");
      return;
    }
    financeManager.showDetailedReport();
  }

  // public для тестирования
  public void handleRegister(String args) {
    String[] parts = args.split("\\s+", 2);
    if (parts.length != 2) {
      System.out.println("📝 Использование: register <логин> <пароль>");
      System.out.println("   Пример: register alex 12345");
      return;
    }

    financeManager.register(parts[0], parts[1]);
  }

  // ИЗМЕНЕНО: public для тестирования
  public void handleLogin(String args) {
    String[] parts = args.split("\\s+", 2);
    if (parts.length != 2) {
      System.out.println("🔑 Использование: login <логин> <пароль>");
      System.out.println("   Пример: login alex 12345");
      return;
    }

    financeManager.login(parts[0], parts[1]);
  }

  // public для тестирования
  public void handleAddIncome(String args) {
    if (!financeManager.isAuthenticated()) {
      System.out.println("🔒 Требуется авторизация");
      return;
    }

    String[] parts = args.split("\\s+", 3);
    if (parts.length < 2) {
      System.out.println("📈 Использование: add_income <категория> <сумма> [описание]");
      System.out.println("   Пример: add_income Зарплата 50000 Октябрьская зарплата");
      return;
    }

    try {
      String category = parts[0];
      double amount = Double.parseDouble(parts[1]);
      String description = parts.length > 2 ? parts[2] : "";

      financeManager.addIncome(category, amount, description);
    } catch (NumberFormatException e) {
      System.out.println("❌ Неверный формат суммы");
    }
  }

  // public для тестирования
  public void handleAddExpense(String args) {
    if (!financeManager.isAuthenticated()) {
      System.out.println("🔒 Требуется авторизация");
      return;
    }

    String[] parts = args.split("\\s+", 3);
    if (parts.length < 2) {
      System.out.println("📉 Использование: add_expense <категория> <сумма> [описание]");
      System.out.println("   Пример: add_expense Еда 3000 Продукты");
      return;
    }

    try {
      String category = parts[0];
      double amount = Double.parseDouble(parts[1]);
      String description = parts.length > 2 ? parts[2] : "";

      financeManager.addExpense(category, amount, description);
    } catch (NumberFormatException e) {
      System.out.println("❌ Неверный формат суммы");
    }
  }

  private void handleSetBudget(String args) {
    if (!financeManager.isAuthenticated()) {
      System.out.println("🔒 Требуется авторизация");
      return;
    }

    String[] parts = args.split("\\s+", 2);
    if (parts.length != 2) {
      System.out.println("🎯 Использование: set_budget <категория> <лимит>");
      System.out.println("   Пример: set_budget Еда 10000");
      return;
    }

    try {
      String category = parts[0];
      double limit = Double.parseDouble(parts[1]);

      financeManager.setBudget(category, limit);
    } catch (NumberFormatException e) {
      System.out.println("❌ Неверный формат лимита");
    }
  }

  private void handleEditBudget(String args) {
    if (!financeManager.isAuthenticated()) {
      System.out.println("🔒 Требуется авторизация");
      return;
    }

    String[] parts = args.split("\\s+", 2);
    if (parts.length != 2) {
      System.out.println("✏️  Использование: edit_budget <категория> <новый_лимит>");
      System.out.println("   Пример: edit_budget Еда 15000");
      return;
    }

    try {
      String category = parts[0];
      double newLimit = Double.parseDouble(parts[1]);

      financeManager.editBudget(category, newLimit);
    } catch (NumberFormatException e) {
      System.out.println("❌ Неверный формат лимита");
    }
  }

  private void handleRemoveBudget(String args) {
    if (!financeManager.isAuthenticated()) {
      System.out.println("🔒 Требуется авторизация");
      return;
    }

    if (args.isEmpty()) {
      System.out.println("🗑️  Использование: remove_budget <категория>");
      System.out.println("   Пример: remove_budget Еда");
      return;
    }

    financeManager.removeBudget(args);
  }

  private void handleStats(String args) {
    if (!financeManager.isAuthenticated()) {
      System.out.println("🔒 Требуется авторизация");
      return;
    }

    List<String> categories = new ArrayList<>();
    LocalDate startDate = null;
    LocalDate endDate = null;

    if (!args.isEmpty()) {
      String[] parts = args.split("\\s+");
      for (String part : parts) {
        if (part.contains("-")) {
          String[] dateRange = part.split("-");
          if (dateRange.length == 2) {
            try {
              startDate = LocalDate.parse(dateRange[0], DATE_FORMATTER);
              endDate = LocalDate.parse(dateRange[1], DATE_FORMATTER);
            } catch (Exception e) {
              categories.add(part);
            }
          } else {
            categories.add(part);
          }
        } else {
          categories.add(part);
        }
      }
    }

    financeManager.showStatistics(categories, startDate, endDate);
  }

  private void handleExampleTZ() {
    if (!financeManager.isAuthenticated()) {
      System.out.println("🔒 Требуется авторизация");
      return;
    }

    System.out.println("\n=== ВЫПОЛНЕНИЕ ПРИМЕРА ИЗ ТЕХНИЧЕСКОГО ЗАДАНИЯ ===");
    System.out.println("Добавляем данные из примера ТЗ:");

    System.out.println("\n1. Добавляем доходы:");
    financeManager.addIncome("Зарплата", 20000, "");
    financeManager.addIncome("Зарплата", 40000, "");
    financeManager.addIncome("Бонус", 3000, "");

    System.out.println("\n2. Добавляем расходы:");
    financeManager.addExpense("Еда", 300, "");
    financeManager.addExpense("Еда", 500, "");
    financeManager.addExpense("Развлечения", 3000, "");
    financeManager.addExpense("Коммунальные услуги", 3000, "");
    financeManager.addExpense("Такси", 1500, "");

    System.out.println("\n3. Устанавливаем бюджеты:");
    financeManager.setBudget("Еда", 4000);
    financeManager.setBudget("Развлечения", 3000);
    financeManager.setBudget("Коммунальные услуги", 2500);

    System.out.println("\n=== РЕЗУЛЬТАТ (как в примере ТЗ) ===");
    financeManager.showExampleFromTZ();
  }

  private void handleTransfer(String args) {
    if (!financeManager.isAuthenticated()) {
      System.out.println("🔒 Требуется авторизация");
      return;
    }

    String[] parts = args.split("\\s+", 3);
    if (parts.length < 2) {
      System.out.println("💸 Использование: transfer <логин_получателя> <сумма> [описание]");
      System.out.println("   Пример: transfer maria 5000 За обед");
      return;
    }

    try {
      String toUser = parts[0];
      double amount = Double.parseDouble(parts[1]);
      String description = parts.length > 2 ? parts[2] : "";

      financeManager.transfer(toUser, amount, description);
    } catch (NumberFormatException e) {
      System.out.println("❌ Неверный формат суммы");
    }
  }

  private void handleAddCategory(String args) {
    if (!financeManager.isAuthenticated()) {
      System.out.println("🔒 Требуется авторизация");
      return;
    }

    String[] parts = args.split("\\s+", 2);
    if (parts.length < 1) {
      System.out.println("🏷️  Использование: add_category <название> [описание]");
      System.out.println("   Пример: add_category Образование Курсы и книги");
      return;
    }

    String name = parts[0];
    String description = parts.length > 1 ? parts[1] : "";

    financeManager.addCategory(name, description);
  }

  private void handleEditCategory(String args) {
    if (!financeManager.isAuthenticated()) {
      System.out.println("🔒 Требуется авторизация");
      return;
    }

    String[] parts = args.split("\\s+", 3);
    if (parts.length < 2) {
      System.out.println(
          "✏️  Использование: edit_category <старое_название> <новое_название> [описание]");
      System.out.println("   Пример: edit_category Еда Продукты");
      return;
    }

    String oldName = parts[0];
    String newName = parts[1];
    String description = parts.length > 2 ? parts[2] : "";

    financeManager.editCategory(oldName, newName, description);
  }

  private void handleExport(String args) {
    if (!financeManager.isAuthenticated()) {
      System.out.println("🔒 Требуется авторизация");
      return;
    }

    String[] parts = args.split("\\s+", 2);
    if (parts.length < 1) {
      System.out.println("📤 Использование: export <имя_файла> [формат]");
      System.out.println("   Примеры:");
      System.out.println("     export my_data           # Binary формат (.dat)");
      System.out.println("     export report csv        # CSV формат");
      System.out.println("     export data json         # JSON формат");
      return;
    }

    String filename = parts[0];
    String format = parts.length > 1 ? parts[1] : "binary";

    if (!format.equals("binary") && !format.equals("csv") && !format.equals("json")) {
      System.out.println("❌ Неподдерживаемый формат. Используйте: binary, csv или json");
      return;
    }

    financeManager.exportToFile(filename, format);
  }

  private void handleImport(String args) {
    if (!financeManager.isAuthenticated()) {
      System.out.println("🔒 Требуется авторизация");
      return;
    }

    String[] parts = args.split("\\s+", 2);
    if (parts.length < 1) {
      System.out.println("📥 Использование: import <имя_файла> [формат]");
      System.out.println("   Примеры:");
      System.out.println("     import my_data.dat        # Binary формат");
      System.out.println("     import data.json          # JSON формат");
      return;
    }

    String filename = parts[0];
    String format = parts.length > 1 ? parts[1] : "binary";

    if (!format.equals("binary") && !format.equals("json")) {
      System.out.println("❌ Неподдерживаемый формат. Используйте: binary или json");
      return;
    }

    System.out.print("⚠️  Текущие данные будут заменены. Продолжить? (да/нет): ");
    String confirmation = scanner.nextLine().trim().toLowerCase();

    if (confirmation.equals("да") || confirmation.equals("yes") || confirmation.equals("y")) {
      financeManager.importFromFile(filename, format);
    } else {
      System.out.println("❌ Импорт отменен");
    }
  }

  private void handleOperations(String args) {
    if (!financeManager.isAuthenticated()) {
      System.out.println("🔒 Требуется авторизация");
      return;
    }

    LocalDate startDate = null;
    LocalDate endDate = null;
    String category = null;

    if (!args.isEmpty()) {
      String[] parts = args.split("\\s+");
      for (String part : parts) {
        if (part.startsWith("дата:")) {
          String dateRange = part.substring(5);
          String[] dates = dateRange.split("-");
          if (dates.length == 2) {
            try {
              startDate = LocalDate.parse(dates[0], DATE_FORMATTER);
              endDate = LocalDate.parse(dates[1], DATE_FORMATTER);
            } catch (Exception e) {
              System.out.println("❌ Неверный формат даты. Используйте дата:ДД.ММ.ГГГГ-ДД.ММ.ГГГГ");
              return;
            }
          }
        } else if (part.startsWith("категория:")) {
          category = part.substring(10);
        }
      }
    }

    financeManager.showOperations(startDate, endDate, category);
  }

  private void handleExit() {
    System.out.print("\n💾 Сохранить данные перед выходом? (да/нет): ");
    String answer = scanner.nextLine().trim().toLowerCase();

    if (answer.equals("да") || answer.equals("yes") || answer.equals("y")) {
      financeManager.logout();
    }

    System.out.println("\n👋 Спасибо за использование Finance Manager!");
    running = false;
  }

  private void clearScreen() {
    for (int i = 0; i < 50; i++) {
      System.out.println();
    }
  }

  private void printWelcome() {
    System.out.println("══════════════════════════════════════════════");
    System.out.println("      🏦 FINANCE MANAGER v1.0");
    System.out.println("   Управление личными финансами");
    System.out.println("══════════════════════════════════════════════");
  }

  private void printHelp() {
    System.out.println("\n══════════════════════════════════════════════");
    System.out.println("              СПРАВКА ПО КОМАНДАМ");
    System.out.println("══════════════════════════════════════════════");

    System.out.println("\n👤 АУТЕНТИФИКАЦИЯ:");
    System.out.println("  register <логин> <пароль>  - Регистрация нового пользователя");
    System.out.println("  login <логин> <пароль>     - Вход в систему");
    System.out.println("  logout                     - Выход из системы");

    System.out.println("\n💰 ОПЕРАЦИИ:");
    System.out.println("  add_income <кат> <сум> [оп] - Добавить доход");
    System.out.println("  add_expense <кат> <сум> [оп]- Добавить расход");
    System.out.println("  transfer <получ> <сум> [оп] - Перевод другому пользователю");

    System.out.println("\n🏷️  КАТЕГОРИИ:");
    System.out.println("  add_category <имя> [оп]     - Добавить категорию");
    System.out.println("  edit_category <ст> <нов> [оп] - Изменить категорию");

    System.out.println("\n🎯 БЮДЖЕТЫ:");
    System.out.println("  set_budget <кат> <лимит>    - Установить бюджет");
    System.out.println("  edit_budget <кат> <лимит>   - Изменить бюджет");
    System.out.println("  remove_budget <кат>         - Удалить бюджет");

    System.out.println("\n📊 ОТЧЕТЫ И СТАТИСТИКА:");
    System.out.println("  balance                    - Показать баланс");
    System.out.println("  stats [кат1 кат2...]       - Статистика (по категориям)");
    System.out.println(
        "  example_tz                 - Выполнить пример из ТЗ и показать результат");
    System.out.println("  stats дата:ДД.ММ.ГГГГ-ДД.ММ.ГГГГ - Статистика за период");
    System.out.println("  budgets                    - Показать все бюджеты");
    System.out.println("  operations                 - Показать все операции");
    System.out.println("  operations дата:ДД.ММ.ГГГГ-ДД.ММ.ГГГГ - Операции за период");
    System.out.println("  operations категория:Еда   - Операции по категории");
    System.out.println("  report                     - Детальный отчет");

    System.out.println("\n💾 ИМПОРТ/ЭКСПОРТ (3 формата):");
    System.out.println("  export <имя> [формат]      - Экспорт данных (binary/csv/json)");
    System.out.println("  import <имя_файла> [формат]- Импорт данных (binary/json)");
    System.out.println("  Примеры:");
    System.out.println("    export data binary       # Binary формат (.dat)");
    System.out.println("    export report csv        # CSV формат");
    System.out.println("    export backup json       # JSON формат");
    System.out.println("    import data.dat binary   # Импорт Binary");
    System.out.println("    import backup.json json  # Импорт JSON");

    System.out.println("\n⚙️  СИСТЕМНЫЕ:");
    System.out.println("  clear                      - Очистить экран");
    System.out.println("  help                       - Показать эту справку");
    System.out.println("  exit                       - Выход из приложения");

    System.out.println("\n📝 ПРИМЕР ИЗ ТЗ:");
    System.out.println(
        "  example_tz                 - Автоматически добавит данные из ТЗ и покажет результат");
    System.out.println("  (ТЗ пример: доходы 20000+40000+3000, расходы 300+500+3000+3000+1500)");
    System.out.println("  (Бюджеты: Еда 4000, Развлечения 3000, Коммунальные услуги 2500)");

    System.out.println("\n⚠️  Автоматические оповещения:");
    System.out.println("  • Превышение бюджета");
    System.out.println("  • 80% использования бюджета");
    System.out.println("  • Отрицательный баланс");
    System.out.println("  • Высокий уровень расходов");

    System.out.println("══════════════════════════════════════════════");
  }
}
