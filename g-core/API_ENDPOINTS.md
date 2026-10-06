# Все команды G-CORE

## AUTH поинты

### Инструкция по Bearer Token

Для авторизованных запросов необходимо передать `Bearer Token` — access token.

Пример заголовка:

```text
Authorization: Bearer <accessToken>
```

### Rate limit

Лимит считается по IP клиента (за nginx и g-web — из `X-Forwarded-For`). При превышении ответ `429` с заголовком `Retry-After` (секунды):

```json
{
  "error": "Слишком много запросов, попробуйте позже",
  "message": "Rate limit exceeded"
}
```

| Категория | Эндпоинты | По умолчанию |
|---|---|---|
| `auth` | `/core/auth/login`, `/core/auth/register` | 20 в минуту (`RATE_LIMIT_AUTH_CAPACITY`) |
| `email` | `/core/auth/resend-verification` | 5 за 10 минут (`RATE_LIMIT_EMAIL_CAPACITY`) |
| `general` | всё остальное | 300 в минуту (`RATE_LIMIT_GENERAL_CAPACITY`) |

---

### `POST` `/core/auth/login`

<i>Аутентифицирует пользователя и выдает access и refresh токены</i>

На вход json raw:

```json
{
  "email": "user555@example.com",
  "password": "555"
}
```

Пример вывода:

```json
{
  "accessToken": "...",
  "refreshToken": "..."
}
```

---

### `GET` `/oauth2/authorization/{provider}`

<i>Запускает вход через внешний сервис.</i> `{provider}`: `google`, `github`, `yandex`, `vk`.

Открыть этот адрес в браузере — пользователь будет перенаправлен на страницу входа провайдера.

---

### `GET` `/login/oauth2/code/{provider}`

<i>Служебный callback для завершения входа.</i> Самостоятельно вызывать не нужно. Этот адрес надо указать как redirect URI в настройках приложения у провайдера: `https://<домен>/login/oauth2/code/{provider}`.

---

### OAuth2

После успешного входа:

* создаётся пользователь, если его ещё нет (по email; если email уже зарегистрирован, аккаунт провайдера привязывается к нему);
* access и refresh токены выставляются в httpOnly-cookie;
* пользователь перенаправляется на `/profile`.

При ошибке редирект на `/login?error=oauth`.

Особенности провайдеров:

* **Yandex**: в приложении на oauth.yandex.ru нужны права «Доступ к адресу электронной почты» и «Доступ к логину, имени и фамилии»; userinfo запрашивается с заголовком `Authorization: OAuth <token>`.
* **VK ID**: публичный клиент с PKCE, client secret не используется; в обмен кода передаются `device_id` и `state` из callback; userinfo запрашивается `POST https://id.vk.com/oauth2/user_info`. Нужен доступ к email в настройках приложения VK ID.

---

### `POST` `/core/auth/register`

<i>Регистрирует нового пользователя</i>

На вход:

```json
{
  "email": "user555@example.com",
  "password": "555",
  "confirmPassword": "555",
  "group": "ИС1-12",
  "department": "OIT"
}
```

Пример вывода при успехе:

```json
{
  "id": 8,
  "email": "user555@example.com",
  "group": "ИС1-12",
  "department": "OIT",
  "role": "STUDENT"
}
```

---

### `POST` `/core/auth/logout`

<i>Логаутит сессию пользователя и деактивирует refresh-токен</i>

На вход:

```json
{
    "refreshToken": "..."
}
```

Вывод, при успешном логауте: `Статус 204: NO CONTENT`

Вывод, при провальном логауте: `Статус 401: UNAUTHORAZED`

---

### `GET` `/core/auth/me`

<i>Выдает данные текущего пользователя</i>

Необходимо передать `Bearer Token` — access token.

Пример вывода:

```json
{
    "id": 8,
    "email": "user555@example.com",
    "group": "ИС1-12",
    "department": "OIT",
    "role": "STUDENT"
}
```

---

### `POST` `/core/auth/refresh`

<i>Выдает новый access token на основе refresh token</i>

На вход:

