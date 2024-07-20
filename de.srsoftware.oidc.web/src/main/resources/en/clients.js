
async function handleClients(response){
    if (response.status == UNAUTHORIZED) {
        window.location.href = 'login.html?return_to='+encodeURI(window.location.href);
        return;
    }
    var clients = await response.json();
    console.log(clients);
}

fetch(api+"/clients",{method:'POST'}).then(handleClients);