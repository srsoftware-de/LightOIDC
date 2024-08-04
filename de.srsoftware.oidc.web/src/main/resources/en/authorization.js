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
        console.log(response);
        if (response.status == 401){
            login();
            return;
        }
        var text = await response.text();
        setText('error',"Error: <br/>"+text);
        show('error');
    }
}

function grantAutorization(days){
    json['authorized'] = { days : days, scopes : scopes};
    backendAutorization();
}

function denyAutorization(){
    redirect(params.get('redirect_uri')+"?error=access denied");
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