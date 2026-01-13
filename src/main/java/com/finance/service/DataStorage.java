package com.finance.service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.finance.core.*;

public class DataStorage {
  private static final String DATA_FILE = "users_data.dat";
  private static final String EXPORT_DIR = "exports/";
  private final ObjectMapper objectMapper;

  public DataStorage() {
    new File(EXPORT_DIR).mkdirs();
    this.objectMapper = new ObjectMapper();
    this.objectMapper.registerModule(new JavaTimeModule());
    this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
  }

  public void saveUsers(Map<String, User> users) {
    try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
      oos.writeObject(users);
    } catch (Exception e) {
      System.out.println("Ошибка при сохранении данных: " + e.getMessage());
    }
  }

  @SuppressWarnings("unchecked")
  public Map<String, User> loadUsers() {
    try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(DATA_FILE))) {
      Map<String, User> users = (Map<String, User>) ois.readObject();

      // 🔧 Инициализируем notifications для каждого кошелька
      if (users != null) {
        users
            .values()
            .forEach(
                user -> {
                  Wallet wallet = user.getWallet();
                  if (wallet != null) {
                    wallet.initializeNotifications();
                  }
                });
      }

      return users;
    } catch (FileNotFoundException e) {
      return new HashMap<>();
    } catch (Exception e) {
      System.out.println("Ошибка при загрузке данных: " + e.getMessage());
      return new HashMap<>();
    }
  }

  public void exportWallet(Wallet wallet, String filename) {
    try {
      String fullPath = prepareFilePath(filename, ".dat");
      try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fullPath))) {
        oos.writeObject(wallet);
      }
      System.out.println("Данные экспортированы в бинарный файл: " + fullPath);
    } catch (Exception e) {
      throw new RuntimeException("Ошибка при экспорте: " + e.getMessage(), e);
    }
  }

  public Wallet importWallet(String filename) {
    try {
      String fullPath = findFile(filename, ".dat");
      try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fullPath))) {
        Wallet wallet = (Wallet) ois.readObject();

        // 🔧 Инициализируем notifications после десериализации
        if (wallet != null) {
          wallet.initializeNotifications();
        }

        return wallet;
      }
    } catch (Exception e) {
      throw new RuntimeException("Ошибка при импорте: " + e.getMessage(), e);
    }
  }

  public void exportToCSV(Wallet wallet, String filename) {
    try {
      String fullPath = prepareFilePath(filename, ".csv");
      StringBuilder csv = new StringBuilder();
      csv.append("Тип,Категория,Сумма,Дата,Описание\n");
      for (Operation op : wallet.getOperations()) {
        String type = op instanceof Income ? "Доход" : "Расход";
        csv.append(
            String.format(
                "%s,%s,%.2f,%s,%s\n",
                type,
                op.getCategory().getName(),
                op.getAmount(),
                op.getDateTime(),
                op.getDescription()));
      }
      Files.write(Paths.get(fullPath), csv.toString().getBytes());
      System.out.println("Данные экспортированы в CSV: " + fullPath);
    } catch (Exception e) {
      throw new RuntimeException("Ошибка при экспорте в CSV: " + e.getMessage(), e);
    }
  }

  public void exportBudgetsToCSV(Wallet wallet, String filename) {
    try {
      String fullPath = prepareFilePath(filename + "_budgets", ".csv");
      StringBuilder csv = new StringBuilder();
      csv.append("Категория,Лимит,Потрачено,Осталось,Процент использования\n");
      for (Budget budget : wallet.getBudgets().values()) {
        csv.append(
            String.format(
                "%s,%.2f,%.2f,%.2f,%.1f%%\n",
                budget.getCategory().getName(),
                budget.getLimit(),
                budget.getSpent(),
                budget.getRemaining(),
                budget.getUsagePercentage()));
      }
      Files.write(Paths.get(fullPath), csv.toString().getBytes());
      System.out.println("Бюджеты экспортированы в CSV: " + fullPath);
    } catch (Exception e) {
      throw new RuntimeException("Ошибка при экспорте бюджетов: " + e.getMessage(), e);
    }
  }

  public void exportToJSON(Wallet wallet, String filename) {
    try {
      String fullPath = prepareFilePath(filename, ".json");
      Map<String, Object> jsonData = new HashMap<>();
      jsonData.put("balance", wallet.getBalance());
      jsonData.put("totalIncome", wallet.getTotalIncome());
      jsonData.put("totalExpense", wallet.getTotalExpense());
      List<Map<String, Object>> operationsList = new ArrayList<>();
      for (Operation op : wallet.getOperations()) {
        Map<String, Object> opMap = new HashMap<>();
        opMap.put("type", op instanceof Income ? "INCOME" : "EXPENSE");
        opMap.put("category", op.getCategory().getName());
        opMap.put("amount", op.getAmount());
        opMap.put("dateTime", op.getDateTime().toString());
        opMap.put("description", op.getDescription());
        operationsList.add(opMap);
      }
      jsonData.put("operations", operationsList);
      List<Map<String, Object>> categoriesList = new ArrayList<>();
      for (Category cat : wallet.getCategories()) {
        Map<String, Object> catMap = new HashMap<>();
        catMap.put("name", cat.getName());
        catMap.put("description", cat.getDescription());
        categoriesList.add(catMap);
      }
      jsonData.put("categories", categoriesList);
      List<Map<String, Object>> budgetsList = new ArrayList<>();
      for (Budget budget : wallet.getBudgets().values()) {
        Map<String, Object> budgetMap = new HashMap<>();
        budgetMap.put("category", budget.getCategory().getName());
        budgetMap.put("limit", budget.getLimit());
        budgetMap.put("spent", budget.getSpent());
        budgetMap.put("remaining", budget.getRemaining());
        budgetMap.put("usagePercentage", budget.getUsagePercentage());
        budgetMap.put("exceeded", budget.isExceeded());
        budgetsList.add(budgetMap);
      }
      jsonData.put("budgets", budgetsList);
      objectMapper.writeValue(new File(fullPath), jsonData);
      System.out.println("Данные экспортированы в JSON: " + fullPath);
    } catch (Exception e) {
      throw new RuntimeException("Ошибка при экспорте в JSON: " + e.getMessage(), e);
    }
  }

  public Wallet importFromJSON(String filename) {
    try {
      String fullPath = findFile(filename, ".json");
      Map<String, Object> jsonData = objectMapper.readValue(new File(fullPath), Map.class);
      Wallet wallet = new Wallet();

      // 🔧 Инициализируем notifications
      wallet.initializeNotifications();

      @SuppressWarnings("unchecked")
      List<Map<String, Object>> categoriesList =
          (List<Map<String, Object>>) jsonData.get("categories");
      if (categoriesList != null) {
        for (Map<String, Object> catMap : categoriesList) {
          String name = (String) catMap.get("name");
          String description = (String) catMap.get("description");
          if (name != null && !name.trim().isEmpty()) {
            wallet.addCategory(new Category(name, description != null ? description : ""));
          }
        }
      }
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> operationsList =
          (List<Map<String, Object>>) jsonData.get("operations");
      if (operationsList != null) {
        List<Operation> importedOperations = new ArrayList<>();
        double calculatedBalance = 0.0;
        for (Map<String, Object> opMap : operationsList) {
          String type = (String) opMap.get("type");
          String categoryName = (String) opMap.get("category");
          double amount = ((Number) opMap.get("amount")).doubleValue();
          String description = (String) opMap.get("description");
          String dateTimeStr = (String) opMap.get("dateTime");
          Category category = wallet.getCategory(categoryName);
          if (category == null) {
            category = new Category(categoryName, "");
            wallet.addCategory(category);
          }
          LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr);
          Operation operation;
          if ("INCOME".equals(type)) {
            operation = new Income(amount, category, description, dateTime);
            calculatedBalance += amount;
          } else {
            operation = new Expense(amount, category, description, dateTime);
            calculatedBalance -= amount;
          }
          importedOperations.add(operation);
        }
        wallet.setOperations(importedOperations);
        wallet.setBalance(calculatedBalance);
      }
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> budgetsList = (List<Map<String, Object>>) jsonData.get("budgets");
      if (budgetsList != null) {
        Map<String, Budget> importedBudgets = new HashMap<>();
        for (Map<String, Object> budgetMap : budgetsList) {
          String categoryName = (String) budgetMap.get("category");
          double limit = ((Number) budgetMap.get("limit")).doubleValue();
          double spent = ((Number) budgetMap.get("spent")).doubleValue();
          if (!wallet.hasCategory(categoryName)) {
            wallet.addCategory(new Category(categoryName, ""));
          }
          Category category = wallet.getCategory(categoryName);
          if (category != null) {
            Budget budget = new Budget(category, limit, spent);
            importedBudgets.put(categoryName.toLowerCase(), budget);
          }
        }
        wallet.setBudgets(importedBudgets);
      }
      return wallet;
    } catch (Exception e) {
      throw new RuntimeException("Ошибка при импорте из JSON: " + e.getMessage(), e);
    }
  }

  private String prepareFilePath(String filename, String extension) {
    String fullPath = filename;
    if (!filename.endsWith(extension)) {
      fullPath = filename + extension;
    }
    File file = new File(fullPath);
    File parent = file.getParentFile();
    if (parent != null) {
      parent.mkdirs();
      return fullPath;
    } else {
      return EXPORT_DIR + fullPath;
    }
  }

  private String findFile(String filename, String extension) throws FileNotFoundException {
    File file = new File(filename);
    if (file.exists()) {
      return filename;
    }
    String withExt = filename + extension;
    file = new File(withExt);
    if (file.exists()) {
      return withExt;
    }
    String exportPath = EXPORT_DIR + withExt;
    file = new File(exportPath);
    if (file.exists()) {
      return exportPath;
    }
    throw new FileNotFoundException("Файл не найден: " + filename);
  }
}
