# План рефакторинга: автоподключение STOMP + MQTT по кнопке

## Контекст

Сейчас функция `connect()` в `app/src/main/resources/Static/app.js:20` делает две вещи сразу:

1. Поднимает **STOMP-соединение** (SockJS + Stomp, подписка на `/topic/data`)
2. Отправляет `/app/connect` с `doConnect`, что через `ConnectManagerImpl.connect()` (`app/src/main/java/ru/maxeltr/homeMq2t/Service/UI/ConnectManagerImpl.java:69`) подключается к **MQTT-брокеру**

Нужно развязать эти два независимых соединения:
- STOMP-соединение устанавливается автоматически при старте приложения.
- Соединение к MQTT-брокеру происходит только по нажатию кнопки Connect.

## Шаг 1 — Разделить функции в `app.js`

- **`connectStomp()`**: только SockJS + Stomp + подписка на `/topic/data`. НЕ отправляет `connectTopic`.
- **`connectMqtt()`**: только `stompClient.send(connectTopic, {}, {'id': 'doConnect'})` (строки `app.js:33`) — запускает MQTT.
- **`disconnectMqtt()`**: только `send('/app/disconnect')`. STOMP-канал остаётся живым.

## Шаг 2 — Разделить управление кнопками

Заменить единый флаг `setConnected()` (`app.js:13`) на два независимых состояния:

- **STOMP-connected** — управляется автоподключением.
- **MQTT-connected** — управляется кнопками Connect/Disconnect.

Логика кнопок:

- После автозапуска STOMP: Connect активна (ждёт клика для MQTT), Disconnect выключена.
- После `connectMqtt()`: Connect выключена, Disconnect активна.
- После `disconnectMqtt()`: обратно.

## Шаг 3 — Автозапуск STOMP при старте

В `$(function () {...})` (`app.js:188`) вызвать `connectStomp()` сразу. Клик кнопки Connect — только `connectMqtt()`.

## Шаг 4 — Защита от гонок состояний

- Блокировать Connect, пока STOMP не подключен (чтобы `send('/app/connect')` не ушёл до готовности подписки).
- `connectMqtt()` игнорируется, если MQTT уже подключен (отдельный флаг).

## Шаг 5 — (опционально) `index.html`

Переименовать «Connect» → «Connect MQTT» и добавить индикатор STOMP-статуса (косметика).

## Итоговые обработчики

```js
$(function () {
    connectStomp();                       // авто-STOMP

    $("#connect").click(function () {     // MQTT только по кнопке
        connectMqtt();
    });
    $("#disconnect").click(function () {
        disconnectMqtt();
    });
});
```

## Примечание

Серверную часть менять не нужно — `ConnectManagerImpl` уже корректно разделяет подписки (STOMP) и подключение к брокеру (MQTT через mediator).