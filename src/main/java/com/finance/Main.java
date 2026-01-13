package com.finance;

import com.finance.cli.CLIInterface;
import com.finance.service.FinanceManager;

public class Main {
  public static void main(String[] args) {
    // Устанавливаем кодировку для JVM
    System.setProperty("file.encoding", "UTF-8");

    // Создаем и запускаем приложение
    FinanceManager financeManager = new FinanceManager();
    CLIInterface cli = new CLIInterface(financeManager);
    cli.start();
  }
}
