# Архитектура приложения homeMq2t

## Обзор

**homeMq2t** — это Spring Boot приложение на базе Netty, которое реализует MQTT 3.1.1 клиент/сервер для обмена сообщениями между устройствами. Приложение предоставляет веб-интерфейс через WebSocket + STOMP, позволяет выполнять команды на хост-системе, опрашивать сенсоры через плагины и отображать данные в карточках дашборда.

---

## 1. Схема высокоуровневой архитектуры

```mermaid
flowchart TB
    subgraph "Внешний мир"
        MQTT_BROKER["MQTT Broker\nmosquitto и др."]
        BROWSER["Браузер\nWeb UI"]
        SENSORS["Внешние сенсоры\nHTTP poll/callback"]
    end

    subgraph "homeMq2t Application"
        direction TB

        subgraph "Transport Layer"
            NETTY["Netty MQTT Pipeline\nканальный уровень"]
            WS_STOMP["WebSocket STOMP\nSpring Messaging"]
        end

        subgraph "Service Layer"
            MEDIATOR["ServiceMediator\nцентральный диспетчер"]
            UI_SERVICE["UIService\nуправление UI"]
            CMD_SERVICE["CommandService\nвыполнение команд"]
            COMP_SERVICE["ComponentService\nплагины-сенсоры"]
            SUB_SERVICE["SubscriptionService\nуправление подписками"]
        end

        subgraph "UI Managers"
            CARD_MGR["DashboardItemCardManager"]
            CMD_MGR["DashboardItemCommandManager"]
            COMP_MGR["DashboardItemComponentManager"]
            MQTT_SET_MGR["DashboardItemMqttSettingManager"]
            DISPLAY_MGR["DisplayManager"]
            CONNECT_MGR["ConnectManager"]
            LOCAL_TASK_MGR["LocalTaskManager"]
            PUBLISH_MGR["MqttManager"]
        end

        subgraph "Data Layer"
            REPOS["JPA Repositories\nH2 Database"]
            ENTITIES["Entities\nCardEntity, CommandEntity\nComponentEntity, DashboardEntity\nMqttSettingsEntity, StartupTaskEntity"]
        end

        subgraph "Config Layer"
            PROP_PROVIDERS["PropertiesProviders\nCard, Command, Component\nDashboard, StartupTask"]
            APP_CONFIG["AppAnnotationConfig\nSpring @Configuration"]
            WEB_SOCKET_CFG["WebSocketConfig\nSTOMP endpoints"]
        end
    end

    MQTT_BROKER <--> NETTY
    BROWSER <--> WS_STOMP
    SENSORS -.->|HTTP| COMP_SERVICE

    NETTY --> MEDIATOR
    WS_STOMP --> UI_SERVICE
    MEDIATOR --> UI_SERVICE
    MEDIATOR --> CMD_SERVICE
    MEDIATOR --> COMP_SERVICE
    MEDIATOR --> SUB_SERVICE
    MEDIATOR --> NETTY

    UI_SERVICE --> CARD_MGR
    UI_SERVICE --> CMD_MGR
    UI_SERVICE --> COMP_MGR
    UI_SERVICE --> MQTT_SET_MGR
    UI_SERVICE --> DISPLAY_MGR
    UI_SERVICE --> CONNECT_MGR
    UI_SERVICE --> LOCAL_TASK_MGR
    UI_SERVICE --> PUBLISH_MGR

    CARD_MGR --> REPOS
    CMD_MGR --> REPOS
    COMP_MGR --> REPOS
    MQTT_SET_MGR --> REPOS
    REPOS --> ENTITIES

    PROP_PROVIDERS --> APP_CONFIG
    APP_CONFIG --> MEDIATOR
    APP_CONFIG --> NETTY
```

---

## 2. Схема MQTT Netty Pipeline

