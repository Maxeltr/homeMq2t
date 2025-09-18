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
clean-session = A flag indicating whether the server should delete all subscriptions and messages associated with the client upon disconnection. If set to true, the server will not retain the client's state.
auto-connect = A flag indicating whether to automatically connect to the server when the application starts.
keep-alive-timer = The time interval (in milliseconds) within which the client should send "ping" messages to the server to keep the connection alive.
wait-disconnect-while-shutdown = A time interval (in milliseconds) to wait for all disconnection operations to complete before shutting down the application.
will-topic = The topic to which the LWT message will be sent in case of client disconnection.
will-message = The message that will be sent to the will-topic if the client disconnects unexpectedly.
connect-timeout = (in milliseconds)
max-bytes-in-message = The maximum size of a message (in bytes) that can be sent or received by the client.
retransmit-delay = The time delay (in milliseconds) before retransmitting a message if no acknowledgment has been received.
reconnect = flag indicating whether to automatically attempt to reconnect to the server in case of a lost connection.
reconnect-delay = The time delay (in milliseconds) before attempting to reconnect to the server after a connection drop. This value may increase with each failed attempt.
reconnect-delay-max = The maximum time delay (in milliseconds) between reconnection attempts. 
polling-sensors-delay = The time interval (in milliseconds) for polling sensors or other data sources if the application uses them to send messages to the server.
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
component[0].subscription.topic = The MQTT topic to which the component subscribes for receiving data updates (e.g., leave blank if not applicable).
subscription qos - The Quality of Service level for the subscription, determining the message delivery guarantee (e.g., "AT_MOST_ONCE").
publication topic - The MQTT topic to which the component publishes data or updates (e.g., leave blank if not applicable).
publication qos - The Quality of Service level for the publication, determining how messages are sent to the topic (e.g., "AT_MOST_ONCE").
retain - A boolean value indicating whether the published message should be retained by the broker for future subscribers.
publication data type - The MIME type of the data being published, indicating the format of the content (e.g., "text/plain").
publication local card - The name of the local card associated with the component, which displays the data locally on the dashboard card. This functionality works without an internet connection.
provider - The Java plugin that is dynamically loaded by the Java ClassLoader and is responsible for polling the sensors or executing specific tasks. This is a plugin that implements the interfaces `Mq2tHttpPollableComponent` or `Mq2tHttpCallbackComponent`.
component[0].provider.args = The arguments or parameters required by the provider.
```

Persistence:
Persistence is not implemented nowadays. The app starts with in-memory persistence, which means all sessions and messages are lost after a server restart.

Contributing:
Feel free to contribute to the project in any way you like!
