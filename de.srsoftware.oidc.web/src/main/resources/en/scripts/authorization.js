var params = new URLSearchParams(window.location.search)
var json = paramsToObject(params);
var scopes = {};

function showConfirmationDialog(name){
    get('name').innerHTML = name;
    show('content');
}

function showScope(response,scope){
    if (response.ok){
        response.text().then(content => {
            get('scopes').innerHTML += content
        });
    } else {
        get('scopes').innerHTML += '<li>'+scope+' (???)</li>';
    }
}

function handleResponse(response){
    hideAll('error');
    if (response.ok){
        response.json().then(json => {
            if (json.rp) {
                setText("rp",json.rp);
                setText("rp2",json.rp);
            }
            get('scopes').innerHTML = '';
            if (json.unauthorized_scopes){
                scopes = json.unauthorized_scopes;
                for (var scope of json.unauthorized_scopes){
                    fetch(web+"scopes/"+scope+".html",{credentials:'include'}).then(response => showScope(response,scope))
                }
                show("content");
                return;
            }
            if (json.scope){
                var query = Object.keys(json).map(key => `${key}=${encodeURIComponent(json[key])}`).join('&');
                var url = params.get('redirect_uri') + '?' + query.toString();
                redirect(url);
                return;
            }
            show('missing_scopes');
        });
    } else {
        console.log("handleResponse(…) ← ",response);
        if (response.status == 401){ // unauthorized
            login();
            return;
        }
        response.json().then(json => {
            console.log("handleResponse → error",json);
            if (json.error) show(json.error);
            if (json.metadata.client_id) setText('client_id',json.metadata.client_id);
            if (json.metadata.parameter) setText('parameter',json.metadata.parameter);
             if (json.metadata.redirect_uri) setText('redirect_uri',json.metadata.redirect_uri);
            if (json.metadata.response_type)setText('response_type',json.metadata.response_type)
        });
        /*if (json.error != "invalid_request_uri"){
            var url = params.get('redirect_uri') + '?' + new URLSearchParams(json).toString();
            console.log('redirecting to '+url);
            redirect(url);
        }*/
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
        },
        credentials:'include'
    }).then(handleResponse);
}

document.addEventListener("logged_in", function(event) { // wait until page loaded
    backendAutorization();
});

