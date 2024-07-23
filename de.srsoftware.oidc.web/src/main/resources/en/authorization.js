var params = new URLSearchParams(window.location.search)
var json = Object.fromEntries(params);

function showConfirmationDialog(name){
    get('name').innerHTML = name;
    show('content');
}

async function handleResponse(response){
    if (response.ok){
        var json = await response.json();
        console.log("handleResponse(ok) ←",json);
        if (!json.confirmed){
            showConfirmationDialog(json.name);
        } else {
            redirect(json.redirect_uri+'?code='+json.code+'&state='+json.state+'&scope=openid');
        }
        return;
    } else {
        var json = await response.json();
        console.log("handleResponse(error) ←",json);
        get('error').innerHTML = "Error: <br/>"+JSON.stringify(json);
        show('error');
    }
}

function grantAutorization(){
    json.confirmed = true;
    backendAutorization();
}

function denyAutorization(){
    redirect(params.get('redirect_uri')+"?error=access denied");
}

function backendAutorization(){
    fetch(api+"/authorize",{
        method: 'POST',
        body: JSON.stringify(json),
        headers: {
            'Content-Type': 'application/json'
        }
    }).then(handleResponse);
}

backendAutorization();