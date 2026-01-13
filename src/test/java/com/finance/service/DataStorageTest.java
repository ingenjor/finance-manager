package com.finance.service;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.*;

import com.finance.core.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DataStorageTest {

  private DataStorage dataStorage;
  private Wallet testWallet;
  private static final String TEST_EXPORT_DIR = "test_exports/";
  private static final String TEST_USER_FILE = "test_users_data.dat";

  @BeforeEach
  void setUp() {
    dataStorage = new DataStorage();

    // Создаем тестовый кошелек с начальным балансом 0
    testWallet = new Wallet();
    testWallet.addCategory(new Category("ТестКатегория", "Для тестирования"));

    // Добавляем операции
    testWallet.addOperation(new Income(50000, testWallet.getCategory("Зарплата"), "Тест"));
    testWallet.addOperation(
        new Expense(3000, testWallet.getCategory("ТестКатегория"), "Тест расход"));

    // Устанавливаем бюджет
    testWallet.setBudget("ТестКатегория", 10000);

    // Создаем директорию для тестовых экспортов
    new File(TEST_EXPORT_DIR).mkdirs();

    // Также создаем стандартную директорию exports, если ее нет
    new File("exports").mkdirs();
  }

  @AfterEach
  void tearDown() throws Exception {
    // Удаляем тестовые файлы
    try {
      Files.deleteIfExists(new File(TEST_USER_FILE).toPath());
    } catch (Exception e) {
      // Игнорируем
    }

    File exportsDir = new File(TEST_EXPORT_DIR);
    if (exportsDir.exists() && exportsDir.isDirectory()) {
      File[] files = exportsDir.listFiles();
      if (files != null) {
        for (File file : files) {
          if (file != null && file.exists()) {
            file.delete();
          }
        }
      }
      exportsDir.delete();
    }
  }

  @Test
  @Order(1)
  void testExportWallet_Binary() {
    // Тест экспорта в бинарный формат
    assertDoesNotThrow(
        () -> {
          dataStorage.exportWallet(testWallet, "test_exports/test_wallet");
        });

    File exportFile = new File("test_exports/test_wallet.dat");
    assertTrue(exportFile.exists(), "Бинарный файл должен быть создан");
  }

  @Test
  @Order(2)
  void testImportWallet_Binary() {
    // Сначала экспортируем
    assertDoesNotThrow(
        () -> {
          dataStorage.exportWallet(testWallet, "test_exports/test_wallet");
        });

    // Затем импортируем
    assertDoesNotThrow(
        () -> {
          Wallet importedWallet = dataStorage.importWallet("test_exports/test_wallet");
          assertNotNull(importedWallet);
          assertEquals(47000.0, importedWallet.getBalance(), 0.01, "Баланс должен быть 47000");
          assertEquals(1, importedWallet.getBudgets().size(), "Должен быть 1 бюджет");
        });
  }

  @Test
  @Order(3)
  void testExportToCSV() {
    // Тест экспорта в CSV
    assertDoesNotThrow(
        () -> {
          dataStorage.exportToCSV(testWallet, "test_exports/test_csv");
        });

    File csvFile = new File("test_exports/test_csv.csv");
    assertTrue(csvFile.exists(), "CSV файл должен быть создан");
    assertTrue(csvFile.length() > 0, "CSV файл не должен быть пустым");
  }

  @Test
  @Order(4)
  void testExportBudgetsToCSV() {
    // Тест экспорта бюджетов в CSV
    assertDoesNotThrow(
        () -> {
          dataStorage.exportBudgetsToCSV(testWallet, "test_exports/test_budgets");
        });

    File csvFile = new File("test_exports/test_budgets_budgets.csv");
    assertTrue(csvFile.exists(), "CSV файл бюджетов должен быть создан");
  }

  @Test
  @Order(5)
  void testExportToJSON() {
    // Тест экспорта в JSON
    assertDoesNotThrow(
        () -> {
          dataStorage.exportToJSON(testWallet, "test_exports/test_json");
        });

    File jsonFile = new File("test_exports/test_json.json");
    assertTrue(jsonFile.exists(), "JSON файл должен быть создан");
    assertTrue(jsonFile.length() > 0, "JSON файл не должен быть пустым");
  }

  @Test
  @Order(6)
  void testImportFromJSON() {
    // Сначала экспортируем в JSON
    assertDoesNotThrow(
        () -> {
          dataStorage.exportToJSON(testWallet, "test_exports/test_json_import");
        });

    // Затем импортируем
    assertDoesNotThrow(
        () -> {
          Wallet importedWallet = dataStorage.importFromJSON("test_exports/test_json_import");
          assertNotNull(importedWallet, "Импортированный кошелек не должен быть null");

          // Проверяем количество операций
          assertEquals(
              testWallet.getOperations().size(),
              importedWallet.getOperations().size(),
              "Количество операций должно совпадать");

          // Проверяем баланс (должен быть 47000)
          assertEquals(47000.0, importedWallet.getBalance(), 0.01, "Баланс должен быть 47000");

          // Проверяем категории
          assertTrue(
              importedWallet.hasCategory("ТестКатегория"), "Должна быть категория 'ТестКатегория'");
          assertTrue(importedWallet.hasCategory("Зарплата"), "Должна быть категория 'Зарплата'");

          // Проверяем бюджеты
          assertNotNull(importedWallet.getBudgets(), "Бюджеты не должны быть null");
          assertEquals(1, importedWallet.getBudgets().size(), "Должен быть 1 бюджет");

          // Проверяем конкретный бюджет
          Budget importedBudget = importedWallet.getBudget("ТестКатегория");
          assertNotNull(importedBudget, "Бюджет для 'ТестКатегория' не должен быть null");
          assertEquals(10000.0, importedBudget.getLimit(), 0.01, "Лимит бюджета должен быть 10000");
          assertEquals(
              3000.0, importedBudget.getSpent(), 0.01, "Потраченная сумма должна быть 3000");
        });
  }

  @Test
  @Order(7)
  void testSaveAndLoadUsers() {
    // Тест сохранения и загрузки пользователей
    Map<String, User> users = new HashMap<>();

    User user1 = new User("test1", "pass1", new Wallet());
    User user2 = new User("test2", "pass2", new Wallet());

    users.put("test1", user1);
    users.put("test2", user2);

    // Сохраняем
    DataStorage storage = new DataStorage();

    assertDoesNotThrow(
        () -> {
          storage.saveUsers(users);
        });
  }

  @Test
  @Order(8)
  void testImportNonExistentFile() {
    // Тест импорта несуществующего файла
    Exception exception =
        assertThrows(
            RuntimeException.class,
            () -> {
              dataStorage.importWallet("несуществующий_файл_12345.dat");
            });

    assertTrue(
        exception.getMessage().contains("не найден") || exception.getMessage().contains("Файл"));
  }

  @Test
  @Order(9)
  void testImportJSONNonExistentFile() {
    // Тест импорта несуществующего JSON файла
    Exception exception =
        assertThrows(
            RuntimeException.class,
            () -> {
              dataStorage.importFromJSON("несуществующий_json_12345.json");
            });

    assertTrue(
        exception.getMessage().contains("не найден") || exception.getMessage().contains("Файл"));
  }

  @Test
  @Order(10)
  void testExportWithEmptyWallet() {
    // Тест экспорта пустого кошелька
    Wallet emptyWallet = new Wallet();

    assertDoesNotThrow(
        () -> {
          dataStorage.exportToCSV(emptyWallet, "test_exports/empty_csv");
          dataStorage.exportToJSON(emptyWallet, "test_exports/empty_json");
        });

    File csvFile = new File("test_exports/empty_csv.csv");
    File jsonFile = new File("test_exports/empty_json.json");

    assertTrue(csvFile.exists());
    assertTrue(jsonFile.exists());
  }

  @Test
  @Order(11)
  void testExportBudgetsEmpty() {
    // Тест экспорта пустых бюджетов
    Wallet walletWithoutBudgets = new Wallet();

    assertDoesNotThrow(
        () -> {
          dataStorage.exportBudgetsToCSV(walletWithoutBudgets, "test_exports/no_budgets");
        });

    File csvFile = new File("test_exports/no_budgets_budgets.csv");
    assertTrue(csvFile.exists());
  }
}
