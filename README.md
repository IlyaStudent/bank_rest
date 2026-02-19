[![CI](https://github.com/IlyaStudent/bank_rest/actions/workflows/ci.yml/badge.svg)](https://github.com/IlyaStudent/bank_rest/actions/workflows/ci.yml)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=IlyaStudent_bank_rest&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=IlyaStudent_bank_rest)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=IlyaStudent_bank_rest&metric=bugs)](https://sonarcloud.io/summary/new_code?id=IlyaStudent_bank_rest)
[![Duplicated Lines (%)](https://sonarcloud.io/api/project_badges/measure?project=IlyaStudent_bank_rest&metric=duplicated_lines_density)](https://sonarcloud.io/summary/new_code?id=IlyaStudent_bank_rest)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=IlyaStudent_bank_rest&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=IlyaStudent_bank_rest)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=IlyaStudent_bank_rest&metric=coverage)](https://sonarcloud.io/summary/new_code?id=IlyaStudent_bank_rest)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=IlyaStudent_bank_rest&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=IlyaStudent_bank_rest)
[![Maintainability](https://sonarcloud.io/api/project_badges/measure?project=IlyaStudent_bank_rest&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=IlyaStudent_bank_rest)
# Bank Cards Management API

REST API для управления банковскими картами с JWT-аутентификацией (Access + Refresh токены), ролевой моделью доступа, шифрованием данных, Redis для управления токенами и интеграцией с Apache Kafka.

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
# ENCRYPTION_KEY (AES-256, 32 байта) — через утилиту проекта
./mvnw -q compile && java -cp target/classes com.example.bankcards.util.KeyGeneratorUtil

# JWT_SECRET (HS512, минимум 64 байта)
openssl rand -base64 64
```

> **Важно:** Приложение не запустится без `ENCRYPTION_KEY` и `JWT_SECRET` (fail fast).

## Запуск

### 1. Запуск инфраструктуры (PostgreSQL + Kafka + Redis)

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

| Компонент | Технологии                                |
|-----------|-------------------------------------------|
| **Backend** | Java 21, Spring Boot 3.2                  |
| **Security** | Spring Security, JWT, AES-256             |
| **Database** | PostgreSQL 15, Spring Data JPA, Liquibase |
| **Caching/Tokens** | Redis 7, Spring Data Redis                |
| **Messaging** | Apache Kafka, Spring Kafka                |
| **Docs** | Swagger/OpenAPI                           |
| **Testing** | JUnit 5, Mockito, Spring Security Test    |
| **Infrastructure** | Docker, Kubernetes, Jenkins CI/CD         |

## CI/CD и инфраструктура

### Docker

Приложение упаковано в оптимизированный Docker-образ с multi-stage сборкой:

- **Build stage** — Eclipse Temurin JDK 21 Alpine, сборка Maven с кэшированием зависимостей
- **Runtime stage** — Eclipse Temurin JRE 21 Alpine (~382MB)
- Встроенный healthcheck

### Kubernetes

Весь стек описан декларативно в YAML-манифестах (`k8s/`):

| Компонент | Тип | Описание |
|-----------|-----|----------|
| **bank-rest-app** | Deployment + NodePort (30080) | Основное приложение |
| **PostgreSQL** | Deployment + ClusterIP | База данных |
| **Redis** | Deployment + ClusterIP | Хранение токенов, blacklist |
| **Kafka + Zookeeper** | Deployment + ClusterIP | Брокер сообщений |
| **Secrets** | Secret | Пароль БД, ключ шифрования, JWT-секрет |
| **ConfigMap** | ConfigMap | URL базы данных, адреса Redis и Kafka, TTL токенов |

### Jenkins CI/CD Pipeline

Автоматический пайплайн из 5 этапов:

```
Checkout → Build & Test (224 теста) → Docker Build → Deploy to K8s → Verify (Health Check)
```

- **Секреты** хранятся в Jenkins Credentials
- **Тесты** — JUnit-отчёты публикуются в Jenkins
- **Деплой** — автоматическое создание K8s-секретов, применение манифестов, rollout restart
- **Верификация** — health check с повторными попытками до подтверждения работоспособности

### Codespaces Dev Environment

Проект настроен для запуска в GitHub Codespaces с полным окружением:

```bash
# Автоматически при создании Codespace (postCreateCommand):
.devcontainer/setup.sh   # Поднимает Minikube + Jenkins

# Ручной деплой (альтернатива Jenkins):
bash k8s/deploy.sh
```

| Сервис | URL |
|--------|-----|
| Приложение | `http://<minikube-ip>:30080` |
| Swagger UI | `http://<minikube-ip>:30080/swagger-ui.html` |
| Jenkins | `http://localhost:8888` |

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
├── k8s/                 # Kubernetes манифесты
│   ├── namespaces.yml
│   ├── configmap.yml
│   ├── secrets.yml.example
│   ├── postgres.yml
│   ├── redis.yml
│   ├── zookeeper.yml
│   ├── kafka.yml
│   ├── app.yml
│   ├── deploy.sh        # Скрипт ручного деплоя
│   └── destroy.sh       # Скрипт удаления ресурсов
├── .devcontainer/       # Codespaces конфигурация
│   ├── devcontainer.json
│   └── setup.sh         # Minikube + Jenkins setup
├── Dockerfile           # Multi-stage Docker сборка
├── Jenkinsfile          # CI/CD pipeline
├── docker-compose.yml   # Локальный запуск (PostgreSQL + Kafka + Redis)
└── pom.xml
```

## Возможности

| Роль | Функционал |
|------|-----------|
| **ADMIN** | CRUD пользователей, создание/удаление карт, назначение ролей, просмотр всех карт |
| **USER** | Просмотр своих карт, запрос блокировки, переводы между картами, история переводов |

**Безопасность:**
- JWT Access + Refresh токены (stateless аутентификация)
- Access Token (15 мин) — авторизация запросов, содержит `jti` (JWT ID) для идентификации
- Refresh Token (7 дней) — обновление пары токенов без повторного логина
- Token Rotation — при refresh старый токен удаляется, выдаётся новый
- Access Token Blacklist — при logout access token мгновенно отзывается через Redis
- Шифрование номеров карт (AES-256-GCM)
- Маскирование при отображении (`**** **** **** 1234`)
- Ролевая модель доступа (RBAC)
- Хеширование паролей (BCrypt)

**Kafka интеграция:**
- При успешном переводе публикуется `TransferEvent` в топик `bank.transfers`
- Notification Service получает события и логирует уведомления

**Redis интеграция:**
- Хранение Refresh Token с автоматическим удалением по TTL (7 дней)
- Blacklist Access Token при logout с TTL равным оставшемуся времени жизни токена

## API Endpoints

### Аутентификация
| Метод | Endpoint | Описание |
|-------|----------|----------|
| POST | `/api/auth/register` | Регистрация, получение Access + Refresh токенов |
| POST | `/api/auth/login` | Авторизация, получение Access + Refresh токенов |
| POST | `/api/auth/refresh` | Обновление пары токенов по Refresh Token |
| POST | `/api/auth/logout` | Отзыв Refresh Token + blacklist Access Token |

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
4. Получены новые токены        → старый Refresh Token удалён из Redis (Token Rotation)
5. POST /api/auth/logout        → Refresh Token удалён + Access Token в blacklist
6. GET  /api/cards (после logout) → 401 (Access Token в blacklist)
```

### Хранение токенов

| Токен | Сервер | Клиент |
|-------|--------|--------|
| **Access Token** | Не хранится (stateless JWT, jti в blacklist при logout) | localStorage / memory |
| **Refresh Token** | Redis (`refresh:{token}` → userId, TTL 7 дней) | localStorage / httpOnly cookie |

### Redis ключи

| Паттерн | Значение | TTL | Описание |
|---------|----------|-----|----------|
| `refresh:{uuid}` | userId | 7 дней | Refresh token → ID пользователя |
| `blacklist:{jti}` | `"1"` | Оставшееся время жизни Access Token | Отозванные Access Token |

## Тестирование

```bash
# Все тесты
./mvnw test
```

### Покрытие тестами

| Слой | Классы | Подход | Что проверяется |
|------|--------|--------|-----------------|
| **Controller** | `AuthController`, `CardController`, `TransferController`, `UserController` | `@WebMvcTest` + MockMvc | HTTP статусы, валидация request body, авторизация по ролям, маппинг response |
| **Service** | `AuthServiceImpl`, `CardServiceImpl`, `TransferServiceImpl`, `UserServiceImpl`, `KafkaProducerServiceImpl` | Unit + Mockito | Бизнес-логика, обработка исключений, взаимодействие с репозиториями |
| **Security** | `JwtProvider`, `JwtAuthenticationFilter`, `CustomUserDetailService` | Unit (реальный JWT / Mockito) | Генерация и валидация токенов, фильтрация запросов, обработка expired/blacklisted токенов |
| **Redis** | `RedisTokenServiceImpl` | Unit + Mockito | Хранение refresh-токенов, blacklist access-токенов, TTL |
| **Util** | `EncryptionUtil`, `CardMaskingUtil`, `KeyGeneratorUtil` | Unit | AES-256-GCM шифрование/дешифрование, маскирование номеров карт, генерация ключей |

Все тесты — чистые unit-тесты, не требуют запущенных PostgreSQL, Redis или Kafka.

## Связанные проекты

- [bank_notification_service](https://github.com/IlyaStudent/bank_notification_service) — Kafka consumer для уведомлений о переводах
