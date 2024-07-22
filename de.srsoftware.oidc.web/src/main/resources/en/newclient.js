function addClient(){
    disable('button');
    var newData = {
        client_id : getValue('client-id'),
        name : getValue('client-name'),
        secret : getValue('client-secret'),
        redirect_uris : getValue('redirect-urls').split("\n")
    };
    fetch(api+'/add/client',{
        method : 'POST',
        headers : {
           'Content-Type': 'application/json'
        },
        body : JSON.stringify(newData)
    }).then(handleClientdResponse);

    setText('button','sent…');
    setTimeout(function(){
        setText('button','Add client');
        enable('button');
    },10000);
}

function handleClientdResponse(response){
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

setTimeout(checkPermissions,100);