```mermaid
flowchart LR
    subgraph "Netty Channel Pipeline"
        DECODER["MqttDecoder"]
        ENCODER["MqttEncoder"]
        IDLE["IdleStateHandler\nkeepalive 20s"]
        PING["MqttPingScheduleHandler\nPINGREQ"]
        CONNECT["MqttConnectHandler\nCONNECT / CONNACK"]
        SUB["MqttSubscriptionHandler\nSUBSCRIBE / SUBACK"]
        PUBLISH["MqttPublishHandlerImpl\nPUBLISH / PUBACK"]
    end

    subgraph "Ack Mediation"
        ACK_MEDIATOR["MqttAckMediator\nPromise tracking"]
    end

    DECODER --> ENCODER
    ENCODER --> IDLE
    IDLE --> PING
    PING --> CONNECT
    CONNECT --> SUB
    SUB --> PUBLISH
    PUBLISH --> ACK_MEDIATOR
    PUBLISH --> ServiceMediator
```

---

## 3. Схема потока данных (Data Flow)

```mermaid
sequenceDiagram
    participant B as Browser Web UI
    participant WS as WebSocket STOMP
    participant UI as UIService
    participant M as ServiceMediator
    participant MQ as MQTT Netty
    participant BR as MQTT Broker
    participant CMD as CommandService
    participant COMP as ComponentService
    participant OS as Host OS

    Note over B,OS: Поток получения данных от MQTT

    BR->>MQ: Publish message
    MQ->>M: channelRead0
    M->>M: Определение ServiceType по топику
    M->>UI: display Msg
    UI->>DISPLAY_MGR: display
    DISPLAY_MGR->>UI_CONTROLLER: send to STOMP /topic
    UI_CONTROLLER->>WS: WebSocket frame
    WS->>B: Обновление карточки

    Note over B,OS: Поток отправки команды из UI

    B->>WS: STOMP /app/publish
    WS->>INPUT_CTRL: @MessageMapping
    INPUT_CTRL->>UI: publish Msg
    UI->>M: publish Msg
    M->>MQ: publish to topic
    MQ->>BR: MQTT PUBLISH

    Note over B,OS: Поток выполнения команды

    BR->>MQ: Publish to command topic
    MQ->>M: process
    M->>CMD: execute
    CMD->>OS: ProcessBuilder
    OS-->>CMD: stdout
    CMD->>M: reply
    M->>MQ: publish result
    MQ->>BR: Publish to reply topic

    Note over B,OS: Поток опроса сенсора

    COMP->>SENSOR: HTTP poll
    SENSOR-->>COMP: data
    COMP->>M: publish data
    M->>MQ: publish to component topic
```

---

## 4. Компонентная архитектура по слоям

### 4.1 Слой конфигурации (Config)

