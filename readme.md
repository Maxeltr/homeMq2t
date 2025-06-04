# Spring Boot Application Based on Netty

This application allows for the subscription and publication of messages to various topics, enabling communication between devices and services. It facilitates sending commands over MQTT to control the host system.

## Features

- Support for MQTT 3.1.1 protocol.
- Support subscribing, publishing, authentication, will messages, keep alive pings and all 3 QoS levels.
- Web UI built on WebSocket and STOMP.
- Data is transmitted in JSON format (including fields: `data`, `type`, and `timestamp`) via MQTT.
- Images must be encoded in Base64 due to JSON constraints.
- Executes commands (scripts) on the host it's running on.
- Executes processes and publishes stdout of the launched program to the configured topic.
- Polling sensors (they should implement `Mq2tHttpPollableComponent` or `Mq2tHttpCallbackComponent`).
- The application displays data from topics in cards, with each card representing a single topic.
- From each card, you can send a message to the configured topic.
- The number of cards is determined by the configuration settings.

## MQTT Settings Description
```properties
host = 
port = 
mq2t-password = 
mq2t-username = 
client-id = 
has-user-name = 
has-password = 
will-qos = 
will-retain = 
will-flag = 
clean-session = 
auto-connect = 
keep-alive-timer = 
wait-disconnect-while-shutdown = 
will-topic =
will-message =
connect-timeout = 
max-bytes-in-message = 
retransmit-delay = 
reconnect = 
reconnect-delay = 
reconnect-delay-max = 
polling-sensors-delay = 
```

## Card Settings Description

```properties
card[0].name = The display name for the card, representing the specific sensor or device
card[0].subscription.topic = The MQTT topic to which the card subscribes for receiving data
card[0].subscription.qos = The Quality of Service level for the subscription, determining the message delivery guarantee (e.g., "AT_MOST_ONCE").
card[0].subscription.data.name = The name of the data being received from the subscription, providing context for the data (not necessary)
card[0].subscription.data.type = The MIME type of the data being received, indicating the format of the content (e.g., "image/jpeg;base64").
card[0].display.data.jsonpath = The JSON path used to extract specific data from the received JSON payload for display purposes.
card[0].publication.topic = The MQTT topic to which the card publishes messages 
card[0].publication.qos = The Quality of Service level for the publication, determining how messages are sent to the topic (e.g., "AT_MOST_ONCE").
card[0].publication.retain = A boolean value indicating whether the published message should be retained by the broker for future subscribers.
card[0].publication.data = The actual data being published to the topic, which can include status updates, commands, or other relevant information.
card[0].publication.data.type = The MIME type of the data being published, indicating the format of the content (e.g., "text/plain").
card[0].local.task.path = The file path to a local task or script that can be executed in conjunction with the card functionality.
card[0].local.task.arguments = The arguments to be passed to the local task or script when it is executed.
card[0].local.task.data.type = The MIME type of the data that the local task will output to stdout and will be displayed in the local card.
```

## Command Settings Description

```properties
command[0].name = The name of the command, representing the specific action to be executed.
command[0].subscription.topic = The MQTT topic to which the command subscribes for receiving execution requests.
command[0].subscription.qos = The Quality of Service level for the subscription, determining the message delivery guarantee (e.g., "AT_MOST_ONCE").
command[0].publication.topic = The MQTT topic to which the command publishes replies or results after execution.
command[0].publication.qos = The Quality of Service level for the publication, determining how messages are sent to the topic (e.g., "AT_MOST_ONCE").
command[0].publication.retain = A boolean value indicating whether the published message should be retained by the broker for future subscribers.
command[0].publication.data.type = The MIME type of the data being published, indicating the format of the content (e.g., "text/plain").
command[0].path = The name of the command, file, or script to be executed (e.g., "java").
command[0].arguments = The arguments to be passed to the command via the command line when it is executed (e.g., "-version").
```

## Component Settings Description

```properties
component[0].name = The name of the component, representing the specific functionality
component[0].subscription.topic = The MQTT topic to which the component subscribes for receiving data updates (e.g., leave blank if not applicable).
component[0].subscription.qos = The Quality of Service level for the subscription, determining the message delivery guarantee (e.g., "AT_MOST_ONCE").
component[0].publication.topic = The MQTT topic to which the component publishes data or updates (e.g., leave blank if not applicable).
component[0].publication.qos = The Quality of Service level for the publication, determining how messages are sent to the topic (e.g., "AT_MOST_ONCE").
component[0].publication.retain = A boolean value indicating whether the published message should be retained by the broker for future subscribers.
component[0].publication.data.type = The MIME type of the data being published, indicating the format of the content (e.g., "text/plain").
component[0].publication.local.card = The name of the local card associated with the component, which displays the data locally on the dashboard card. This functionality works without an internet connection.
component[0].provider = The Java plugin that is dynamically loaded by the Java ClassLoader and is responsible for polling the sensors or executing specific tasks. This is a plugin that implements the interfaces `Mq2tHttpPollableComponent` or `Mq2tHttpCallbackComponent`.
component[0].provider.args = The arguments or parameters required by the provider.
```

Persistence:
Persistence is not implemented nowadays. The app starts with in-memory persistence, which means all sessions and messages are lost after a server restart.

Contributing:
Feel free to contribute to the project in any way you like!
