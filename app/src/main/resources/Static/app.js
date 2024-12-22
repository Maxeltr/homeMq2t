var stompClient = null;

function setConnected(connected) {
    $("#connect").prop("disabled", connected);
    $("#disconnect").prop("disabled", !connected);

}

function connect() {
    var socket = new SockJS('/mq2tClientDashboard');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function (frame) {
        console.log('Connected: ' + frame);
        stompClient.subscribe('/topic/data', function (message) {
            showData(JSON.parse(message.body));
        });
        //stompClient.send("/app/connect", {}, JSON.stringify({'topic': "", 'type': "application/json", 'timestamp': Date.now(), 'payload': {'name': "connect"}}));
        //stompClient.send("/app/connect", {}, JSON.stringify({'topic': "onconnecte", 'payload': "-", 'type': "application/json", 'timestamp': "--"}));
        stompClient.send("/app/connect", {}, JSON.stringify({'id': "doConnect"}));
    });
}

function disconnect() {
    stompClient.send("/app/disconnect", {}, JSON.stringify({'id': "disconnect"}));
    if (stompClient !== null) {
        stompClient.disconnect();
    }
    setConnected(false);
    console.log("Disconnected");
}

function createCommand(id) {
    console.log('createCommand ' + id);
    stompClient.send("/app/publish", {}, JSON.stringify({'id': id}));
    //stompClient.send("/app/connected", {}, JSON.stringify({'id': "connectsfgdsfgsdfg"}));
}

/* function onConnect(message) {
 var dashboard = document.getElementById('dashboard');
 var payload = JSON.parse(message.payload);
 if (payload.name.toUpperCase() === 'ONCONNECT' && payload.status.toUpperCase() === 'OK') {
 setConnected(true);
 }
 dashboard.innerHTML = atob(payload.data);
 } */

function showData(message) {
    if (message.type.toUpperCase() !== "APPLICATION/JSON") {
        console.log("Error. Incorrect payload type. Require application/json");
        document.getElementById('dashboard').innerHTML = "<div style=\"color:red;\">Error. Incorrect payload type. Require application/json.</div>";
        return;
    }

    var payload = JSON.parse(message.payload);
    if (payload.name.toUpperCase() === 'ONCONNECT') {
        if (payload.type.toUpperCase() === 'TEXT/HTML;BASE64') {
            document.getElementById('dashboard').innerHTML = atob(payload.data);
        } else {
            console.log("Error. Incorrect payload type. Require text/html;base64");
        }
        if (payload.status.toUpperCase() === 'OK') {
            setConnected(true);
        }
        return;
    }

    var card = payload.name;

    if (message.timestamp === 'undefined') {
        console.log('message.timestamp is undefined');
        document.getElementById(card + '-timestamp').innerHTML === 'undefined';
    } else {
        var date = new Date(parseInt(message.timestamp, 10));
        var hours = date.getHours();
        var minutes = '0' + date.getMinutes();
        var seconds = '0' + date.getSeconds();
        document.getElementById(card + '-timestamp').innerHTML = hours + ':' + minutes.substr(-2) + ':' + seconds.substr(-2);
    }



    if (message.type !== 'undefined') {
        if (message.type.toUpperCase() === 'IMAGE/JPEG') {
            var image = new Image();
            image.src = 'data:image/jpeg;base64,' + payload.data;
            document.getElementById(card + '-payload').innerHTML = '<img src="' + image.src + '" class="img-fluid" alt="...">';
            var saveButton = document.getElementById(card + '-save');
            saveButton.setAttribute('href', image.src);
            saveButton.classList.remove("disabled");
        } else if (message.type.toUpperCase() === 'TEXT/PLAIN') {
            document.getElementById(card + '-payload').innerHTML = '<p>' + atob(payload.data) + '</p>';
        } else if (message.type.toUpperCase() === 'APPLICATION/JSON') {
            console.log('message.type is APPLICATION/JSON ' + atob(payload.data));
        } else {
            document.getElementById(card + '-payload').innerHTML = '<p>' + "Type is " + message.type + '<br>' + atob(payload.data) + '</p>';
        }
    } else {
        console.log('message.type is undefined');
        document.getElementById(card + '-payload').innerHTML = '<p>' + 'message.type is undefined' + '<br>' + atob(payload.data) + '</p>';
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

    $(document).on("click", "#sendCommand", function(){
        var arg = $(this).val();
        createCommand(arg);
    });


});



