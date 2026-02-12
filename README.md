# Bank Cards Management API

REST API для управления банковскими картами с JWT-аутентификацией (Access + Refresh токены), ролевой моделью доступа, шифрованием данных и интеграцией с Apache Kafka.

## Архитектура системы

**Bank API** — основной сервис для управления картами и переводами. При успешном переводе публикует событие в Kafka.

**Notification Service** — микросервис-consumer, получает события о переводах и логирует уведомления для отправителя и получателя.

## Конфигурация

Перед запуском создайте файл `.env` на основе шаблона:

```bash
cp .env.example .env
```

Генерация ключей:
```bash
# ENCRYPTION_KEY
openssl rand -base64 32

# JWT_SECRET
openssl rand -base64 32
```

> **Важно:** Приложение не запустится без `ENCRYPTION_KEY` и `JWT_SECRET` (fail fast).

## Запуск

### 1. Запуск инфраструктуры (PostgreSQL + Kafka)

```bash
docker-compose up -d
```

### 2. Запуск Bank API

```bash
./mvnw spring-boot:run
```

Swagger UI: http://localhost:8080/swagger-ui.html

### 3. Запуск Notification Service (опционально)

```bash
cd ../bank_notification_service
./mvnw spring-boot:run
```

Сервис будет слушать события на порту 8081.

**Тестовый админ:** `admin` / `admin123`

## Технологии

| Компонент | Технологии |
|-----------|-----------|
| **Backend** | Java 21, Spring Boot 3.2 |
| **Security** | Spring Security, JWT, AES-256 |
| **Database** | PostgreSQL, Spring Data JPA, Liquibase |
| **Messaging** | Apache Kafka, Spring Kafka |
| **Docs** | Swagger/OpenAPI |
| **Testing** | JUnit 5, Mockito |
| **Infrastructure** | Docker Compose |

## Структура проекта

```
bank_rest/
├── src/main/java/com/example/bankcards/
│   ├── controller/      # REST endpoints
│   ├── service/         # Бизнес-логика
│   ├── repository/      # Доступ к данным
│   ├── entity/          # JPA-сущности
│   ├── dto/             # Data Transfer Objects
│   ├── event/           # Kafka events (TransferEvent)
│   ├── security/        # JWT, фильтры
│   ├── config/          # Конфигурации
│   ├── mapper/          # Entity <-> DTO
│   ├── exception/       # Обработка ошибок
│   ├── util/            # Утилиты (шифрование, маскирование)
│   └── validation/      # Custom validators
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/    # Liquibase миграции
├── docker-compose.yml   # PostgreSQL + Kafka + Zookeeper
└── pom.xml
```

## Возможности

| Роль | Функционал |
|------|-----------|
| **ADMIN** | CRUD пользователей, создание/удаление карт, просмотр всех карт |
| **USER** | Просмотр своих карт, запрос блокировки, переводы между картами |

**Безопасность:**
- JWT Access + Refresh токены (stateless аутентификация)
- Access Token (15 мин) — авторизация запросов
- Refresh Token (7 дней) — обновление пары токенов без повторного логина
- Token Rotation — при refresh старый токен отзывается, выдаётся новый
- Шифрование номеров карт (AES-256-GCM)
- Маскирование при отображении (`**** **** **** 1234`)
- Ролевая модель доступа (RBAC)

**Kafka интеграция:**
- При успешном переводе публикуется `TransferEvent` в топик `bank.transfers`
- Notification Service получает события и логирует уведомления

## API Endpoints

### Аутентификация
| Метод | Endpoint | Описание |
|-------|----------|----------|
| POST | `/api/auth/register` | Регистрация, получение Access + Refresh токенов |
| POST | `/api/auth/login` | Авторизация, получение Access + Refresh токенов |
| POST | `/api/auth/refresh` | Обновление пары токенов по Refresh Token |
| POST | `/api/auth/logout` | Отзыв Refresh Token (выход из сессии) |

### Карты
| Метод | Endpoint | Описание | Роль |
|-------|----------|----------|------|
| GET | `/api/cards` | Список карт пользователя | USER |
| GET | `/api/cards/{id}` | Информация о карте | USER |
| POST | `/api/cards` | Создание карты | ADMIN |
| PUT | `/api/cards/{id}` | Обновление карты | ADMIN |
| DELETE | `/api/cards/{id}` | Удаление карты | ADMIN |
| PUT | `/api/cards/{id}/block` | Блокировка карты | USER |

### Переводы
| Метод | Endpoint | Описание | Роль |
|-------|----------|----------|------|
| POST | `/api/transfers` | Выполнить перевод | USER |
| GET | `/api/transfers` | История переводов | USER |

### Пользователи (Admin)
| Метод | Endpoint | Описание | Роль |
|-------|----------|----------|------|
| GET | `/api/users` | Список пользователей | ADMIN |
| GET | `/api/users/{id}` | Информация о пользователе | ADMIN |
| POST | `/api/users` | Создание пользователя | ADMIN |
| PUT | `/api/users/{id}` | Обновление пользователя | ADMIN |
| DELETE | `/api/users/{id}` | Удаление пользователя | ADMIN |
| POST | `/api/users/{id}/roles` | Назначение роли | ADMIN |

## Аутентификация (JWT Access + Refresh)

### Flow

```
1. POST /api/auth/login        → { accessToken, refreshToken, expiresIn, user }
2. GET  /api/cards              → Authorization: Bearer <accessToken>
3. Access Token истёк (401)     → POST /api/auth/refresh { refreshToken }
4. Получены новые токены        → старый Refresh Token отозван (Token Rotation)
5. POST /api/auth/logout        → Refresh Token отозван, сессия завершена
```

### Хранение токенов

| Токен | Сервер | Клиент |
|-------|--------|--------|
| **Access Token** | Не хранится (stateless JWT) | localStorage / memory |
| **Refresh Token** | Таблица `refresh_tokens` в БД | localStorage / httpOnly cookie |

```

## Тестирование

```bash
# Все тесты
./mvnw test
```

### Покрытие тестами

**Controller тесты** (`@WebMvcTest`):
- Проверка HTTP статусов (200, 201, 400, 401, 403, 404, 409, 422)
- Валидация request body
- Аутентификация и авторизация
- Маппинг response

**Service тесты** (Unit с Mockito):
- Бизнес-логика
- Обработка исключений
- Взаимодействие с репозиториями

## Kafka команды

```bash
# Список топиков
docker exec bank_rest_kafka kafka-topics --list --bootstrap-server localhost:9092

# Просмотр сообщений в топике
docker exec bank_rest_kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic bank.transfers \
  --from-beginning

# Информация о consumer group
docker exec bank_rest_kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe --group bank_notification_service
```

## Связанные проекты

- [bank_notification_service](https://github.com/IlyaStudent/bank_notification_service) — Kafka consumer для уведомлений о переводах
