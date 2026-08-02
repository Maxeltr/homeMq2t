# Spring Boot Application Based on Netty

This application allows for the subscription and publication of messages to various topics, enabling communication between devices and services. It facilitates sending commands over MQTT to control the host system.

## Features

- Support for MQTT 3.1.1 protocol.
- Netty for scalable non-blocking network I/O.
- Support subscribing, publishing, authentication, will messages, keep alive pings and all 3 QoS levels.
- Web UI built on WebSocket and STOMP.
- Data is transmitted in JSON format (including fields: `data`, `type`, and `timestamp`) via MQTT.
- Images must be encoded in Base64 due to JSON constraints.
- Executes commands (scripts) on the host it's running on.
- Executes processes and publishes stdout of the launched program to the configured topic.
- Extensible via Java providers loaded dynamically by ClassLoader.
- Polling sensors (they should implement `Mq2tHttpPollableComponent` or `Mq2tHttpCallbackComponent`).
- The application displays data from topics in cards, with each card representing a single topic.
- From each card, you can send a message to the configured topic.
- The number of cards is determined by the configuration settings.

## Architecture
The app is built as a Spring Boot service that uses Netty for network I/O. MQTT client/server interactions are handled inside the Netty pipeline and integrated with application services. The Web UI communicates using WebSocket + STOMP to provide real-time updates and controls. Components (sensors, pollers, local tasks) are implemented as pluggable Java providers.

## Cards & Commands
Card: visual unit mapping to one MQTT topic for display and interactions. Cards extract data via JSONPath and can publish configured payloads or arbitrary JSON messages from UI.
Command: subscribes for execution requests, runs configured local executable/script, publishes stdout (and optionally exit status) to a configured MQTT topic.

## Extending with plugins
Implement provider interfaces: Mq2tHttpPollableComponent or Mq2tHttpCallbackComponent.
Package providers as jars and place them on the application classpath or configured plugin directory.
Provide provider class name and args in components configuration; the app loads them via ClassLoader.

## Security & best practices
Use TLS for MQTT/WebSocket in production and strong credentials.
Ensure executed scripts are trusted and run with appropriate user permissions.
Limit plugin directories and validate provider classes to reduce risk.



## MQTT Settings Description
```properties
host = The address of the MQTT server to which the client will connect. This can be an IP address or a domain name.
port = The port used to connect to the MQTT server.
mq2t-password = The password for authenticating the client on the MQTT server.
mq2t-username = The username for authenticating the client on the MQTT server.
client-id = A unique identifier for the client, used to identify the connection on the server. It should be unique for each client.
has-user-name = A flag indicating whether a username is required for connecting to the server. If set to true, the mq2t-username must be provided.
has-password = A flag indicating whether a password is required for connecting to the server. If set to true, the mq2t-password must be provided.
will-qos = The Quality of Service (QoS) level for the "Last Will and Testament" (LWT) message. It determines how the server should handle this message in case of an unexpected client disconnection.
will-retain = A flag indicating whether the LWT message should be retained on the server for new subscribers.
will-flag = A flag indicating whether the last will message should be sent when the client disconnects.
clean-session = A flag indicating whether the server should delete all subscriptions and messages associated with the client upon disconnection. If set to true, the server will not retain the client state.
auto-connect = A flag indicating whether to automatically connect to the server when the application starts.
will-topic = The topic to which the LWT message will be sent in case of client disconnection.
will-message = The message that will be sent to the will-topic if the client disconnects unexpectedly.
reconnect = flag indicating whether to automatically attempt to reconnect to the server in case of a lost connection.
```

## Card Settings Description

