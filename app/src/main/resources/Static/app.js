var stompClient = null;

function setConnected(connected) {
    $("#connect").prop("disabled", connected);
    $("#disconnect").prop("disabled", !connected);
    $("#shutdown").prop("disabled", !connected);
}

function connect() {
    var socket = new SockJS('/mq2tClientDashboard');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function (frame) {
        console.log('Connected: ' + frame);
        stompClient.subscribe('/topic/data', function (message) {
            showData(JSON.parse(message.body), message.headers.card);
        });
        stompClient.send("/app/connect", {}, JSON.stringify({'id': "doConnect"}));
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

function showImage(message, cardNumber) {
    var image = new Image();
    image.src = 'data:image/jpeg;base64,' + message.data;

    el = document.getElementById(cardNumber + '-payload');
    if (el !== null) {
        el.innerHTML = '<img src="' + image.src + '" class="img-fluid" alt="...">';
    }

    var saveButton = document.getElementById(cardNumber + '-save');
    if (saveButton !== null) {
        saveButton.setAttribute('href', image.src);
        saveButton.classList.remove("disabled");
    }
}

function showPlainText(message, cardNumber) {
    el = document.getElementById(cardNumber + '-payload');
    if (el !== null) {
        el.innerHTML = '<p>' + message.data + '</p>';
    }
}

function showTimestamp(message, cardNumber) {
    if (message.timestamp === 'undefined') {
        el = document.getElementById(cardNumber + '-timestamp');
        if (el !== null) {
            el.innerHTML = 'undefined';
        }
    } else {
        var date = new Date(parseInt(message.timestamp, 10));
        var hours = date.getHours();
        var minutes = '0' + date.getMinutes();
        var seconds = '0' + date.getSeconds();
        el = document.getElementById(cardNumber + '-timestamp');
        if (el !== null) {
            el.innerHTML = hours + ':' + minutes.substr(-2) + ':' + seconds.substr(-2);
        }
    }
}

function showData(message, cardNumber) {
    var el;

    showTimestamp(message, cardNumber);

    if (message.type !== 'undefined') {
        if (message.type.toUpperCase() === 'IMAGE/JPEG;BASE64') {
            showImage(message, cardNumber);

        } else if (message.type.toUpperCase() === 'TEXT/PLAIN') {
            showPlainText(message, cardNumber);

        } else if (message.type.toUpperCase() === 'APPLICATION/JSON') {
            try {
                var payload = JSON.parse(message.data);
            } catch (SyntaxError) {
                console.log('Error. Not valid Json. Shows as plain text.');
                document.getElementById('errors').innerHTML = "<div style=\"color:red;\">Error. Invalid json. Card=" + cardNumber + ".</div>";
                showPlainText(message, cardNumber);
                return;
            }

            if (payload.hasOwnProperty("name") && payload.name.toUpperCase() === 'ONCONNECT') {
                if (payload.hasOwnProperty("type") && payload.type.toUpperCase() === 'TEXT/HTML;BASE64') {
                    data = payload.hasOwnProperty("data") ? atob(payload.data) : "<div style=\"color:red;\">Error to show dashboard. No data available.</div>";
                } else {
                    console.log("Error. Incorrect payload type. Require text/html;base64 for message 'onconnect'");
                    data = "<div style=\"color:red;\">Error. Incorrect payload type. Require text/html;base64 for message 'onconnect'.</div>";
                }
                
                if (payload.hasOwnProperty("status")) {
                    if (payload.status.toUpperCase() === 'OK') {	
                        setConnected(true);
                    }
                } else {
                    console.log("Error. No status available.");
                }
                
                el = document.getElementById('dashboard');
                if (el !== null) {
                    el.innerHTML = data;	
                } else {
                    console.log("Error. No dashboard available.");	
                }
                                
                return;
            }

            if (payload.hasOwnProperty("name")) {
                el = document.getElementById(cardNumber + '-text');
                if (el !== null) {
                    el.innerHTML = payload.name;
                }
            }

            if (payload.hasOwnProperty("status")) {
                el = document.getElementById(cardNumber + '-status');
                if (el !== null) {
                    el.innerHTML = payload.status;
                }
            }

            el = document.getElementById(cardNumber + '-payload');
            if (el !== null) {
                if (payload.hasOwnProperty("data")) {
                    el.innerHTML = '<p>' + payload.data + '</p>';
                } else {
                    el.innerHTML = '<p>' + payload + '</p>';
                }
            }

        } else {
            console.log("Error. Incorrect payload type for card=" + cardNumber + ". Message type is " + message.type);
            document.getElementById('errors').innerHTML = "<div style=\"color:red;\">Error. Incorrect payload type for card=" + cardNumber + ".</div>";
        }
    } else {
        console.log('Error: message type is undefined');
        document.getElementById('errors').innerHTML = "<div style=\"color:red;\">Error: message type is undefined.</div>";
    }
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

    $(document).on("click", "#sendCommand", function () {
        var arg = $(this).val();
        createCommand(arg);
    });


});