| Компонент | Назначение |
|-----------|-----------|
| [`AppAnnotationConfig`](app/src/main/java/ru/maxeltr/homeMq2t/Config/AppAnnotationConfig.java) | Главный Spring @Configuration класс. Создаёт бины: HmMq2t, ServiceMediator, UIService, CommandService, ComponentService, SubscriptionService, все DashboardItemManager'ы, пулы потоков, ObjectMapper |
| [`WebSocketConfig`](app/src/main/java/ru/maxeltr/homeMq2t/Config/WebSocketConfig.java) | Настройка STOMP WebSocket: endpoint `/mq2tClientDashboard`, simple broker `/topic`, префикс `/app` |
| [`AppProperties`](app/src/main/java/ru/maxeltr/homeMq2t/Config/AppProperties.java) | Загрузка общих настроек из application.properties |
| [`CardPropertiesProvider`](app/src/main/java/ru/maxeltr/homeMq2t/Config/CardPropertiesProvider.java) / [`Impl`](app/src/main/java/ru/maxeltr/homeMq2t/Config/CardPropertiesProviderImpl.java) | Настройки карточек: топики подписки/публикации, QoS, JSONPath, типы данных |
| [`CommandPropertiesProvider`](app/src/main/java/ru/maxeltr/homeMq2t/Config/CommandPropertiesProvider.java) / [`Impl`](app/src/main/java/ru/maxeltr/homeMq2t/Config/CommandPropertiesProviderImpl.java) | Настройки команд: путь к исполняемому файлу, аргументы, топики |
| [`ComponentPropertiesProvider`](app/src/main/java/ru/maxeltr/homeMq2t/Config/ComponentPropertiesProvider.java) / [`Impl`](app/src/main/java/ru/maxeltr/homeMq2t/Config/ComponentPropertiesProviderImpl.java) | Настройки компонентов-сенсоров: класс провайдера, аргументы, периодичность |
| [`DashboardPropertiesProvider`](app/src/main/java/ru/maxeltr/homeMq2t/Config/DashboardPropertiesProvider.java) / [`Impl`](app/src/main/java/ru/maxeltr/homeMq2t/Config/DashboardPropertiesProviderImpl.java) | Настройки дашбордов |
| [`StartupTaskPropertiesProvider`](app/src/main/java/ru/maxeltr/homeMq2t/Config/StartupTaskPropertiesProvider.java) | Задачи, выполняемые при старте приложения |
| [`ImmutableObjectMapper`](app/src/main/java/ru/maxeltr/homeMq2t/Config/ImmutableObjectMapper.java) | Jackson ObjectMapper для immutable объектов |
| [`MediaTypes`](app/src/main/java/ru/maxeltr/homeMq2t/Config/MediaTypes.java) | Константы MIME-типов |

### 4.2 Транспортный слой — MQTT over Netty

| Компонент | Назначение |
|-----------|-----------|
| [`HmMq2t`](app/src/main/java/ru/maxeltr/homeMq2t/Mqtt/HmMq2t.java) / [`HmMq2tImpl`](app/src/main/java/ru/maxeltr/homeMq2t/Mqtt/HmMq2tImpl.java) | Главный интерфейс MQTT клиента: connect, reconnect, disconnect, subscribe, unsubscribe, publish |
| [`MqttChannelInitializer`](app/src/main/java/ru/maxeltr/homeMq2t/Mqtt/MqttChannelInitializer.java) | Netty ChannelInitializer: собирает pipeline из MqttDecoder, MqttEncoder, IdleStateHandler, PingScheduleHandler, ConnectHandler, SubscriptionHandler, PublishHandler |
| [`MqttConnectHandler`](app/src/main/java/ru/maxeltr/homeMq2t/Mqtt/MqttConnectHandler.java) | Обработка CONNECT/CONNACK |
| [`MqttSubscriptionHandler`](app/src/main/java/ru/maxeltr/homeMq2t/Mqtt/MqttSubscriptionHandler.java) | Обработка SUBSCRIBE/SUBACK/UNSUBSCRIBE |
| [`MqttPublishHandlerImpl`](app/src/main/java/ru/maxeltr/homeMq2t/Mqtt/MqttPublishHandlerImpl.java) | Обработка входящих PUBLISH сообщений. Маршрутизирует в ServiceMediator по топику |
| [`MqttPingScheduleHandler`](app/src/main/java/ru/maxeltr/homeMq2t/Mqtt/MqttPingScheduleHandler.java) | Периодическая отправка PINGREQ для keepalive |
| [`MqttAckMediator`](app/src/main/java/ru/maxeltr/homeMq2t/Mqtt/MqttAckMediator.java) / [`Impl`](app/src/main/java/ru/maxeltr/homeMq2t/Mqtt/MqttAckMediatorImpl.java) | Отслеживание Promise для подтверждений (ACK) |
| [`MqttUtils`](app/src/main/java/ru/maxeltr/homeMq2t/Mqtt/MqttUtils.java) | Утилиты для работы с MQTT сообщениями |

### 4.3 Слой сервисов (Service)

#### 4.3.1 Центральный диспетчер

