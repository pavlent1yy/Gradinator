# AGENTS.md

This file provides guidance to Codex (claude.ai/code) when working with code in this repository.

## Обзор

Gradinator — расписание ЯГК (Ярославский колледж градостроительства). Monorepo из трёх приложений:

| Модуль | Порт | Пакет | Отвечает за |
|---|---|---|---|
| `g-api` | 9090 | `com.pavlent1yy.gradinator` | скачивание и парсинг Excel, снапшоты расписания, REST расписания |
| `g-core` | 9091 | `com.pavlent1yy.gcore` | пользователи, JWT + refresh-сессии, OAuth2 (Google, GitHub, Yandex, VK ID), подтверждение email, прокси к g-api |
| `g-web` | 3042 | — | UI |

Цепочка вызовов строго `g-web → g-core → g-api`, web к g-api напрямую не ходит. Java-модули объединены корневым `pom.xml` (packaging `pom`), g-web живёт отдельно.

Приоритеты задач — в [`roadmap.md`](./roadmap.md).

## Команды

В корне `mvnw` нет (в README он упомянут ошибочно), есть только `g-api/mvnw` и `g-core/mvnw`. Из корня используется системный `mvn`:

```bash
mvn -pl g-core -am spring-boot:run          # запустить модуль
mvn -pl g-api test                           # тесты модуля
mvn -pl g-api test -Dtest=QueryServiceTest   # один класс (#method для одного метода)
mvn -pl g-api -am clean package -DskipTests  # сборка jar (как в Dockerfile)
```

g-api запускайте с рабочей директорией в **корне репо**: пути `api.storage-dir-path=./g-api/scheduleFiles` и `api.groups-config-path=./groups.cfg` относительные.

Команды g-web — в `g-web/AGENTS.md`.

Всё сразу: `docker compose up -d` (сначала `cp .env.example .env`). Прод: `compose.prod.yml` + скрипты в `deploy/` (nginx, TLS, `init-databases.sql`). CI — `.github/workflows/ci.yml` (lint g-web должен быть чистым, иначе CI красный); деплой — push в `main` → `deploy.yml` → `deploy/deploy.sh` на сервере (healthcheck + автооткат).

## Конфигурация

- По умолчанию профиль `local`: Postgres на `localhost:5433`, БД `gradin` (g-api) и `gradinator_core` (g-core), `ddl-auto=update`. В `prod` стоит `ddl-auto=validate`, поэтому **любое изменение схемы делается только Flyway-миграцией** (`<module>/src/main/resources/db/migration/V<N>__*.sql`), иначе прод не поднимется.
- Локальные секреты лежат в `<module>/src/main/resources/private-info.properties` (gitignored, подключается через `spring.config.import`). Нужны `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, а для g-core ещё `GOOGLE_CLIENT_ID/SECRET`, `GITHUB_CLIENT_ID/SECRET`, `YANDEX_CLIENT_ID/SECRET` и `VK_CLIENT_ID` (VK ID без секрета, PKCE). Нестандартные запросы userinfo/token для Yandex и VK собраны в `ProviderOAuth2UserService` и `VkIdTokenParametersConverter`.
- Тесты g-core идут на H2 в режиме PostgreSQL, Flyway выключен (`g-core/src/test/resources/application.properties`).
- Таймзона везде Europe/Moscow (compose, Dockerfile, `JAVA_TOOL_OPTIONS`).

## Архитектура: что неочевидно

### Роутинг g-web ↔ g-core
`g-web/next.config.ts` переписывает `/api/:path*` в `${G_CORE_URL}/:path*`, а `/oauth2/**` и `/login/oauth2/**` проксирует в g-core целиком, так что OAuth-рукопожатие идёт через origin фронта. Клиент `g-web/src/lib/api.ts` обращается к `/api/core/...`, поэтому **контроллеры g-core должны быть под префиксом `/core/...`** (`/core/auth`, `/core/schedule`). Контроллер с другим префиксом из web недоступен.

Авторизация на httpOnly-cookie (`AuthCookieService`): access JWT плюс refresh-сессия в БД (`RefreshSession`, `JwtRefreshTokenService`). После OAuth `OAuth2AuthenticationSuccessHandler` редиректит на `app.frontend-url` (страница profile: у нового OAuth-пользователя нет группы). Связка провайдер → пользователь хранится в `UserOAuthAccount`, у OAuth-пользователя `passwordHash = null`.

Пайплайн расписания описан в `g-api/AGENTS.md`.

g-core получает расписание и группы через `GApiClient` (`g-api.url`) и проксирует их в `/core/schedule/**`. Кроме того, все неадминские пути g-api доступны на сайте по тем же адресам `/api/...` через белый список `GApiProxyController` (`PUBLIC_PATHS` + `@GetMapping`, держать в синхроне). В обоих сервисах разрешён `/error` и error-dispatch: без этого любая ошибка на публичном пути превращается в 401/403.

Список эндпоинтов: `g-api/API_ENDPOINTS.md`, `g-core/API_ENDPOINTS.md`.
