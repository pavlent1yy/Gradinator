# Все команды G-CORE

## AUTH поинты

### Инструкция по Bearer Token

Для авторизованных запросов необходимо передать `Bearer Token` — access token.

Пример заголовка:

```text
Authorization: Bearer <accessToken>
```

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

### `PUT` `/user/change-password`

<i>Изменяет пароль текущего пользователя</i>

Необходимо передать `Bearer Token` — access token.

На вход:

```json
{
    "oldPassword": "555",
    "newPassword": "123"
}
```

Пример ответа: `200 OK`

---

### `PUT` `/user/change-group`

<i>Изменяет учебную группу текущего пользователя</i>

Необходимо передать `Bearer Token` — access token.

На вход новая группа:

```json
{
    "newGroup": "ИС1-33"
}
```

Пример ответа: `200 OK`

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
