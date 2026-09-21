# Каталог планов проекта homeMq2t

Индекс планирования и документации проекта. Документы, относящиеся к текущей работе, лежат в корне `plans/`; завершённые планы перемещаются в `plans/done/`.

## Активные документы

| Документ | Назначение | Статус |
|----------|------------|--------|
| [`architecture.md`](architecture.md) | Текущая архитектура приложения (карта для разработчиков и ИИ) | 📌 Живой справочник |
| [`Roadmap.md`](Roadmap.md) | Будущие функциональные улучшения: MQTT, UI, планировщик, тех. долг | 🔄 В работе |
| [`architectural-debt-remediation.md`](architectural-debt-remediation.md) | План устранения архитектурного долга по фазам (P0–P2), учёт лимитов Orange Pi Zero 512 МБ | 🔄 В работе |
| [`msg-impl-record-refactor.md`](msg-impl-record-refactor.md) | Замена `MsgImpl` (Builder) на Java `record` | ⏳ Запланировано (не начато) |
| [`subscribe-refactor.md`](subscribe-refactor.md) | Ретраи подписки SUBSCRIBE с `ConcurrentHashMap` и таймаутом | 🔶 Частично внедрено: `PENDING_SUBSCRIBES` + таймаут есть; ретраи с `MAX_RETRY_SUB_ATTEMPTS` не реализованы |

## Завершённые планы (архив)

| Документ | Что было сделано |
|----------|------------------|
| [`done/refactoring-remove-command-component.md`](done/refactoring-remove-command-component.md) | Удалены Command/Component сущности и сервисы, динамическая загрузка .jar (ServiceLoader); `ProcessExecutor` перенесён в `Service/` |
| [`done/stomp-auto-mqtt-manual-refactor.md`](done/stomp-auto-mqtt-manual-refactor.md) | Разделены STOMP-автоподключение и MQTT-подключение по кнопке (`connectStomp` / `connectMqtt` / `disconnectMqtt` в `app.js`) |

## Соглашения

- **Активный план** — документ с задачами, которые ещё не выполнены полностью.
- **Завершённый план** — переносится в `plans/done/`, в активных документах ссылки обновляются на `done/<file>`.
- **Удаление** — устаревшие или пустые файлы удаляются (например, `Implemention-status.md` был пустой заглушкой).
- При добавлении нового плана — добавить строку в таблицу «Активные документы».