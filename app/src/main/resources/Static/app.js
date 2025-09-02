let stompClient = null;
let subDataTopic = '/topic/data';
let connectTopic = '/app/connect';
let dataSuscription = null;

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

function createCommand(id) {
    stompClient.send("/app/publish", {}, JSON.stringify({'id': id}));
}

function getCardSettings(id) {
    stompClient.send("/app/getCardSettings", {}, JSON.stringify({'id': id}));
}

function getCommandSettings(id) {
    stompClient.send("/app/getCommandSettings", {}, JSON.stringify({'id': id}));
}

function saveCard(data) {
    stompClient.send("/app/saveCard", {}, JSON.stringify({'data': data}));
}

function saveCommand(data) {
    stompClient.send("/app/saveCommand", {}, JSON.stringify({'data': data}));
}

function saveComponent(data) {
    stompClient.send("/app/saveComponent", {}, JSON.stringify({'data': data}));
}

function deleteDashboardCard(data) {
    stompClient.send("/app/deleteCard", {}, JSON.stringify({'data': data}));
}

function deleteCommand(data) {
    stompClient.send("/app/deleteCommand", {}, JSON.stringify({'data': data}));
}

function deleteComponent(data) {
    stompClient.send("/app/deleteComponent", {}, JSON.stringify({'data': data}));
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
    setInnerHtml(receiverId + '-payload', '<p>' + JSON.stringify(payload) + '</p>');
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
        createCommand(arg);
    });

    $(document).on("click", "#editCardSettings", function () {
        const arg = $(this).val();
        getCardSettings(arg);
    });

    $(document).on("click", "#editCommandSettings", function () {
        const arg = $(this).val();
        getCommandSettings(arg);
    });
    
    $(document).on("click", "#editComponentSettings", function () {
        const arg = $(this).val();
        getComponentSettings(arg);
    });

    $(document).on("click", "#addCard", function () {
        const arg = $(this).val();
        getCardSettings(arg);
    });

    $(document).on("click", "#addCommand", function () {
        const arg = $(this).val();
        getCommandSettings(arg);
    });

    $(document).on("click", "#addComponent", function () {
        const arg = $(this).val();
        getComponentSettings(arg);
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
        saveCard(JSON.stringify(getFormData('cardSettingsForm')));
        setTimeout(() => {
            goToStartDashboard();
        }, 100);
    });

    $(document).on("click", "#saveCommand", function () {
        saveCommand(JSON.stringify(getFormData('commandSettingsForm')));
        setTimeout(() => {
            goToCommandDashboard();
        }, 100);
    });

    $(document).on("click", "#saveComponent", function () {
        saveComponent(JSON.stringify(getFormData('componentSettingsForm')));
        setTimeout(() => {
            goToComponentDashboard();
        }, 100);
    });
    
    $(document).on("click", "#cancel", function () {
        goToStartDashboard();
    });

    $(document).on("click", "#deleteCard", function () {
        if (!confirm('Delete card?')) {
            return;
        }
        deleteDashboardCard(JSON.stringify(getFormData('cardSettingsForm')));
        setTimeout(() => {
            goToStartDashboard();
        }, 100);
    });
    
    $(document).on("click", "#deleteCommand", function () {
        if (!confirm('Delete command?')) {
            return;
        }
        deleteCommand(JSON.stringify(getFormData('commandSettingsForm')));
        setTimeout(() => {
            goToCommandDashboard();
        }, 100);
    });
    
    $(document).on("click", "#deleteComponent", function () {
        if (!confirm('Delete component?')) {
            return;
        }
        deleteComponent(JSON.stringify(getFormData('componentSettingsForm')));
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

});



