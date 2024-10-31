var params = new URLSearchParams(window.location.search);
var id = params.get('id');
var token_validity = 10;

function displayDuration(){
    var mins = token_validity;
    hrs = Math.floor(mins/60);
    mins-=60*hrs;
    days = Math.floor(hrs/24);
    hrs-=24*days;
    setText('days',days);
    setText('hours',hrs);
    setText('minutes',mins);
}

function durationUpdate(){
    token_validity = getValue('token_validity');
    displayDuration();
}

function handleAutoDiscover(response){
    if (response.ok){
        response.json().then(json => {
            console.log(json);
            setText('authorization',json.authorization_endpoint);
            setText('token',json.token_endpoint);
            setText('userinfo',json.userinfo_endpoint);
        });
    }
}
function handleLoadResponse(response){
    if (response.ok){
        response.json().then(json => {
            get('client-id').value = json.client_id;
            get('client-name').value = json.name;
            get('client-secret').value = json.secret;
            get('redirect-urls').value = json.redirect_uris.join("\n");
            get('landing-page').value = json.landing_page?json.landing_page:'';
            token_validity = json.token_validity?json.token_validity:10;
            displayDuration();
        });
    }
}

function handleUpdateResponse(response){
    if (response.ok) {
        enable('button');
        setText('button','saved.');
        redirect('clients.html');
    }
}

function resetButton(){
    enable('button');
    setText('button','Update')
}

function updateClient(){
    disable('button');
    setText('button','sent data…')
    var data = {
        client_id : getValue('client-id'),
        name : getValue('client-name'),
        secret : getValue('client-secret'),
        redirect_uris : getValue('redirect-urls').split("\n"),
        landing_page : getValue('landing-page'),
        token_validity : getValue('token_validity')
    };
    fetch(client_controller+'/update',{
        method : 'POST',
        headers : {
           'Content-Type': 'application/json'
        },
        body : JSON.stringify(data),
        credentials:'include'
    }).then(handleUpdateResponse);
    setTimeout(resetButton,4000);
}

document.addEventListener("DOMContentLoaded", function(event) { // wait until page loaded
    fetch(api+'/client', {
        method: 'POST',
        body: JSON.stringify({
            client_id : id
        }),
        credentials:'include'
    }).then(handleLoadResponse);
    var autodiscover = window.location.origin+'/.well-known/openid-configuration';
    setText('autodiscover',autodiscover);
    fetch(autodiscover).then(handleAutoDiscover);
});


