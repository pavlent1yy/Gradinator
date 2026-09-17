# G-Core

G-Core - основное backend-приложение Gradinator на Spring Boot.

Сервис отвечает за пользователей, аутентификацию, авторизацию, пользовательские настройки и взаимодействие с G-API.

## Ответственность

G-Core отвечает за:

* регистрацию и управление пользователями;
* аутентификацию и авторизацию;
* выдачу и обновление JWT;
* управление refresh sessions;
* пользовательские настройки;
* проверку прав доступа;
* взаимодействие с G-API;
* предоставление API для G-Web.

G-Core является основной backend-точкой входа для клиентских приложений

## Архитектура

Основной поток запросов:

```text
G-Web
   │
   ▼
G-Core
   │
   ▼
G-API
```

## Аутентификация

Для аутентификации используется JWT.

Система использует два типа токенов:

* **Access token** — используется для авторизации запросов;
* **Refresh token** — используется для получения нового access token.

Refresh sessions хранятся в базе данных и позволяют управлять жизненным циклом refresh token.

Основная логика аутентификации находится в `AuthService`.

Проверка access token выполняется через `JwtAuthenticationFilter`.

## Взаимодействие с G-API

Для работы с расписанием G-Core использует внутренний HTTP-клиент:

```text
G-Core
   │
   │ HTTP
   ▼
G-API
```

Основная реализация находится в `GApiClient`.

G-Core не занимается parsing исходных файлов и хранением расписания. Эти задачи находятся в зоне ответственности G-API.

## Структура проекта

Основные части приложения:

```text
g-core/
└── src/
    └── main/
        └── java/
            └── com/pavlent1yy/gcore/
                ├── controller/
                ├── service/
                │   └── jwt/
                ├── client/
                ├── entity/
                ├── repository/
                ├── config/
                └── ...
```

Ключевые компоненты:

* `AuthService` — аутентификация и работа с токенами;
* `JwtAuthenticationFilter` — обработка JWT в HTTP-запросах;
* `GApiClient` — взаимодействие с G-API;
* controllers — HTTP API приложения;
* entities/repositories — работа с пользовательскими данными и refresh sessions.

## Конфигурация

Для локальной сборки проекта необходимо создать файл `private-info.properties` по следующему примеру:

```properties
DB_USERNAME=
DB_PASSWORD=

jwt.secret=
jwt.expiration=3600000
jwt.access-expiration=900000
jwt.refresh-expiration=2592000000
```

Значения expiration указаны в миллисекундах:

* `JWT_ACCESS_EXPIRATION` — 15 минут;
* `JWT_REFRESH_EXPIRATION` — 30 дней.

Файл `private-info.properties` содержит приватные данные и не должен добавляться в Git.

Адрес G-API задаётся через конфигурацию приложения.

Для локального запуска:

```properties
G_API_URL=http://localhost:9090
```

В production значение указывает на G-API внутри Docker network.

## Локальный запуск

Запуск через Maven:

```bash
./mvnw -pl g-core spring-boot:run
```

По умолчанию G-Core доступен тут:

```text
http://localhost:9091
```

Самый простой вариант создать базу на локали - поставить `spring.jpa.hibernate.ddl-auto=update` в properties

Для полноценного запуска также требуется доступная PostgreSQL база данных и запущенный G-API для полноценной работы G-CORE как клиента

## Сборка

Сборка модуля:

```bash
./mvnw -pl g-core package
```

Запуск тестов:

```bash
./mvnw -pl g-core test
```

Подробнее об общей архитектуре:

[Gradinator](../README.md)

Другие компоненты:

* [G-API](../g-api/README.md)
* [G-Web](../g-web/README.md)
