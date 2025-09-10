let stompClient = null;
let subDataTopic = '/topic/data';
let connectTopic = '/app/connect';
let dataSubscription = null;
let sendCommandTopic = "/app/publish";
let getCardSettingsTopic = "/app/getCardSettings";
let getCommandSettingsTopic = "/app/getCommandSettings";
let getComponentSettingsTopic = "/app/getComponentSettings";
let getMqttSettingsTopic = "/app/getMqttSettings";
let saveCardTopic = "/app/saveCard";
let saveCommandTopic = "/app/saveCommand";
let saveComponentTopic = "/app/saveComponent";
let deleteCardTopic = "/app/deleteCard";
let deleteCommandTopic = "/app/deleteCommand";
let deleteComponentTopic = "/app/deleteComponent";

function setConnected(connected) {
    $("#connect").prop("disabled", connected);
    $("#disconnect").prop("disabled", !connected);
    $("#shutdown").prop("disabled", !connected);
    $("#options").prop("disabled", !connected);
}

function connect() {
    if (stompClient && stompClient.connected) {
        console.warn('Already connected');
        return;
    }
    let socket = new SockJS('/mq2tClientDashboard');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function (frame) {
        console.log('Connected: ' + frame);
        setConnected(true);
        dataSubscription = stompClient.subscribe(subDataTopic, function (message) {
            showData(JSON.parse(message.body), message.headers.card);
        });
        stompClient.send(connectTopic, {}, JSON.stringify({'id': "doConnect"}));
    }, error => {
        console.error('STOMP connection error', error);
        setConnected(false);
    });
    stompClient.debug = function (msg) {
        console.log(msg);
    };
}

function goToStartDashboard() {
    stompClient.send("/app/displayCardDashboard", {}, JSON.stringify({'id': ""}));
}

function goToCommandDashboard() {
    stompClient.send("/app/displayCommandDashboard", {}, JSON.stringify({'id': ""}));
}

function goToComponentDashboard() {
    stompClient.send("/app/displayComponentDashboard", {}, JSON.stringify({'id': ""}));
}

function disconnect() {
    if (stompClient !== null) {
        stompClient.send("/app/disconnect", {}, JSON.stringify({'id': "disconnect"}));
        if (dataSubscription) {
            stompClient.unsubscribe();
        }
        stompClient.disconnect();
    }
    setConnected(false);
}

function shutdown() {
    stompClient.send("/app/shutdownApp", {}, JSON.stringify({'id': "shutdown"}));
    document.body.innerHTML = "<div style=\"color:green;\">Bye!</div>";
}

function showImage(message, receiverId) {
    let image = new Image();
    image.src = 'data:image/jpeg;base64,' + message.data;

    setInnerHtml(receiverId + '-payload', '<img src="' + image.src + '" class="img-fluid" alt="...">');

    let saveButton = document.getElementById(receiverId + '-save');
    if (saveButton !== null) {
        saveButton.setAttribute('href', image.src);
        saveButton.classList.remove("disabled");
    }
}

function showPlainText(message, receiverId) {
    setInnerHtml(receiverId + '-payload', '<p>' + message.data + '</p>');
}

function showTimestamp(message, receiverId) {
    if (typeof message.timestamp === 'undefined') {
        setInnerHtml(receiverId + '-timestamp', 'undefined');
    } else {
        let date = new Date(parseInt(message.timestamp, 10));
        let hours = date.getHours();
        let minutes = '0' + date.getMinutes();
        let seconds = '0' + date.getSeconds();
        setInnerHtml(receiverId + '-timestamp', hours + ':' + minutes.substr(-2) + ':' + seconds.substr(-2));
    }
}

