var params = new URLSearchParams(window.location.search);
var id = params.get('id');


async function handleLoadResponse(response){
    if (response.ok){
        var json = await response.json();
        get('client-id').value = json.client_id;
        get('client-name').value = json.name;
        get('client-secret').value = json.secret;
        get('redirect-urls').value = json.redirect_uris.join("\n");
    }
}

async function handleUpdateResponse(response){
    if (response.ok) {
        enable('button');
        setText('button','saved.');
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
        redirect_uris : getValue('redirect-urls').split("\n")
    };
    fetch(client_controller+'/update',{
        method : 'POST',
        headers : {
           'Content-Type': 'application/json'
        },
        body : JSON.stringify(data)
    }).then(handleUpdateResponse);
    setTimeout(resetButton,4000);
}

fetch(api+'/client',
    {
        method: 'POST',
        body: JSON.stringify({
            client_id : id
        })
    }).then(handleLoadResponse);