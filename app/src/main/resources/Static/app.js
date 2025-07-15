let stompClient = null;

function setConnected(connected) {
    $("#connect").prop("disabled", connected);
    $("#disconnect").prop("disabled", !connected);
    $("#shutdown").prop("disabled", !connected);
    $("#options").prop("disabled", !connected);
}

function connect() {
    let socket = new SockJS('/mq2tClientDashboard');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function (frame) {
        console.log('Connected: ' + frame);
        stompClient.subscribe('/topic/data', function (message) {
            showData(JSON.parse(message.body), message.headers.card);
        });
        stompClient.send("/app/connect", {}, JSON.stringify({'id': "doConnect"}));
    }, error => {
        console.error('STOMP connection error', error);
    });
}

function disconnect() {
    stompClient.send("/app/disconnect", {}, JSON.stringify({'id': "disconnect"}));
    if (stompClient !== null) {
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
    //stompClient.send("/app/connected", {}, JSON.stringify({'id': "connectsfgdsfgsdfg"}));
}

function editSettings(id) {
    stompClient.send("/app/editSettings", {}, JSON.stringify({'id': id}));
}

function saveCard(data) {
    stompClient.send("/app/saveCard", {}, JSON.stringify({'data': data}));
}

function showImage(message, cardNumber) {
    let image = new Image();
    image.src = 'data:image/jpeg;base64,' + message.data;

    el = document.getElementById(cardNumber + '-payload');
    if (el !== null) {
        el.innerHTML = safeHtml('<img src="' + image.src + '" class="img-fluid" alt="...">');
    }

    let saveButton = document.getElementById(cardNumber + '-save');
    if (saveButton !== null) {
        saveButton.setAttribute('href', image.src);
        saveButton.classList.remove("disabled");
    }
}

function showPlainText(message, cardNumber) {
    el = document.getElementById(cardNumber + '-payload');
    if (el !== null) {
        el.innerHTML = safeHtml('<p>' + message.data + '</p>');
    }
}

function showTimestamp(message, cardNumber) {
    if (typeof message.timestamp === 'undefined') {
        el = document.getElementById(cardNumber + '-timestamp');
        if (el !== null) {
            el.innerHTML = 'undefined';
        }
    } else {
        let date = new Date(parseInt(message.timestamp, 10));
        let hours = date.getHours();
        let minutes = '0' + date.getMinutes();
        let seconds = '0' + date.getSeconds();
        el = document.getElementById(cardNumber + '-timestamp');
        if (el !== null) {
            el.innerHTML = hours + ':' + minutes.substr(-2) + ':' + seconds.substr(-2);
        }
    }
}

function showBase64(payload) {
    let data;
    if (payload.hasOwnProperty("type") && payload.type.toUpperCase() === 'TEXT/HTML;BASE64') {
        data = payload.hasOwnProperty("data") ? atob(payload.data) : "<div style=\"color:red;\">Error to show dashboard. No data available.</div>";
    } else {
        console.log("Error. Incorrect payload type. Require text/html;base64.");
        data = "<div style=\"color:red;\">Error. Incorrect payload type. Require text/html;base64.</div>";
    }

    el = document.getElementById('dashboard');
    if (el !== null) {
        el.innerHTML = safeHtml(data);	//add
    } else {
        console.log("Error. No dashboard available.");
    }
}

function showData(message, cardNumber) {
    let el, payload;

    showTimestamp(message, cardNumber);

    if (typeof message.type !== 'undefined') {
        if (message.type.toUpperCase() === 'IMAGE/JPEG;BASE64') {
            showImage(message, cardNumber);

        } else if (message.type.toUpperCase() === 'TEXT/PLAIN') {
            showPlainText(message, cardNumber);

        } else if (message.type.toUpperCase() === 'APPLICATION/JSON') {
            try {
                payload = JSON.parse(message.data);
            } catch (SyntaxError) {
                console.log('Error. Not valid Json. Shows as plain text.');
                document.getElementById('errors').innerHTML = "<div style=\"color:red;\">Error. Invalid json. Card=" + safeHtml(cardNumber) + ".</div>";
                showPlainText(message, cardNumber);
                return;
            }

            if (!payload.hasOwnProperty("name")) {
                console.log('Error. Property name is not available in json.');
                document.getElementById('errors').innerHTML = "<div style=\"color:red;\">Error. Property name is not available in json.</div>";
                return;
            }

            if (payload.name.toUpperCase() === 'ONCONNECT') {
                showBase64(payload);

                if (payload.hasOwnProperty("status")) {
                    if (payload.status.toUpperCase() === 'OK') {
                        setConnected(true);
                    } else {
                        setConnected(false);
                    }
                } else {
                    console.log("Error. No status available.");
                }

            } else if (payload.name.toUpperCase() === 'ONEDITCARDSETTINGS') {
                showBase64(payload);
            } else {
                el = document.getElementById(cardNumber + '-text');
                if (el !== null) {
                    el.innerHTML = safeHtml(payload.name);
                }

                if (payload.hasOwnProperty("status")) {
                    el = document.getElementById(cardNumber + '-status');
                    if (el !== null) {
                        el.innerHTML = safeHtml(payload.status);
                    }
                }

                el = document.getElementById(cardNumber + '-payload');
                if (el !== null) {
                    if (payload.hasOwnProperty("data")) {
                        el.innerHTML = safeHtml('<p>' + payload.data + '</p>');
                    } else {
                        el.innerHTML = safeHtml('<p>' + JSON.stringify(payload) + '</p>');
                        console.log("Error. No property data for card=" + cardNumber);
                    }
                }
            }
        } else {
            console.log("Error. Incorrect payload type for card=" + cardNumber + ". Message type is " + message.type);
            document.getElementById('errors').innerHTML = "<div style=\"color:red;\">Error. Incorrect payload type for card=" + safeHtml(cardNumber) + ".</div>";
        }
    } else {
        console.log('Error: message type is undefined');
        document.getElementById('errors').innerHTML = "<div style=\"color:red;\">Error: message type is undefined.</div>";
    }
}

function safeHtml(html) {
    return DOMPurify.sanitize(html, {
        ALLOWED_TAGS: ['b', 'i', 'em', 'strong', 'p', 'ul', 'li', 'img', 'div', 'button', 'small', 'h5', 'svg', 'path'],
        ALLOWED_ATTR: ['src', 'alt', 'title', 'class', 'type', 'id', 'value', 'aria-label', 'width', 'height', 'fill', 'viewBox', 'd']
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

    $(document).on("click", "#editSettings", function () {
        const arg = $(this).val();
        editSettings(arg);
    });

    $(document).on("click", "#saveCard", function () {
        let el = document.getElementById('settingsForm');
        let formData = new FormData(el);
        let data = {};
        formData.forEach((value, key) => {
            data[key] = value;
        });
        saveCard(JSON.stringify(data));
        connect();
    });

    $(document).on("click", "#cancel", function () {
        connect();
    });
});



