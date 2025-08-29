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

function deleteDashboardCard(data) {
    stompClient.send("/app/deleteCard", {}, JSON.stringify({'data': data}));
}

function showImage(message, cardNumber) {
    let image = new Image();
    image.src = 'data:image/jpeg;base64,' + message.data;

    setInnerHtml(cardNumber + '-payload', '<img src="' + image.src + '" class="img-fluid" alt="...">');

    let saveButton = document.getElementById(cardNumber + '-save');
    if (saveButton !== null) {
        saveButton.setAttribute('href', image.src);
        saveButton.classList.remove("disabled");
    }
}

function showPlainText(message, cardNumber) {
    setInnerHtml(cardNumber + '-payload', '<p>' + message.data + '</p>');
}

function showTimestamp(message, cardNumber) {
    if (typeof message.timestamp === 'undefined') {
        setInnerHtml(cardNumber + '-timestamp', 'undefined');
    } else {
        let date = new Date(parseInt(message.timestamp, 10));
        let hours = date.getHours();
        let minutes = '0' + date.getMinutes();
        let seconds = '0' + date.getSeconds();
        setInnerHtml(cardNumber + '-timestamp', hours + ':' + minutes.substr(-2) + ':' + seconds.substr(-2));
    }
}

function showBase64(payload) {
    try {
        if (null === setInnerHtml('dashboard', b64ToUtf8(payload))) {
            console.error("Error. No dashboard available.");
            setInnerHtml('errors', "<div style=\"color:red;\">Error. No dashboard available.</div>");
        }
    } catch (err) {
        console.error('Error while showing base64 payload:', err);
        setInnerHtml('errors', "<div style=\"color:red;\">Error. The Base64 string to be decoded is not correctly encoded.</div>");
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

function showData(message, cardNumber) {
    let el, payload;

    showTimestamp(message, cardNumber);

    if (typeof message.type !== 'undefined') {
        if (message.type.toUpperCase() === 'IMAGE/JPEG;BASE64') {
            showImage(message, cardNumber);

        } else if (message.type.toUpperCase() === 'TEXT/PLAIN') {
            showPlainText(message, cardNumber);

        } else if (message.type.toUpperCase() === 'TEXT/HTML;BASE64') {
            showBase64(message.data);

        } else if (message.type.toUpperCase() === 'APPLICATION/JSON') {
            try {
                payload = JSON.parse(message.data);
            } catch (SyntaxError) {
                console.error('Error. Not valid Json for card=' + cardNumber + '. Shows as plain text.');
                //setInnerHtml('errors', "<div style=\"color:red;\">Error. Invalid json. Card=" + cardNumber + ".</div>");
                showPlainText(message, cardNumber);
                return;
            }

            if (payload.hasOwnProperty("data")) {
                setInnerHtml(cardNumber + '-payload', '<p>' + payload.data + '</p>');
            } else {
                setInnerHtml(cardNumber + '-payload', '<p>' + JSON.stringify(payload) + '</p>');
                console.log("No data property for card=" + cardNumber + ' - showing full JSON.');
            }

        } else {
            console.error("Error. Incorrect payload type for card=" + cardNumber + ". Message type is " + message.type);
            setInnerHtml('errors', "<div style=\"color:red;\">Error. Incorrect payload type for card=" + cardNumber + ".</div>");
        }
    } else {
        console.error('Error: message type is undefined');
        setInnerHtml('errors', "<div style=\"color:red;\">Error: message type is undefined.</div>");
    }
}

function setInnerHtml(selector, html) {
    let el = document.getElementById(selector);
    if (el)
        el.innerHTML = safeHtml(html);

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

    $(document).on("click", "#addCard", function () {
        const arg = $(this).val();
        getCardSettings(arg);
    });

    $(document).on("click", "#addCommand", function () {
        const arg = $(this).val();
        getCommandSettings(arg);
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
        }, 300);
    });

    $(document).on("click", "#saveCommand", function () {
        saveCommand(JSON.stringify(getFormData('commandSettingsForm')));
        setTimeout(() => {
            goToCommandDashboard();
        }, 300);
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
        }, 300);

    });

    $(document).on("click", "#viewCommands", function () {
        goToCommandDashboard();
    });

});