| Компонент | Назначение |
|-----------|-----------|
| [`ServiceMediator`](app/src/main/java/ru/maxeltr/homeMq2t/Service/ServiceMediator.java) / [`ServiceMediatorImpl`](app/src/main/java/ru/maxeltr/homeMq2t/Service/ServiceMediatorImpl.java) | **Центральный диспетчер приложения.** Принимает входящие MQTT сообщения, определяет тип сервиса по топику через `ServiceType` enum и делегирует обработку: UI (карточки), Command (команды), Component (сенсоры). Также выполняет startup-задачи при инициализации |
| [`ServiceType`](app/src/main/java/ru/maxeltr/homeMq2t/Service/ServiceType.java) | Enum с типами сервисов: `UI`, `COMMAND`, `COMPONENT`. Каждый тип содержит ссылку на метод-обработчик и метод получения номеров по топику |

#### 4.3.2 UI Service

| Компонент | Назначение |
|-----------|-----------|
| [`UIService`](app/src/main/java/ru/maxeltr/homeMq2t/Service/UI/UIService.java) / [`UIServiceImpl`](app/src/main/java/ru/maxeltr/homeMq2t/Service/UI/UIServiceImpl.java) | Оркестрирует все UI-операции: connect/disconnect, отображение дашбордов, карточек, команд, компонентов, настроек MQTT, публикация сообщений, запуск локальных задач |
| [`ConnectManager`](app/src/main/java/ru/maxeltr/homeMq2t/Service/UI/ConnectManager.java) / [`Impl`](app/src/main/java/ru/maxeltr/homeMq2t/Service/UI/ConnectManagerImpl.java) | Управление подключением к MQTT брокеру |
| [`DisplayManager`](app/src/main/java/ru/maxeltr/homeMq2t/Service/UI/DisplayManager.java) / [`DisplayManagerImpl`](app/src/main/java/ru/maxeltr/homeMq2t/Service/UI/DisplayManagerImpl.java) | Форматирование данных для отображения: применение JSONPath, санитизация HTML, установка MIME-типа |
| [`DashboardItemManager`](app/src/main/java/ru/maxeltr/homeMq2t/Service/UI/DashboardItemManager.java) | Интерфейс для менеджеров элементов дашборда |
| [`DashboardItemCardManagerImpl`](app/src/main/java/ru/maxeltr/homeMq2t/Service/UI/DashboardItemCardManagerImpl.java) | Управление карточками на дашборде |
| [`DashboardItemCommandManagerImpl`](app/src/main/java/ru/maxeltr/homeMq2t/Service/UI/DashboardItemCommandManagerImpl.java) | Управление командами на дашборде |
| [`DashboardItemComponentManagerImpl`](app/src/main/java/ru/maxeltr/homeMq2t/Service/UI/DashboardItemComponentManagerImpl.java) | Управление компонентами на дашборде |
| [`DashboardItemMqttSettingManagerImpl`](app/src/main/java/ru/maxeltr/homeMq2t/Service/UI/DashboardItemMqttSettingManagerImpl.java) | Управление настройками MQTT на дашборде |
| [`MqttManager`](app/src/main/java/ru/maxeltr/homeMq2t/Service/UI/MqttManager.java) / [`MqttManagerImpl`](app/src/main/java/ru/maxeltr/homeMq2t/Service/UI/MqttManagerImpl.java) | Публикация сообщений в MQTT из UI |
| [`LocalTaskManager`](app/src/main/java/ru/maxeltr/homeMq2t/Service/UI/LocalTaskManager.java) / [`LocalTaskManagerImpl`](app/src/main/java/ru/maxeltr/homeMq2t/Service/UI/LocalTaskManagerImpl.java) | Запуск локальных задач/скриптов из карточки |
| [`UIJsonFormatter`](app/src/main/java/ru/maxeltr/homeMq2t/Service/UI/UIJsonFormatter.java) / [`Base64HtmlJsonFormatterImpl`](app/src/main/java/ru/maxeltr/homeMq2t/Service/UI/Base64HtmlJsonFormatterImpl.java) | Форматирование JSON для отображения, поддержка Base64 |
| [`HtmlSanitizer`](app/src/main/java/ru/maxeltr/homeMq2t/Service/UI/HtmlSanitizer.java) / [`HtmlSanitizerImpl`](app/src/main/java/ru/maxeltr/homeMq2t/Service/UI/HtmlSanitizerImpl.java) | Санитизация HTML через Jsoup для защиты XSS |

