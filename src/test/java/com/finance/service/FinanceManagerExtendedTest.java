package com.finance.service;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.finance.core.*;
import com.finance.exception.FinanceException;

class FinanceManagerExtendedTest {

  @Test
  void testEditBudget_NonExistentCategoryThrowsException() {
    // Тест FinanceManager.editBudget с несуществующей категорией
    FinanceManager fm = new FinanceManager();
    String user = "editbudget_" + System.currentTimeMillis();
    fm.register(user, "pass");
    fm.login(user, "pass");

    // Не устанавливаем бюджет предварительно
    assertThrows(FinanceException.class, () -> fm.editBudget("НесуществующаяКатегория", 10000));
  }

  @Test
  void testRemoveBudget_NonExistentThrowsException() {
    // Тест FinanceManager.removeBudget с несуществующим бюджетом
    FinanceManager fm = new FinanceManager();
    String user = "rmbudget_" + System.currentTimeMillis();
    fm.register(user, "pass");
    fm.login(user, "pass");

    assertThrows(FinanceException.class, () -> fm.removeBudget("НесуществующаяКатегория"));
  }

  @Test
  void testImportFromFile_FileNotFoundThrowsException() {
    // Тест FinanceManager.importFromFile с несуществующим файлом
    FinanceManager fm = new FinanceManager();
    String user = "import_" + System.currentTimeMillis();
    fm.register(user, "pass");
    fm.login(user, "pass");

    // Создаем несуществующее имя файла
    String nonExistentFile = "несуществующий_файл_" + System.currentTimeMillis() + ".json";
    assertThrows(FinanceException.class, () -> fm.importFromFile(nonExistentFile, "json"));
  }

  @Test
  void testTransfer_ToSelfThrowsException() {
    // Тест: перевод самому себе должен вызывать исключение
    FinanceManager fm = new FinanceManager();
    String user = "selfxfer_" + System.currentTimeMillis();
    fm.register(user, "pass");
    fm.login(user, "pass");
    fm.addIncome("Зарплата", 10000, "");

    assertThrows(FinanceException.class, () -> fm.transfer(user, 1000, "Самому себе"));
  }

  @Test
  void testAddCategory_DuplicateThrowsException() {
    // Тест: добавление дублирующей категории
    FinanceManager fm = new FinanceManager();
    String user = "dupcat_" + System.currentTimeMillis();
    fm.register(user, "pass");
    fm.login(user, "pass");

    fm.addCategory("УникальнаяКатегория", "");

    assertThrows(
        FinanceException.class,
        () -> fm.addCategory("УникальнаяКатегория", "Попытка добавить снова"));
  }

  @Test
  void testEditCategory_NonExistentThrowsException() {
    // Тест: редактирование несуществующей категории
    FinanceManager fm = new FinanceManager();
    String user = "editcat_" + System.currentTimeMillis();
    fm.register(user, "pass");
    fm.login(user, "pass");

    assertThrows(FinanceException.class, () -> fm.editCategory("Несуществующая", "Новая", ""));
  }

  @Test
  void testShowStatistics_WithSpecificCategories() {
    // Тест: статистика по конкретным категориям (ветка !categoryNames.isEmpty())
    FinanceManager fm = new FinanceManager();
    String user = "stats_" + System.currentTimeMillis();
    fm.register(user, "pass");
    fm.login(user, "pass");

    fm.addIncome("Зарплата", 50000, "");
    fm.addExpense("Еда", 3000, "");
    fm.addExpense("Транспорт", 2000, "");

    // Не проверяем вывод, просто убеждаемся, что метод не падает
    assertDoesNotThrow(
        () -> fm.showStatistics(List.of("Еда", "Транспорт", "Несуществующая"), null, null));
  }

  @Test
  void testShowOperations_WithFilters() {
    // Тест: операции с фильтрами по дате и категории
    FinanceManager fm = new FinanceManager();
    String user = "ops_" + System.currentTimeMillis();
    fm.register(user, "pass");
    fm.login(user, "pass");

    fm.addIncome("Зарплата", 50000, "");
    fm.addExpense("Еда", 3000, "");

    LocalDate startDate = fm.parseDate("01.01.2023");
    LocalDate endDate = fm.parseDate("31.12.2024");

    assertDoesNotThrow(() -> fm.showOperations(startDate, endDate, "Еда"));

    // С несуществующей категорией
    assertDoesNotThrow(() -> fm.showOperations(null, null, "Несуществующая"));
  }

