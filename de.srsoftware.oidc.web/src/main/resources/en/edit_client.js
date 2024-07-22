var params = new URLSearchParams(window.location.search);
var id = params.get('id');

fetch(api+'/client',
    {
        method: 'POST',
        body: JSON.stringify({
            client_id : id
        })
    }).then(handleResponse);

async function handleResponse(response){
    if (response.ok){
        var json = await response.json();
        get('client_id').value = json.client_id;
        get('name').value = json.name;
        get('secret').value = json.secret;
        get('redirects').value = json.redirect_uris.join("\n");
    }
}