#### 4.3.3 Command Service

| Компонент | Назначение |
|-----------|-----------|
| [`CommandService`](app/src/main/java/ru/maxeltr/homeMq2t/Service/Command/CommandService.java) / [`CommandServiceImpl`](app/src/main/java/ru/maxeltr/homeMq2t/Service/Command/CommandServiceImpl.java) | Асинхронное выполнение команд на хост-системе. Получает команду из MQTT, парсит, выполняет через ProcessExecutor, отправляет результат через ReplySender |
| [`CommandParser`](app/src/main/java/ru/maxeltr/homeMq2t/Service/Command/CommandParser.java) / [`CommandParserImpl`](app/src/main/java/ru/maxeltr/homeMq2t/Service/Command/CommandParserImpl.java) | Парсинг входящего MQTT сообщения для извлечения имени команды |
| [`ProcessExecutor`](app/src/main/java/ru/maxeltr/homeMq2t/Service/Command/ProcessExecutor.java) / [`ProcessExecutorImpl`](app/src/main/java/ru/maxeltr/homeMq2t/Service/Command/ProcessExecutorImpl.java) | Запуск внешних процессов через `ProcessBuilder`, захват stdout |
| [`ReplySender`](app/src/main/java/ru/maxeltr/homeMq2t/Service/Command/ReplySender.java) / [`ReplySenderImpl`](app/src/main/java/ru/maxeltr/homeMq2t/Service/Command/ReplySenderImpl.java) | Отправка результата выполнения команды в MQTT топик |

#### 4.3.4 Component Service

| Компонент | Назначение |
|-----------|-----------|
| [`ComponentService`](app/src/main/java/ru/maxeltr/homeMq2t/Service/ComponentService.java) / [`ComponentServiceImpl`](app/src/main/java/ru/maxeltr/homeMq2t/Service/ComponentServiceImpl.java) | Управление плагинными компонентами (сенсорами). Загружает `Mq2tComponent` через ServiceLoader, инициализирует pollable и callback компоненты, публикует их данные в MQTT |

#### 4.3.5 Subscription Service

| Компонент | Назначение |
|-----------|-----------|
| [`SubscriptionService`](app/src/main/java/ru/maxeltr/homeMq2t/Service/SubscriptionService.java) / [`SubscriptionServiceImpl`](app/src/main/java/ru/maxeltr/homeMq2t/Service/SubscriptionServiceImpl.java) | Управление MQTT подписками. Подписывается на топики из конфигурации карточек, команд и компонентов. Отслеживает статус подписок, переподключается при необходимости |

### 4.4 Слой контроллеров (Controller)

| Компонент | Назначение |
|-----------|-----------|
| [`InputUIController`](app/src/main/java/ru/maxeltr/homeMq2t/Controller/InputUIController.java) / [`InputUIControllerImpl`](app/src/main/java/ru/maxeltr/homeMq2t/Controller/InputUIControllerImpl.java) | Spring @Controller. Принимает STOMP сообщения от браузера: connect, disconnect, publish, get/save настроек карточек, команд, компонентов, MQTT |
| [`OutputUIController`](app/src/main/java/ru/maxeltr/homeMq2t/Controller/OutputUIController.java) / [`OutputUIControllerImpl`](app/src/main/java/ru/maxeltr/homeMq2t/Controller/OutputUIControllerImpl.java) | Отправка данных в браузер через STOMP `/topic` |