  @Test
  void testParseDate_InvalidFormat() {
    // Тест: парсинг неверного формата даты
    FinanceManager fm = new FinanceManager();
    String user = "date_" + System.currentTimeMillis();
    fm.register(user, "pass");
    fm.login(user, "pass");

    assertThrows(FinanceException.class, () -> fm.parseDate("2023-10-01")); // Неправильный формат

    assertThrows(FinanceException.class, () -> fm.parseDate("не дата")); // Совсем не дата
  }

  @Test
  void testExportToFile_AllFormats() {
    // Тест: экспорт во все поддерживаемые форматы
    FinanceManager fm = new FinanceManager();
    String user = "export_" + System.currentTimeMillis();
    fm.register(user, "pass");
    fm.login(user, "pass");

    fm.addIncome("Зарплата", 50000, "");
    fm.addExpense("Еда", 3000, "");

    // Создаем директорию для экспорта
    new File("exports").mkdirs();

    // Binary формат
    assertDoesNotThrow(() -> fm.exportToFile("test_binary", "binary"));

    // CSV формат
    assertDoesNotThrow(() -> fm.exportToFile("test_csv", "csv"));

    // JSON формат
    assertDoesNotThrow(() -> fm.exportToFile("test_json", "json"));

    // Очищаем тестовые файлы
    new File("exports/test_binary.dat").delete();
    new File("exports/test_csv.csv").delete();
    new File("exports/test_json.json").delete();
  }

  @Test
  void testGetNotifications() {
    // Тест: получение уведомлений
    FinanceManager fm = new FinanceManager();
    String user = "notif_" + System.currentTimeMillis();
    fm.register(user, "pass");
    fm.login(user, "pass");

    // Создаем ситуацию для уведомления (превышение бюджета)
    fm.setBudget("Еда", 1000);
    fm.addIncome("Зарплата", 5000, "");
    fm.addExpense("Еда", 1200, "Превышение бюджета");

    List<String> notifications = fm.getAndClearNotifications();
    assertNotNull(notifications);
    assertFalse(notifications.isEmpty());
  }

  @Test
  void testIsAuthenticated() {
    // Тест: проверка статуса авторизации
    FinanceManager fm = new FinanceManager();
    String user = "auth_" + System.currentTimeMillis();

    // До авторизации
    assertFalse(fm.isAuthenticated());

    // После авторизации
    fm.register(user, "pass");
    fm.login(user, "pass");
    assertTrue(fm.isAuthenticated());

    // После выхода
    fm.logout();
    assertFalse(fm.isAuthenticated());
  }

  @Test
  void testGetCurrentUser() {
    // Тест: получение текущего пользователя
    FinanceManager fm = new FinanceManager();
    String user = "current_" + System.currentTimeMillis();

    // До авторизации
    assertNull(fm.getCurrentUser());

    // После авторизации
    fm.register(user, "pass");
    fm.login(user, "pass");
    assertNotNull(fm.getCurrentUser());
    assertEquals(user, fm.getCurrentUser().getLogin());
  }

  @Test
  void testShowExampleFromTZ() {
    // Тест: выполнение примера из ТЗ
    FinanceManager fm = new FinanceManager();
    String user = "tz_" + System.currentTimeMillis();
    fm.register(user, "pass");
    fm.login(user, "pass");

    assertDoesNotThrow(() -> fm.showExampleFromTZ());
  }

  @Test
  void testShowBudgets() {
    // Тест: отображение бюджетов
    FinanceManager fm = new FinanceManager();
    String user = "budgets_" + System.currentTimeMillis();
    fm.register(user, "pass");
    fm.login(user, "pass");

    fm.setBudget("Еда", 10000);
    fm.setBudget("Транспорт", 5000);

    assertDoesNotThrow(() -> fm.showBudgets());
  }

  @Test
  void testShowDetailedReport() {
    // Тест: детальный отчет
    FinanceManager fm = new FinanceManager();
    String user = "report_" + System.currentTimeMillis();
    fm.register(user, "pass");
    fm.login(user, "pass");

    fm.addIncome("Зарплата", 50000, "");
    fm.addExpense("Еда", 3000, "");
    fm.setBudget("Еда", 10000);

    assertDoesNotThrow(() -> fm.showDetailedReport());
  }
}
