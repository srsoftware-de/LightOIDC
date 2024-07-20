const UNAUTHORIZED = 401;

async function handleUser(response){
    if (response.status == UNAUTHORIZED) {
        window.location.href = 'login.html?return_to='+encodeURI(window.location.href);
        return;
    }
    var user = await response.json();
    // TODO: load navigation
}

fetch(api+"/user",{method:'POST'}).then(handleUser);