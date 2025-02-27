function addClient(){
    disable('button');
    var newData = {
        client_id : getValue('client-id'),
        name : getValue('client-name'),
        secret : getValue('client-secret'),
        redirect_uris : getValue('redirect-urls').split("\n"),
        landing_page : getValue('landing-page')
    };
    fetch(client_controller+'/add',{
        method : 'POST',
        headers : {
           'Content-Type': 'application/json'
        },
        body : JSON.stringify(newData),
        credentials:'include'
    }).then(handleClientResponse);

    setText('button','sent…');
    setTimeout(function(){
        setText('button','Add client');
        enable('button');
    },10000);
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

function handleClientResponse(response){
    if (response.ok){
        redirect("clients.html");
    } else {
        setText('button','Failed!');
        enable('button');
    }
}

function checkPermissions(){
    if (user && !user.permissions.includes('MANAGE_CLIENTS')) redirect("index.html");
}


document.addEventListener("DOMContentLoaded", function(event) { // wait until page loaded
setTimeout(checkPermissions,100);
    var autodiscover = window.location.origin+'/.well-known/openid-configuration';
    setText('autodiscover',autodiscover);
    fetch(autodiscover).then(handleAutoDiscover);
});