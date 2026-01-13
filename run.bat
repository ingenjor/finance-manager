@echo off
chcp 65001 >nul
echo.
echo ============================================
echo    Finance Manager - Запуск приложения
echo ============================================
echo.

REM Проверяем наличие Java
java -version >nul 2>&1
if errorlevel 1 (
    echo ❌ ОШИБКА: Java не установлена или не найдена в PATH
    echo Установите Java 17 или выше
    pause
    exit /b 1
)

REM Проверяем наличие Maven
mvn -version >nul 2>&1
if errorlevel 1 (
    echo ❌ ОШИБКА: Maven не установлен или не найден в PATH
    echo Установите Maven 3.8.0 или выше
    pause
    exit /b 1
)

REM Проверяем, собран ли проект
if not exist "target\finance-manager-1.0-SNAPSHOT-jar-with-dependencies.jar" (
    echo 📦 JAR файл не найден. Собираем проект...
    echo.
    call mvn clean package -q -DskipTests
    if errorlevel 1 (
        echo ❌ Ошибка при сборке проекта!
        echo Проверьте настройки Maven и зависимости.
        pause
        exit /b 1
    )
    echo ✅ Проект успешно собран!
    echo.
)

echo 🚀 Запуск приложения...
echo Для выхода введите 'exit'
echo Для справки введите 'help'
echo ============================================
echo.
java -Dfile.encoding=UTF-8 -jar "target\finance-manager-1.0-SNAPSHOT-jar-with-dependencies.jar"

pause
