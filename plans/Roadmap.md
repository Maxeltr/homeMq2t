14. **Использование Строк-Шаблонов (%VALUE%)** - пересылка данных настраивается через маску payloadTemplate (например, {"status": "%VALUE%"}). Веб-панель может управлять любым IOT устройством без изменения java кода
15. **Планировщик CronTrigger** - запуск скриптов, опрос датчиков и т.п. по расписанию с использованием маски звездочек CronTrigger
, хранения неподтвержденных MQTT сообщений, хранения графиков (например, температуры и т.п.)
**Повторная передача неподтвержденных сообщений** - `RetransmitTask` периодически проверяет и повторно отправляет неподтвержденные NQTT сообщения с помощью ThreadPoolTaskScheduler, PeriodicTrigger
**Поддержка TLS** - защищенное подключение к брокеру
**Таблица триггеров** - для выполнения команд и заданий
**Поддержка mqtt wildcards** - для сокращения количества правил
Base64HtmlJsonFormatterImpl разделить на json и html переназвать JsonFormatter на jsonpathutil
для карточек ввести тип uiType
файлы БД и логи перенести в userHome
внешний фреймворк для контролов 
заменить билдер на рекордз
перенастроить спринг на virtualThreads
если на топик не удалось подписаться или был понижен qos то сигнализировать? корректировать список подписок? 
в SubscriptionService Promise<MqttSubAckMessage> promise = mediator.subscribe(prepared); promise.awaitUninterruptibly(this.connectTimeout); если вышел таймаут то устанавливать promise в failure
в entity? или в subscriptionservice у подписок сделать свойство сигнализирующее о понижении qos брокером
завести entity Subcriptions в которое записвыать актуальный qos после подписки. в карточках харнить ссылку на entity. также entity Pubkications. создать таблицы Subcriptions и Pubkications
в карточках переименовать subscriptionDataName subscriptionDataType publicationData и т.д. в отображаемые даты или типа того
переделать getNewMessageId с учетом многопоточночти id = this.nextMessageId.updateAndGet(current >= 0xffff) ? 1 : current + 1);
HmMq2tImpl в методе subscribe добавить проверку isConnected
после коннект перед подпиской проверять sessionPresent если есть сессия то subscribe не вызывать