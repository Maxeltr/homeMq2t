Spring Boot application based on Netty. It allows for the subscription and publication of messages to various topics, enabling communication between devices and services. This application allows you to send commands over MQTT to control the host system.

## Features
Support of mqtt3.1.1 protocol.
Web UI built on websocket and STOMP.
Data is transmitted in JSON format (including fields: data, type, and timestamp) via MQTT.
Images must be encoded in Base64 due to JSON constraints.
Executes commands (scripts) on the host it's running on.
Executes processes and publishes stdout of the launched program to the configured topic.
Polling sensors (They should implement Mq2tHttpPollableComponent or Mq2tHttpCallbackComponent).
The application displays data from topics in cards, with each card representing a single topic.
From each card, you can send a message to the configured topic.
The number of cards is determined by the configuration settings.

Card Settings Description
card[0].name = The display name for the card, representing the specific sensor or device
card[0].subscription.topic = The MQTT topic to which the card subscribes for receiving data
card[0].subscription.qos = The Quality of Service level for the subscription, determining the message delivery guarantee (e.g., "AT_MOST_ONCE").
card[0].subscription.data.name = The name of the data being received from the subscription, providing context for the data (not necassary)
card[0].subscription.data.type = The MIME type of the data being received, indicating the format of the content (e.g., "image/jpeg;base64").
card[0].display.data.jsonpath = The JSON path used to extract specific data from the received JSON payload for display purposes.
card[0].publication.topic = The MQTT topic to which the card publishes messages 
card[0].publication.qos = The Quality of Service level for the publication, determining how messages are sent to the topic (e.g., "AT_MOST_ONCE").
card[0].publication.retain = A boolean value indicating whether the published message should be retained by the broker for future subscribers.
card[0].publication.data = The actual data being published to the topic, which can include status updates, commands or other relevant information
card[0].publication.data.type = The MIME type of the data being published, indicating the format of the content (e.g., "text/plain").
card[0].local.task.path = The file path to a local task or script that can be executed in conjunction with the card's functionality.
card[0].local.task.arguments = The arguments to be passed to the local task or script when it is executed.
card[0].local.task.data.type = The MIME type of the data that the local task will output to stdoutand will be displayed in the local card.

Command Settings Description