### 4.5 Слой модели (Model)

| Компонент | Назначение |
|-----------|-----------|
| [`Msg`](app/src/main/java/ru/maxeltr/homeMq2t/Model/Msg.java) / [`MsgImpl`](app/src/main/java/ru/maxeltr/homeMq2t/Model/MsgImpl.java) | Иммутабельное сообщение с полями: id, type, data, timestamp. Использует Builder pattern |
| [`Dashboard`](app/src/main/java/ru/maxeltr/homeMq2t/Model/Dashboard.java) / [`DashboardImpl`](app/src/main/java/ru/maxeltr/homeMq2t/Model/DashboardImpl.java) | Модель дашборда: номер, имя, HTML, список ViewModel |
| [`DashboardType`](app/src/main/java/ru/maxeltr/homeMq2t/Model/DashboardType.java) | Enum типов дашборда |
| [`ViewModel`](app/src/main/java/ru/maxeltr/homeMq2t/Model/ViewModel.java) | Абстрактная модель представления для элементов дашборда. Загружает HTML из файлов Static/ |
| [`CardImpl`](app/src/main/java/ru/maxeltr/homeMq2t/Model/CardImpl.java) | ViewModel для карточки |
| [`CommandImpl`](app/src/main/java/ru/maxeltr/homeMq2t/Model/CommandImpl.java) | ViewModel для команды |
| [`ComponentImpl`](app/src/main/java/ru/maxeltr/homeMq2t/Model/ComponentImpl.java) | ViewModel для компонента |
| [`MqttSettingsImpl`](app/src/main/java/ru/maxeltr/homeMq2t/Model/MqttSettingsImpl.java) | ViewModel для настроек MQTT |
| [`CardSettingsImpl`](app/src/main/java/ru/maxeltr/homeMq2t/Model/CardSettingsImpl.java) | ViewModel для настроек карточки |
| [`CommandSettingsImpl`](app/src/main/java/ru/maxeltr/homeMq2t/Model/CommandSettingsImpl.java) | ViewModel для настроек команды |
| [`ComponentSettingsImpl`](app/src/main/java/ru/maxeltr/homeMq2t/Model/ComponentSettingsImpl.java) | ViewModel для настроек компонента |
| [`Status`](app/src/main/java/ru/maxeltr/homeMq2t/Model/Status.java) | Enum статусов |

### 4.6 Слой сущностей БД (Entity)

| Компонент | Назначение |
|-----------|-----------|
| [`BaseEntity`](app/src/main/java/ru/maxeltr/homeMq2t/Entity/BaseEntity.java) | Абстрактный базовый класс: name, number |
| [`DashboardEntity`](app/src/main/java/ru/maxeltr/homeMq2t/Entity/DashboardEntity.java) | JPA Entity для таблицы `dashboard_settings`. Содержит тип дашборда и список элементов (CardEntity) |
| [`CardEntity`](app/src/main/java/ru/maxeltr/homeMq2t/Entity/CardEntity.java) | JPA Entity для карточки: топики, QoS, JSONPath, retain, локальная задача |
| [`CommandEntity`](app/src/main/java/ru/maxeltr/homeMq2t/Entity/CommandEntity.java) | JPA Entity для команды: путь, аргументы, топики |
| [`ComponentEntity`](app/src/main/java/ru/maxeltr/homeMq2t/Entity/ComponentEntity.java) | JPA Entity для компонента: класс провайдера, аргументы, периодичность |
| [`MqttSettingsEntity`](app/src/main/java/ru/maxeltr/homeMq2t/Entity/MqttSettingsEntity.java) | JPA Entity для настроек MQTT: host, port, username, password, client-id, will, clean-session |
| [`StartupTaskEntity`](app/src/main/java/ru/maxeltr/homeMq2t/Entity/StartupTaskEntity.java) | JPA Entity для задач автозапуска |
| [`HasSubscription`](app/src/main/java/ru/maxeltr/homeMq2t/Entity/HasSubscription.java) | Интерфейс для сущностей, имеющих MQTT подписку |

