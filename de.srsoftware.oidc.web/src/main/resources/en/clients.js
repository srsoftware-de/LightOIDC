
async function handleClients(response){
    if (response.status == UNAUTHORIZED) {
        redirect('login.html?return_to='+encodeURI(window.location.href))
        return;
    }
    var clients = await response.json();
    get()
}

fetch(api+"/clients",{method:'POST'}).then(handleClients);