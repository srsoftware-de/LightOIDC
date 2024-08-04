var params = new URLSearchParams(window.location.search)
var json = Object.fromEntries(params);
var scopes = {};

function showConfirmationDialog(name){
    get('name').innerHTML = name;
    show('content');
}

async function showScope(response,scope){
    if (response.ok){
        var content = await response.text();
        get('scopes').innerHTML += content;
    } else {
        get('scopes').innerHTML += '<li>'+scope+' (???)</li>';
    }
}

async function handleResponse(response){
    if (response.ok){
        var json = await response.json();
        if (json.rp) {
            setText("rp",json.rp);
            setText("rp2",json.rp);
        }
        get('scopes').innerHTML = '';
        if (json.unauthorized_scopes){
            scopes = json.unauthorized_scopes;
            for (var scope of json.unauthorized_scopes){
                fetch(web+"scopes/"+scope+".html").then(response => showScope(response,scope))
            }
            show("content");
            return;
        }
        if (json.scope){
            var url = params.get('redirect_uri') + '?' + new URLSearchParams(json).toString();
            redirect(url);
            return;
        }
        show('missing_scopes');
    } else {
        console.log("handleResponse(…) ← ",response);
        if (response.status == 401){
            login();
            return;
        }
        var json = await response.json();
        setText('error',"Error: <br/>"+json.error_description);
        show('error');
        if (json.error != "invalid_request_uri"){
            var url = params.get('redirect_uri') + '?' + new URLSearchParams(json).toString();
            console.log('redirecting to '+url);
            redirect(url);
        }
    }
}

function grantAutorization(days){
    json['authorized'] = { days : days, scopes : scopes};
    backendAutorization();
}

function denyAutorization(){
    redirect(params.get('redirect_uri')+"?error=consent_required");
}

function backendAutorization(){
    fetch(client_controller+"/authorize",{
        method: 'POST',
        body: JSON.stringify(json),
        headers: {
            'Content-Type': 'application/json'
        }
    }).then(handleResponse);
}

backendAutorization();