### 4.7 Слой репозиториев (Repository)

| Компонент | Назначение |
|-----------|-----------|
| [`CardRepository`](app/src/main/java/ru/maxeltr/homeMq2t/Repository/CardRepository.java) | JPA Repository для CardEntity |
| [`CommandRepository`](app/src/main/java/ru/maxeltr/homeMq2t/Repository/CommandRepository.java) | JPA Repository для CommandEntity |
| [`ComponentRepository`](app/src/main/java/ru/maxeltr/homeMq2t/Repository/ComponentRepository.java) | JPA Repository для ComponentEntity |
| [`DashboardRepository`](app/src/main/java/ru/maxeltr/homeMq2t/Repository/DashboardRepository.java) | JPA Repository для DashboardEntity |
| [`MqttSettingsRepository`](app/src/main/java/ru/maxeltr/homeMq2t/Repository/MqttSettingsRepository.java) | JPA Repository для MqttSettingsEntity |
| [`StartupTaskRepository`](app/src/main/java/ru/maxeltr/homeMq2t/Repository/StartupTaskRepository.java) | JPA Repository для StartupTaskEntity |

### 4.8 WebSocket слой

| Компонент | Назначение |
|-----------|-----------|
| [`Mq2tSubProtocolWebSocketHandler`](app/src/main/java/ru/maxeltr/homeMq2t/Websocket/Mq2tSubProtocolWebSocketHandler.java) | Расширение SubProtocolWebSocketHandler. Регистрирует WebSocket сессии в SessionHandler |
| [`SessionHandler`](app/src/main/java/ru/maxeltr/homeMq2t/Websocket/SessionHandler.java) | Управление WebSocket сессиями |

### 4.9 Статические ресурсы (Frontend)

| Файл | Назначение |
|------|-----------|
| [`index.html`](app/src/main/resources/Static/index.html) | Главная страница |
| [`dashboard.html`](app/src/main/resources/Static/dashboard.html) | Шаблон дашборда |
| [`card.html`](app/src/main/resources/Static/card.html) | Шаблон карточки |
| [`command.html`](app/src/main/resources/Static/command.html) | Шаблон команды |
| [`component.html`](app/src/main/resources/Static/component.html) | Шаблон компонента |
| [`cardSettings.html`](app/src/main/resources/Static/cardSettings.html) | Шаблон настроек карточки |
| [`commandSettings.html`](app/src/main/resources/Static/commandSettings.html) | Шаблон настроек команды |
| [`componentSettings.html`](app/src/main/resources/Static/componentSettings.html) | Шаблон настроек компонента |
| [`mqttSettings.html`](app/src/main/resources/Static/mqttSettings.html) | Шаблон настроек MQTT |
| [`app.js`](app/src/main/resources/Static/app.js) | Клиентский JavaScript: STOMP подключение, обработка событий |
| [`main.css`](app/src/main/resources/Static/main.css) | Стили |

### 4.10 Конфигурационные файлы

| Файл | Назначение |
|------|-----------|
| [`application.properties`](app/src/main/resources/application.properties) | Основные настройки приложения |
| [`schema.sql`](app/src/main/resources/schema.sql) | DDL для H2 database |
| [`strings.properties`](app/src/main/resources/strings.properties) | Локализация строк |

---

## 5. Схема базы данных

