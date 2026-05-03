# otpService

Backend-сервис для защиты операций одноразовыми кодами (OTP).

## Стек

- Java 17, Maven
- PostgreSQL 17, JDBC (без ORM)
- HTTP сервер: `com.sun.net.httpserver`
- JWT (JJWT 0.12), BCrypt, Logback
- Email: Angus Mail (SMTP / Mailpit)
- SMS: jsmpp + встроенный SMPP-эмулятор
- Telegram Bot API

## Быстрый старт

### 1. База данных

```sql
CREATE DATABASE otpdb;
```

```powershell
psql -U postgres -d otpdb -f src\main\resources\db\schema.sql
```

### 2. Конфигурация

Заполнить своими значениями файлы в `src/main/resources/`:

| Файл | Параметры |
|---|---|
| `application.properties` | Порт сервера, подключение к БД, JWT-секрет |
| `email.properties` | SMTP host/port/логин |
| `sms.properties` | SMPP host/port/логин (для эмулятора: localhost:2775) |
| `telegram.properties` | Bot token + chat_id |

### 3. Сборка

```powershell
mvn package
```

### 4. Запуск эмуляторов (опционально)

**Email — Mailpit:**
```powershell
mailpit
# Веб-интерфейс: http://localhost:8025
```

**SMS — встроенный SMPP-эмулятор:**
```powershell
java -cp target\otp-java-service.jar com.otpservice.tools.SmppSimulator
# Слушает порт 2775, выводит входящие SMS в консоль
```

### 5. Запуск сервиса

```powershell
java -jar target\otp-java-service.jar
```

### 6. Docker (альтернатива)

Создать файл `.env` в корне проекта (по образцу `.env.example`):

```
POSTGRES_PASSWORD=changeme
JWT_SECRET=your-secret-key-minimum-32-characters!!
```

```powershell
docker-compose up --build
```

---

## API

### Аутентификация (без токена)

| Метод | Путь | Тело |
|---|---|---|
| POST | `/api/auth/register` | `{"login":"alice","password":"secret","role":"USER"}` |
| POST | `/api/auth/login` | `{"login":"alice","password":"secret"}` |

> Роли: `USER` или `ADMIN`. Второй `ADMIN` не допускается.

Ответ логина: `{"token":"eyJ..."}` — использовать в заголовке `Authorization: Bearer <token>`.

### Администратор

| Метод | Путь | Тело |
|---|---|---|
| GET | `/api/admin/users` | — |
| DELETE | `/api/admin/users/{id}` | — |
| PUT | `/api/admin/config` | `{"codeLength":6,"ttlSeconds":300}` |

### Пользователь

| Метод | Путь | Тело |
|---|---|---|
| POST | `/api/otp/generate` | `{"operationId":"op-1","channel":"EMAIL","destination":"user@example.com"}` |
| POST | `/api/otp/validate` | `{"operationId":"op-1","code":"483921"}` |

Доступные каналы: `EMAIL`, `SMS`, `TELEGRAM`, `FILE`

---

## Тестирование (PowerShell)

```powershell
# Регистрация администратора
Invoke-RestMethod -Method POST -Uri "http://localhost:8080/api/auth/register" `
  -ContentType "application/json" `
  -Body '{"login":"admin","password":"admin123","role":"ADMIN"}'

# Регистрация пользователя
Invoke-RestMethod -Method POST -Uri "http://localhost:8080/api/auth/register" `
  -ContentType "application/json" `
  -Body '{"login":"user1","password":"user123","role":"USER"}'

# Логин и сохранение токена
$token = (Invoke-RestMethod -Method POST -Uri "http://localhost:8080/api/auth/login" `
  -ContentType "application/json" `
  -Body '{"login":"user1","password":"user123"}').token

# Генерация OTP в файл
Invoke-RestMethod -Method POST -Uri "http://localhost:8080/api/otp/generate" `
  -ContentType "application/json" `
  -Headers @{Authorization="Bearer $token"} `
  -Body '{"operationId":"op-1","channel":"FILE","destination":"test"}'
# Код появится в otp-codes.txt

# Генерация OTP по Email (требуется запущенный Mailpit)
Invoke-RestMethod -Method POST -Uri "http://localhost:8080/api/otp/generate" `
  -ContentType "application/json" `
  -Headers @{Authorization="Bearer $token"} `
  -Body '{"operationId":"op-email","channel":"EMAIL","destination":"user@test.com"}'

# Генерация OTP по SMS (требуется запущенный SmppSimulator)
Invoke-RestMethod -Method POST -Uri "http://localhost:8080/api/otp/generate" `
  -ContentType "application/json" `
  -Headers @{Authorization="Bearer $token"} `
  -Body '{"operationId":"op-sms","channel":"SMS","destination":"79001234567"}'

# Генерация OTP через Telegram
Invoke-RestMethod -Method POST -Uri "http://localhost:8080/api/otp/generate" `
  -ContentType "application/json" `
  -Headers @{Authorization="Bearer $token"} `
  -Body '{"operationId":"op-tg","channel":"TELEGRAM","destination":"user1"}'

# Валидация кода
Invoke-RestMethod -Method POST -Uri "http://localhost:8080/api/otp/validate" `
  -ContentType "application/json" `
  -Headers @{Authorization="Bearer $token"} `
  -Body '{"operationId":"op-1","code":"XXXXXX"}'

# Список пользователей (только для ADMIN)
$adminToken = (Invoke-RestMethod -Method POST -Uri "http://localhost:8080/api/auth/login" `
  -ContentType "application/json" `
  -Body '{"login":"admin","password":"admin123"}').token

Invoke-RestMethod -Method GET -Uri "http://localhost:8080/api/admin/users" `
  -Headers @{Authorization="Bearer $adminToken"}

# Обновить конфиг OTP
Invoke-RestMethod -Method PUT -Uri "http://localhost:8080/api/admin/config" `
  -ContentType "application/json" `
  -Headers @{Authorization="Bearer $adminToken"} `
  -Body '{"codeLength":8,"ttlSeconds":120}'

# Удалить пользователя
Invoke-RestMethod -Method DELETE -Uri "http://localhost:8080/api/admin/users/2" `
  -Headers @{Authorization="Bearer $adminToken"}
```

---

## Структура проекта

```
src/main/java/com/otpservice/
├── Main.java
├── dao/          # JDBC-репозитории (UserDao, OtpConfigDao, OtpCodeDao)
├── model/        # User, OtpCode, OtpConfig + enums (Role, OtpStatus)
├── scheduler/    # Планировщик: помечает просроченные OTP как EXPIRED
├── server/       # HttpServer, JWT-фильтр, обработчики запросов
├── service/      # Бизнес-логика + каналы уведомлений
├── tools/        # SmppSimulator — встроенный SMPP-эмулятор для тестирования
└── util/         # JWT, BCrypt, OTP-генератор, Jackson
```
