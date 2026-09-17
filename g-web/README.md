# G-Web

G-Web - web-клиент Gradinator на Next.js.

Приложение предоставляет пользовательский интерфейс для работы с Gradinator и взаимодействует с backend через G-Core.

## Ответственность

- отображение расписания;
- управление состоянием пользователя и аутентификацией;
- взаимодействие с G-Core API;
- управление состоянием расписания;
- отображение пользовательских данных.

G-Web не подрузумивает взаимодействие с G-API напрямую

## Технологии

- Next.js
- React
- TypeScript
- Tailwind CSS

## Структура

Основные части приложения:

```text
g-web/
└── src/
    ├── app/            # страницы и layout'ы
    │   └── providers/  # React providers
    ├── components/     # UI-компоненты
    └── lib/            # клиентская логика и API
```

Ключевые компоненты:

- `AuthProvider` — состояние аутентификации;
- `ScheduleProvider` — состояние расписания;
- `api.ts` — клиент для взаимодействия с backend API.

## Конфигурация

Адрес G-Core задаётся через environment variables.

Для локальной разработки:

```env
G_CORE_URL=http://localhost:9091
```

В production значение указывает на G-Core внутри Docker network.

## Локальный запуск

Установить зависимости:

```bash
npm install
```

Запустить development server:

```bash
npm run dev
```

После запуска приложение доступно по адресу:

```text
http://localhost:3000
```

## Production

Production-версия G-Web собирается и запускается через Docker Compose из корневого проекта.

Подробнее:

- [Gradinator](../README.md)
- [G-Core](../g-core/README.md)