```mermaid
erDiagram
    dashboard_settings {
        Long id PK
        String name
        Integer number
        String type
    }

    card_settings {
        Long id PK
        String name
        Integer number
        String sub_topic
        String sub_qos
        String sub_data_name
        String sub_data_type
        String display_data_jsonpath
        String pub_topic
        String pub_qos
        Boolean retain
        String pub_data
        String pub_data_type
        String local_task_path
        String local_task_arguments
        String local_task_data_type
        Long dashboard_id FK
    }

    command_settings {
        Long id PK
        String name
        Integer number
        String sub_topic
        String sub_qos
        String pub_topic
        String pub_qos
        Boolean retain
        String pub_data_type
        String path
        String arguments
    }

    component_settings {
        Long id PK
        String name
        Integer number
        String sub_topic
        String sub_qos
        String pub_topic
        String pub_qos
        Boolean retain
        String provider_class
        String provider_args
        Long polling_interval
    }

    mqtt_settings {
        Long id PK
        String host
        Integer port
        String username
        String password
        String client_id
        Boolean has_user_name
        Boolean has_password
        Integer will_qos
        Boolean will_retain
        Boolean will_flag
        Boolean clean_session
        Boolean auto_connect
        String will_topic
        String will_message
        Boolean reconnect
    }

    startup_task_settings {
        Long id PK
        String name
        String path
        String arguments
    }

    dashboard_settings ||--o{ card_settings : contains
```

---

## 6. Ключевые архитектурные решения

1. **Netty для MQTT** — полный контроль над MQTT протоколом на уровне каналов Netty, без использования сторонних MQTT библиотек
2. **ServiceMediator как оркестратор** — все входящие MQTT сообщения проходят через единый диспетчер, который определяет тип сервиса по топику
3. **Плагинная архитектура сенсоров** — компоненты загружаются через `ServiceLoader` из внешних JAR, реализуя интерфейсы `Mq2tPollableComponent` / `Mq2tCallbackComponent`
4. **Immutable сообщения** — `Msg` использует Builder pattern для иммутабельной передачи данных
5. **Хранение в H2** — встроенная БД для настроек карточек, команд, компонентов и MQTT
6. **Web UI через STOMP** — реальное время через WebSocket с Simple Broker
7. **Асинхронное выполнение команд** — `@Async("processExecutor")` для неблокирующего запуска внешних процессов
8. **Санитизация HTML** — защита от XSS через Jsoup/DOMPurify

---

## 7. Поток сообщений (Message Flow)

```mermaid
flowchart TB
    subgraph "Входящее MQTT сообщение"
        A["MQTT PUBLISH\nот брокера"]
    end

    subgraph "ServiceMediator обработка"
        B["MqttPublishHandlerImpl\nchannelRead0"]
        C["Определение ServiceType\nпо топику"]
    end

    subgraph "Маршрутизация"
        D["UI тип"]
        E["COMMAND тип"]
        F["COMPONENT тип"]
    end

    subgraph "Обработка"
        G["UIService.display\n→ DisplayManager\n→ OutputUIController\n→ STOMP /topic"]
        H["CommandService.execute\n→ ProcessExecutor\n→ ReplySender\n→ MQTT publish"]
        I["ComponentService.process\n→ Mqtt2tComponent\n→ MQTT publish"]
    end

    A --> B
    B --> C
    C --> D
    C --> E
    C --> F
    D --> G
    E --> H
    F --> I
```

---

## 8. Диаграмма зависимостей (Gradle)

```
homeMq2t (root)
├── app (Spring Boot приложение)
│   ├── spring-boot-starter
│   ├── spring-boot-starter-websocket
│   ├── spring-boot-starter-data-jpa
│   ├── netty-all (4.2.6.Final)
│   ├── h2database (H2)
│   ├── jackson (ObjectMapper)
│   ├── json-path (Jayway)
│   ├── jsoup (HTML sanitizer)
│   ├── commons-lang3
│   ├── classgraph
│   ├── webjars: bootstrap, jquery, bootstrap-icons, sockjs, stomp, dompurify
│   └── mq2tLib (внешний модуль)
└── mq2tLib (../mq2tLib/lib) — библиотека с интерфейсами Mq2tComponent