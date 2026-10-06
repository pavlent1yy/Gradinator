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

### `GET` `/oauth2/authorization/google`

<i>Запускает вход через Google.</i>

Открыть этот адрес в браузере — пользователь будет перенаправлен на страницу входа Google.

После успешного входа пользователь автоматически возвращается в Gradinator и перенаправляется на `/profile`.

---

### `GET` `/login/oauth2/code/google`

<i>Служебный endpoint для завершения входа через Google.</i>

Самостоятельно вызывать его не нужно. Google использует его после авторизации пользователя.

---

### Google OAuth2

После успешного входа:

* создаётся пользователь, если его ещё нет;
* Google-аккаунт привязывается к новому/готовому пользователю;
* выдаются access и refresh токены:
    ```json
     {
          "accessToken": "...",
          "refreshToken": "..."
     }
    ```
* пользователь перенаправляется в `/profile`.

Для frontend'а основной адрес, который нужно использовать для запуска входа:

```text
GET /oauth2/authorization/google
```

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
