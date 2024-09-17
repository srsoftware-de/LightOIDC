var params = new URLSearchParams(window.location.search);
var id = params.get('id');


function handleLoadResponse(response){
    if (response.ok){
        response.json().then(json => {
            get('client-id').value = json.client_id;
            get('client-name').value = json.name;
            get('client-secret').value = json.secret;
            get('redirect-urls').value = json.redirect_uris.join("\n");
            get('landing-page').value = json.landing_page?json.landing_page:'';
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
        landing_page : getValue('landing-page')
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

fetch(api+'/client',
    {
        method: 'POST',
        body: JSON.stringify({
            client_id : id
        }),
        credentials:'include'
    }).then(handleLoadResponse);
