var stompClient = null;

function setConnected(connected) {
    $("#connect").prop("disabled", connected);
    $("#disconnect").prop("disabled", !connected);

}

function connect() {
    var socket = new SockJS('/mq2tClientDashboard');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function (frame) {
        //setConnected(true);
        console.log('Connected: ' + frame);
        stompClient.subscribe('/topic/replies', function (message) {
            showReplies(JSON.parse(message.body), message.headers.card);
        });
        stompClient.subscribe('/topic/data', function (message) {
            showData(JSON.parse(message.body), message.headers.card);
        });
        stompClient.send("/app/connect", {}, JSON.stringify({'id': "-1", 'name': "connect"}));
    });
}

function disconnect() {
    if (stompClient !== null) {
        stompClient.disconnect();
    }
    //setConnected(false);
    console.log("Disconnected");
}

function createCommand(commandNumber) {
    //stompClient.send("/app/createCommand", {}, JSON.stringify({'commandNumber': commandNumber}));
    //stompClient.send("/app/connected", {}, JSON.stringify({'id': "connectsfgdsfgsdfg"}));
}

function showReplies(message, card) {
    console.log(card);

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

    if (message.result === 'undefined') {
        console.log('message.result is undefined');
        document.getElementById(card + '-payload').innerHTML = '<p> message.result is undefined </p>';
        return;
    }

    if (message.result.toUpperCase() !== 'OK') {
        console.log('message.result is fail');
        document.getElementById(card + '-payload').innerHTML = '<p>' + 'Result is fail ' + message.result + '<br>' + message.payload + '</p>';
        return;
    }

    if (message.type !== 'undefined') {
        if (message.type.toUpperCase() === 'IMAGE/JPEG') {
            var image = new Image();
            image.src = 'data:image/jpeg;base64,' + message.payload;
            document.getElementById(card + '-payload').innerHTML = '<img src="' + image.src + '" class="img-fluid" alt="...">';
            var saveButton = document.getElementById(card + '-save');
            saveButton.setAttribute('href', image.src);
            saveButton.classList.remove("disabled");
        } else if (message.type.toUpperCase() === 'TEXT/PLAIN') {
            document.getElementById(card + '-payload').innerHTML = '<p>' + message.payload + '</p>';
        } else {
            console.log('message.type is ' + message.type);
            document.getElementById(card + '-payload').innerHTML = '<p>' + "Type is " + message.type + '<br>' + message.payload + '</p>';
        }
    } else {
        console.log('message.type is undefined');
        document.getElementById(card + '-payload').innerHTML = '<p>' + 'message.type is undefined' + '<br>' + message.payload + '</p>';
    }

}

function showData(message, card) {
    console.log('message : ' + message.name + ' ' + message.status + ' ' + card);

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

    if (message.name.toUpperCase() === 'connect') {
        if (message.status.toUpperCase() === 'FAIL') {
            setConnected(true);
        }
        return;
    }

    if (message.type !== 'undefined') {
        if (message.type.toUpperCase() === 'IMAGE/JPEG') {
            var image = new Image();
            image.src = 'data:image/jpeg;base64,' + message.payload;
            document.getElementById(card + '-payload').innerHTML = '<img src="' + image.src + '" class="img-fluid" alt="...">';
            var saveButton = document.getElementById(card + '-save');
            saveButton.setAttribute('href', image.src);
            saveButton.classList.remove("disabled");
        } else if (message.type.toUpperCase() === 'TEXT/PLAIN') {
            document.getElementById(card + '-payload').innerHTML = '<p>' + message.payload + '</p>';
        } else {
            console.log('message.type is ' + message.type);
            document.getElementById(card + '-payload').innerHTML = '<p>' + "Type is " + message.type + '<br>' + message.payload + '</p>';
        }
    } else {
        console.log('message.type is undefined');
        document.getElementById(card + '-payload').innerHTML = '<p>' + 'message.type is undefined' + '<br>' + message.payload + '</p>';
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
    $("#sendCommand ").click(function () {
        var arg = $(this).val();
        createCommand(arg);
    });


});



