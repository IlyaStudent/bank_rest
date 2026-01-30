# Bank Cards Management API

REST API для управления банковскими картами с JWT-аутентификацией, ролевой моделью доступа и шифрованием данных.

## Запуск

```bash
# 1. Запуск PostgreSQL
docker-compose up -d

# 2. Сборка и запуск
./mvnw spring-boot:run

# 3. Swagger UI
open http://localhost:8080/swagger-ui.html
```

**Тестовый админ:** `admin` / `admin123`

## Технологии

- **Java 21** / **Spring Boot 3**
- **Spring Security** + **JWT**
- **Spring Data JPA** + **PostgreSQL**
- **Liquibase** (миграции БД)
- **AES-256** (шифрование карт)
- **Swagger/OpenAPI**
- **JUnit 5** + **Mockito**
- **Docker Compose**

## Архитектура

Слоистая архитектура с разделением ответственности:

- **Controller** — обработка HTTP-запросов, валидация входных данных
- **Service** — бизнес-логика, транзакции
- **Repository** — доступ к данным (Spring Data JPA)
- **DTO** — объекты для API (отделены от Entity)
- **Entity** — JPA-сущности

## Возможности

| Роль | Функционал |
|------|-----------|
| **ADMIN** | CRUD пользователей, создание/удаление карт, просмотр всех карт |
| **USER** | Просмотр своих карт, запрос блокировки, переводы между картами |

**Безопасность:**
- JWT-токены с настраиваемым сроком действия
- Шифрование номеров карт (AES-256)
- Маскирование при отображении (`**** **** **** 1234`)
- Ролевая модель доступа (RBAC)

## API Endpoints

| Метод | Endpoint | Описание | Роль |
|-------|----------|----------|------|
| POST | `/api/auth/register` | Регистрация | — |
| POST | `/api/auth/login` | Авторизация | — |
| GET | `/api/cards` | Список карт | USER |
| POST | `/api/cards` | Создание карты | ADMIN |
| POST | `/api/transfers` | Перевод | USER |
| GET | `/api/users` | Список пользователей | ADMIN |

## Тестирование

```bash
./mvnw test
```

Unit-тесты покрывают сервисный слой: `AuthService`, `CardService`, `TransferService`, `UserService`.
