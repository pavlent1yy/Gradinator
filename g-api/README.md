# G-API

G-API — backend-сервис Gradinator, отвечающий за получение,
обработку, хранение и предоставление данных расписания Ярославского колледжа градостроительства.

Сервис получает расписание из сырых исходных Excel-файлов, преобразует его во внутреннюю 
модель и сохраняет результат в виде снапшотов в базу. После этого данные доступны через REST API для G-Core
и других внутренних потребителей.

## Ответственность

G-API отвечает за:

- импорт исходных Excel-файлов;
- parsing и normalization расписания;
- построение snapshots расписания;
- хранение snapshots и истории их изменений;
- предоставление расписания через REST API;
- предоставление справочных данных;
- кэширование часто запрашиваемых данных;
- защиту API и rate limiting;
- административное управление parsing и snapshot build operations;
- monitoring состояния сервиса.

## Архитектура

Основной поток обработки расписания:

```text
Excel source file
       │
       ▼
┌──────────────┐
│    Parser    │
└──────┬───────┘
       │
       │ normalized data
       ▼
┌──────────────┐
│   Snapshot   │
│    Builder   │
└──────┬───────┘
       │
       │ persist
       ▼
┌──────────────┐
│  PostgreSQL  │
└──────┬───────┘
       │
       │ query
       ▼
┌──────────────┐
│ Schedule API │
└──────┬───────┘
       │
       ▼
     G-Core
```

Такое разделение позволяет изолировать структуру исходников от внутренней модели расписания

Например, изменение структуры Excel-файла должно требовать изменений преимущественно в parser,
не затрагивая отстальные модули G-Core и G-Web.

## Источник данных

Основным источником расписания являются Excel-файлы колледжа, расположенные на <i>[сайте колледжа](https://ygk.edu.yar.ru/raspisanie.html)</i> 
 
Перед сохранением данные проходят через несколько этапов:

1. Parser читает исходный workbook.
2. Полученные данные преобразуются во внутреннюю модель.
3. Snapshot Builder формирует snapshot расписания.
4. Snapshot сохраняется в PostgreSQL БД.
5. Schedule API предоставляет сохранённые данные через REST API.

Такой подход позволяет хранить конкретные состояния расписания и обращаться к историческим данным.

## API

G-API предоставляет REST API.

Полный список endpoint'ов и их краткое описание находятся в [`API_ENDPOINTS.md`](./API_ENDPOINTS.md).

G-API не управляет пользовательскими данными и аккаунтами Gradinator. 
Все это дело находится в G-Core.

## Технологии

- Java
- Spring Boot
- Spring Security
- Spring Data JPA
- Hibernate
- Apache POI
- PostgreSQL
- Maven

## Структура проекта

Основные части приложения:

```text
g-api/
└── src/
    └── main/
        └── java/
            └── com/pavlent1yy/gradinator/
                ├── controller/
                ├── service/
                │   └── parser/
                ├── config/
                ├── entity/
                ├── repository/
                └── ...
```

Ключевые компоненты:

- `ExcelFileParserService` — parsing исходных Excel-файлов;
- `SnapshotBuildService` — построение snapshots;
- `ScheduleController` — REST API расписания;
- `AdminSnapshotController` — административные операции со snapshots;
- `CacheConfig` — конфигурация кэширования;
- `SecurityConfig` — конфигурация безопасности API.

## Конфигурация

Основные параметры приложения задаются через Spring configuration и environment variables.

G-API использует PostgreSQL для хранения snapshots и связанных данных.

В production окружении адрес базы данных и остальные параметры передаются через Docker Compose и `.env`.

## Локальный запуск

Для запуска G-API локально необходимо настроить подключение к PostgreSQL.

После этого приложение можно запустить через Maven:

```bash
./mvnw -pl g-api spring-boot:run
```

По умолчанию сервис доступен по адресу:

```text
http://localhost:9090
```

## Сборка

Сборка отдельного модуля:

```bash
./mvnw -pl g-api package
```

Сборка с запуском тестов:

```bash
./mvnw -pl g-api test
```

Также для локальной сборки проекта необходимо создать `private-info.properties` файл по следующему примеру:
```
DB_USERNAME=
DB_PASSWORD=
```

Файл `private-info.properties` содержит приватные данные и не должен добавляться в Git.

Самый простой вариант создать базу на локали - поставить spring.jpa.hibernate.ddl-auto=update в properties

## Связь с другими компонентами

G-API является внутренним сервисом Gradinator.

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

G-Web не обращается к G-API напрямую.

Подробнее об архитектуре всей системы:

[Gradinator](../README.md)

Другие компоненты:

- [G-Core](../g-core/README.md)
- [G-Web](../g-web/README.md)

## P.S
### Благодарность
При разработке G-API мне очень помогли наработки проекта [`spring-ygk-raspisanie`](https://github.com/jestaaas/spring-ygk-raspisanie/tree/main).

Спасибо автору проекта за проделанную работу - она существенно упростила начальный этап разработки Gradinator,
хоть и сам проект с архитектурной точки зрения говнище страшное