function showBase64(payload, receiverId) {
    let str = '';
    try {
        str = b64ToUtf8(payload);
        if (receiverId === 'dashboard') {
            setInnerHtml('dashboard', str);
        } else {
            setInnerHtml(receiverId + '-payload', '<p>' + str + '</p>');
        }
    } catch (err) {
        console.error('Error. The Base64 string to be decoded is not correctly encoded. ', err);
        if (receiverId === 'dashboard') {
            setInnerHtml('errors', "<div style=\"color:red;\">Error. The Base64 string to be decoded is not correctly encoded.</div>");
        } else {
            setInnerHtml(receiverId + '-payload', '<p style=\"color:red;\">Error. The Base64 string to be decoded is not correctly encoded</p>');
        }
    }
}

function b64ToUtf8(str) {
    let binary = atob(str);

    let bytes = new Uint8Array(binary.length);
    for (let i = 0; i < binary.length; i++) {
        bytes[i] = binary.charCodeAt(i);
    }

    return new TextDecoder('utf-8').decode(bytes);
}

function showJson(message, receiverId) {
    try {
        payload = JSON.parse(message.data);
    } catch (SyntaxError) {
        console.error('Error. Not valid Json for card=' + receiverId);
        setInnerHtml(receiverId + '-payload', '<p style=\"color:red;\">Error parsing JSON</p>');
        return;
    }
    
    if (payload.hasOwnProperty("data")) {
        setInnerHtml(receiverId + '-payload', '<p>' + payload.data + '</p>');
    } else {
        setInnerHtml(receiverId + '-payload', '<p>' + JSON.stringify(payload) + '</p>');
    }
}

function showData(message, receiverId) {
    let payload;
    if (!message) {
        setInnerHtml(receiverId + '-payload', '<p style=\"color:red;\">Error. Message is null/undefined</p>');
        console.error('Message is null/undefined for card=' + receiverId);
        return;
    }

    showTimestamp(message, receiverId);

    let type = (typeof message.type === 'string') ? message.type.toUpperCase() : '';
    switch (type) {
        case 'IMAGE/JPEG;BASE64':
            showImage(message, receiverId);
            break;

        case 'TEXT/PLAIN':
            showPlainText(message, receiverId);
            break;

        case 'TEXT/HTML;BASE64':
            showBase64(message.data, receiverId);
            break;

        case 'APPLICATION/JSON':
            showJson(message, receiverId);
            break;

        default:
            setInnerHtml(receiverId + '-payload', '<p style=\"color:red;\">Error. Incorrect payload type</p>');
            console.error("Error. Incorrect payload type for card=" + receiverId + ". Type is " + message.type);
    }
}

function setInnerHtml(selector, html) {
    let el = document.getElementById(selector);
    if (el) {
        el.innerHTML = safeHtml(html);
    }

    return el;
}

function safeHtml(html) {
    return DOMPurify.sanitize(html, {
        ALLOWED_TAGS: ['form', 'label', 'select', 'option', 'input', 'b', 'i', 'em', 'strong', 'p', 'ul', 'li', 'img', 'div', 'button', 'small', 'h5', 'svg', 'path', 'a'],
        ALLOWED_ATTR: ['method', 'selected', 'name', 'src', 'alt', 'title', 'class', 'type', 'id', 'value', 'aria-label', 'width', 'height', 'fill', 'viewBox', 'd', 'style', 'href']
    });
}