```json
{
    "refreshToken": "..."
}
```

Пример ответа: `200 OK`

---

## Пользовательские поинты

### `PUT` `/core/user/change-password`

<i>Изменяет пароль текущего пользователя</i>

Необходимо передать `Bearer Token` — access token.

На вход:

```json
{
    "oldPassword": "555",
    "newPassword": "123"
}
```

Пример ответа: `200 OK`. Неверный старый пароль или аккаунт без пароля (вход через OAuth): `400 {"error": "..."}`

---

### `PUT` `/core/user/change-group`

<i>Изменяет учебную группу текущего пользователя</i>

Необходимо передать `Bearer Token` — access token.

На вход новая группа:

```json
{
    "newGroup": "ИС1-33"
}
```

Пример ответа: `200 OK`. Группы нет в G-API: `400 {"error": "..."}`

---

# Поинты расписания

G-Core предоставляет клиентский доступ к API расписания G-API.

Все endpoint'ы являются прокси к соответствующим endpoint'ам G-API и имеют те же параметры и формат ответа.

Доступны по EP: `/core/schedule/..`

* `/groups` — дает группы
* `/groups/departments` — дает группы с отделениями
* `/groups/find-department` — поиск отделения по группе
* `/groups/department-names` — все имена отделений

Подробное описание endpoint'ов находится в [`G-API/API_ENDPOINTS.md`](../g-api/API_ENDPOINTS.md).

---

## Пропуски (`/core/absences`, нужна авторизация)

Пропущенная пара (`MISSED`) = 2 часа, опоздание (`LATE`) = 1 час. На одну пару в день одна отметка. Даты в будущем отклоняются (`400`).

| Метод | Путь | Что делает |
|---|---|---|
| `GET` | `/core/absences?from=YYYY-MM-DD&to=YYYY-MM-DD` | список отметок за период (не больше 400 дней) |
| `PUT` | `/core/absences` | отметить пару: `{"date","pairNumber":1..8,"type":"MISSED"\|"LATE","subject"?}`; повторная отметка меняет тип |
| `DELETE` | `/core/absences?date=&pairNumber=` | снять отметку, `204` |
| `POST` | `/core/absences/day` | `{"date"}` — отметить пропуском все пары дня по расписанию группы пользователя; `400`, если группы нет или пар нет |
| `DELETE` | `/core/absences/day?date=` | очистить день, `204` |
| `GET` | `/core/absences/stats` | `{week, month, semester, total}`, в каждом `{from, to, hours, missedPairs, lates}`. Семестры: 1 сен – 31 дек и 1 янв – 31 авг |

## Удаление аккаунта

### `DELETE` `/core/user`

Удаляет пользователя и все его данные (пропуски, привязки OAuth, сессии, токены подтверждения), стирает auth-cookie. Ответ `204`.

---

## Поиск и справочники (`/core/schedule/...`, без авторизации)

Расписание всех групп на день берётся из g-api `/api/schedule?date=` и кэшируется в g-core на 5 минут, справочники — на час. Учитывается тип недели: для знаменателя ячейка знаменателя, иначе числитель.

| Метод | Путь | Что делает |
|---|---|---|
| `GET` | `/core/schedule/search?q=&type=ANY\|TEACHER\|SUBJECT\|ROOM&date=` | пары всех групп, где запрос входит в преподавателя / предмет / аудиторию (без учёта регистра и `ё`). `q` короче 2 символов → `[]`. Не больше 300 результатов: `[{group, pairNumber, subjects, teachers, rooms, hasChanges}]` |
| `GET` | `/core/schedule/free-rooms?date=` | по каждой паре дня: `{pairNumber, freeRooms, busyRooms}`. Свободные — аудитории из расписания этого дня, не занятые на паре |
| `GET` | `/core/schedule/teachers` | преподаватели (очищенный и отсортированный список g-api) |
| `GET` | `/core/schedule/subjects` | предметы |
| `GET` | `/core/schedule/rooms` | аудитории (`А203,М106` разбивается на две) |
| `GET` | `/core/schedule/current-weektype` | `{label, weekType}` из g-api |