```properties
name - The display name for the card, representing the specific sensor or device
subscription topic - The MQTT topic to which the card subscribes for receiving data
subscription qos - The Quality of Service level for the subscription, determining the message delivery guarantee (e.g., "AT_MOST_ONCE").
subscription data name - The name of the data being received from the subscription, providing context for the data (not necessary)
subscription data type - The MIME type of the data being received, indicating the format of the content (e.g., "image/jpeg;base64").
display data jsonpath - The JSON path used to extract specific data from the received JSON payload for display purposes.
publication topic - The MQTT topic to which the card publishes messages 
publication qos - The Quality of Service level for the publication, determining how messages are sent to the topic (e.g., "AT_MOST_ONCE").
retain - A boolean value indicating whether the published message should be retained by the broker for future subscribers.
publication data - The actual data being published to the topic, which can include status updates, commands, or other relevant information.
publication data type - The MIME type of the data being published, indicating the format of the content (e.g., "text/plain").
local task path - The file path to a local task or script that can be executed in conjunction with the card functionality.
local task arguments - The arguments to be passed to the local task or script when it is executed.
local task data type - The MIME type of the data that the local task will output to stdout and will be displayed in the local card.
```

## Command Settings Description

```properties
name - The name of the command, representing the specific action to be executed.
subscription topic - The MQTT topic to which the command subscribes for receiving execution requests.
subscription qos - The Quality of Service level for the subscription, determining the message delivery guarantee (e.g., "AT_MOST_ONCE").
publication topic - The MQTT topic to which the command publishes replies or results after execution.
publication qos - The Quality of Service level for the publication, determining how messages are sent to the topic (e.g., "AT_MOST_ONCE").
retain - A boolean value indicating whether the published message should be retained by the broker for future subscribers.
publication data type - The MIME type of the data being published, indicating the format of the content (e.g., "text/plain").
path - The name of the command, file, or script to be executed (e.g., "java").
arguments - The arguments to be passed to the command via the command line when it is executed (e.g., "-version").
```

## Component Settings Description

```properties
name - The name of the component, representing the specific functionality
subscription topic - The MQTT topic to which the component subscribes for receiving data updates (e.g., leave blank if not applicable).
subscription qos - The Quality of Service level for the subscription, determining the message delivery guarantee (e.g., "AT_MOST_ONCE").
publication topic - The MQTT topic to which the component publishes data or updates (e.g., leave blank if not applicable).
publication qos - The Quality of Service level for the publication, determining how messages are sent to the topic (e.g., "AT_MOST_ONCE").
retain - A boolean value indicating whether the published message should be retained by the broker for future subscribers.
publication data type - The MIME type of the data being published, indicating the format of the content (e.g., "text/plain").
publication local card - The name of the local card associated with the component, which displays the data locally on the dashboard card. This functionality works without an internet connection.
provider - The Java plugin that is dynamically loaded by the Java ClassLoader and is responsible for polling the sensors or executing specific tasks. This is a plugin that implements the interfaces `Mq2tHttpPollableComponent` or `Mq2tHttpCallbackComponent`.
provider args - The arguments or parameters required by the provider.
```

Persistence:
Persistence is not implemented nowadays. The app starts with in-memory persistence, which means all sessions and messages are lost after a server restart.

Contributing:
Feel free to contribute to the project in any way you like!


# 🏠 homeMq2t (MQTT Server on Netty & Spring Boot 3.5.6)

Инструкция по быстрому развертыванию проекта на новом компьютере (Windows 11). Следуйте этим шагам, чтобы избежать проблем с кодировками, версиями Java, путями модулей и падениями в JAR.

---

## 🛠 Шаг 1. Глобальная настройка Windows (КРИТИЧЕСКИ ВАЖНО)
По умолчанию Windows использует кодировку `CP1251`, из-за чего ломаются утилиты Git, SSH и компилятор Java при наличии кириллицы в путях (например, `C:\Users\Общий`).

1. Нажмите `Win + R`, введите `control` и откройте **Панель управления**.
2. Перейдите в **Региональные стандарты (Region)** -> вкладка **Дополнительно (Administrative)**.
3. Нажмите кнопку **Изменить язык системы (Change system locale)**.
4. **Обязательно поставьте галочку:** `Beta: Использовать Unicode (UTF-8) для поддержки языка во всем мире`.
5. Перезагрузите компьютер.

---

## 🔑 Шаг 2. Настройка SSH и привязка к GitHub
Проект использует авторизацию по SSH. Не используйте кастомные папки вроде `C:\ssh_keys`, делайте строго по умолчанию:

1. Откройте терминал и создайте чистый стандартный ключ (на все вопросы нажимайте `Enter`):
   ```bash
   ssh-keygen -t ed25519 -C "ваш_email@example.com"
   ```
2. Скопируйте ключ в буфер обмена Windows:
   ```powershell
   cat ~/.ssh/id_ed25519.pub | clip
   ```
3. Добавьте его в свой профиль на [://github.com](https://://github.com).
4. Проверьте связь в терминале: `ssh -T git@github.com`. Должно появиться приветствие `Hi Maxeltr!`.

---

## ☕ Шаг 3. Требования к окружению (Java 25 & Gradle 9.6.1)
Проект использует передовые фичи и требует строго определенных версий инструментов:

*   **Java SDK:** Требуется **Java 25 (LTS)**. Рекомендуется дистрибутив **Azul Zulu OpenJDK 25**, так как он юридически чист для коммерции и не содержит скрытой телеметрии.
    *   *Важно:* При установке `.msi` обязательно включите галочку `Set JAVA_HOME environment variable`.
*   **Gradle:** Используется **Gradle 9.6.1**. Фоновое скачивание и совместимость с Java 25 настроены через файл `gradle/wrapper/gradle-wrapper.properties`.

---

## 📂 Шаг 4. Структура папок и Клонирование
Проект является многомодульным. Корневой проект приложения (`homeMq2t`) и проект общей библиотеки (`mq2tLib`) должны лежать **в одной общей директории** на одном уровне:

```text
C:\Projects\
    ├── homeMq2t\       <-- Основной репозиторий (этот проект)
    └── mq2tLib\        <-- Репозиторий библиотеки
```

1. Склонируйте оба репозитория по SSH в папку `C:\Projects\`:
   ```bash
   git clone git@github.com:Maxeltr/homeMq2t.git
   git clone git@github.com:Maxeltr/mq2tLib.git
   ```
2. В файле `homeMq2t/settings.gradle` путь к библиотеке должен указывать строго на соседнюю папку:
   ```groovy
   project(':mq2tLib').projectDir = file('../mq2tLib')
   ```
3. Откройте в VS Code папку `C:\Projects\homeMq2t`.

---

## 🚀 Шаг 5. Первичная настройка Git и Сборка

1. Укажите Git свои данные (чтобы коммиты корректно привязывались к вашему профилю `Maxeltr`):
   ```bash
   git config --global user.name "Maksim"
   git config --global user.email "ваша_почта@example.com"
   ```
2. Выполните сборку проекта через графический интерфейс VS Code (вкладка со **слоником Gradle** -> задача **`app:assemble`**) или через встроенный терминал:
   ```bash
   ./gradlew assemble
   ```
3. Запустите MQTT-сервер:
   ```bash
   ./gradlew bootRun
   ```

---

## ⚠️ Шаг 6. Правила работы с ресурсами (Шаблоны HTML)
Чтобы проект не падал при сборке и запуске из готового `.jar` архива, **запрещено** использовать файловые пути через `System.getProperty("user.dir")` и метод `.getFile()`. 

*   Все внешние HTML-шаблоны карточек и дашбордов должны загружаться как **потоки данных (Stream)** через класс Spring `ClassPathResource`.
*   В конфигурационных файлах пути к ресурсам должны указываться относительно корня папки `resources`, например: `card-settings-template-path = /Static/cardSettings.html` (без префикса `/src/main/resources`).

---

## 🤖 Шаг 7. Использование ИИ-агента Yandex SourceCraft
В проект встроен ИИ-агент для автоматического написания кода в режиме максимальной экономии токенов.

*   В корне проекта в папке `plans/architecture.md` зафиксированы схемы **UML Mermaid**. Это «карта памяти» для ИИ. Не удаляйте её, чтобы агент не сканировал весь проект заново.
*   Для общения с ИИ используйте **левую панель (режим Architect -> Agent)**. Правая панель VS Code заблокирована под платный GitHub Copilot и не используется.
*   При формировании запросов вставляйте символ `@`, чтобы передавать ИИ только конкретные Java-классы (например, `@ViewModel.java`), а не весь проект целиком — это экономит тысячи токенов.