$(function () {
    $("form").on('submit', function (e) {
        e.preventDefault();
    });
    
    $("#connect").click(function () {
        connect();
    });
    
    $("#disconnect").click(function () {
        disconnect();
    });
    
    $("#shutdown").click(function () {
        shutdown();
    });

    $("#options").click(function () {
        $("#dropdown-menu").toggle();
    });

    $(document).click(function (event) {
        if (!$(event.target).closest('#options').length) {
            $("#dropdown-menu").hide();
        }
    });

    $(document).on("click", "#sendCommand", function () {
        const arg = $(this).val();
        stompClient.send(sendCommandTopic, {}, JSON.stringify({'id': arg}));
    });

    $(document).on("click", "#editCardSettings", function () {
        const arg = $(this).val();
        stompClient.send(getCardSettingsTopic, {}, JSON.stringify({'id': arg}));
    });

    $(document).on("click", "#editCommandSettings", function () {
        const arg = $(this).val();
        stompClient.send(getCommandSettingsTopic, {}, JSON.stringify({'id': arg}));
    });
    
    $(document).on("click", "#editComponentSettings", function () {
        const arg = $(this).val();
        stompClient.send(getComponentSettingsTopic, {}, JSON.stringify({'id': arg}));
    });
    
    $(document).on("click", "#addCard", function () {
        const arg = $(this).val();
        stompClient.send(getCardSettingsTopic, {}, JSON.stringify({'id': arg}));
    });

    $(document).on("click", "#addCommand", function () {
        const arg = $(this).val();
        stompClient.send(getCommandSettingsTopic, {}, JSON.stringify({'id': arg}));
    });

    $(document).on("click", "#addComponent", function () {
        const arg = $(this).val();
        stompClient.send(getComponentSettingsTopic, {}, JSON.stringify({'id': arg}));
    });
    
    function getFormData(name) {
        let el = document.getElementById(name);
        let formData = new FormData(el);
        let data = {};
        formData.forEach((value, key) => {
            data[key] = value;
        });

        return data;
    }

    $(document).on("click", "#saveCard", function () {
        stompClient.send(saveCardTopic, {}, JSON.stringify({'data': JSON.stringify(getFormData('cardSettingsForm'))}));
        setTimeout(() => {
            goToStartDashboard();
        }, 100);
    });

    $(document).on("click", "#saveCommand", function () {
        stompClient.send(saveCommandTopic, {}, JSON.stringify({'data': JSON.stringify(getFormData('commandSettingsForm'))}));
        setTimeout(() => {
            goToCommandDashboard();
        }, 100);
    });

    $(document).on("click", "#saveComponent", function () {
        stompClient.send(saveComponentTopic, {}, JSON.stringify({'data': JSON.stringify(getFormData('componentSettingsForm'))}));
        setTimeout(() => {
            goToComponentDashboard();
        }, 100);
    });

    $(document).on("click", "#cancel", function () {
        const arg = $(this).val();
        if (arg === 'card') {
            goToStartDashboard();
        } else if (arg === 'command') {
            goToCommandDashboard();
        } else if (arg === 'component') {
            goToComponentDashboard();
        } else {
            console.error('Invalid value for cancel button - ' + arg);
        }
    });

    $(document).on("click", "#deleteCard", function () {
        if (!confirm('Delete card?')) {
            return;
        }
        stompClient.send(deleteCardTopic, {}, JSON.stringify({'data': JSON.stringify(getFormData('cardSettingsForm'))}));
        setTimeout(() => {
            goToStartDashboard();
        }, 100);
    });

    $(document).on("click", "#deleteCommand", function () {
        if (!confirm('Delete command?')) {
            return;
        }
        stompClient.send(deleteCommandTopic, {}, JSON.stringify({'data': JSON.stringify(getFormData('commandSettingsForm'))}));
        setTimeout(() => {
            goToCommandDashboard();
        }, 100);
    });

    $(document).on("click", "#deleteComponent", function () {
        if (!confirm('Delete component?')) {
            return;
        }
        stompClient.send(deleteComponentTopic, {}, JSON.stringify({'data': JSON.stringify(getFormData('componentSettingsForm'))}));
        setTimeout(() => {
            goToComponentDashboard();
        }, 100);
    });

    $(document).on("click", "#viewCards", function () {
        goToStartDashboard();
    });

    $(document).on("click", "#viewCommands", function () {
        goToCommandDashboard();
    });

    $(document).on("click", "#viewComponents", function () {
        goToComponentDashboard();
    });
    
    $(document).on("click", "#viewMqttSettings", function () {
        stompClient.send(getMqttSettingsTopic, {}, JSON.stringify({'id': ""}));
    });

});