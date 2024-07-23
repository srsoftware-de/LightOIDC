var params = new URLSearchParams(window.location.search)
var json = Object.fromEntries(params);

async function handleResponse(response){
    if (response.ok){
        var json = await response.json();
        console.log(json);
        redirect(json.redirect_uri+'?code='+json.code+'&state='+json.state);
        return;
    }
}

fetch(api+"/authorize",{
    method: 'POST',
    body: JSON.stringify(json),
    headers: {
        'Content-Type': 'application/json'
    }
}).then(handleResponse);