async function handleClients(response){
    if (response.status == UNAUTHORIZED) {
        redirect('login.html?return_to='+encodeURI(window.location.href))
        return;
    }
    var clients = await response.json();
    var bottom = document.getElementById('bottom');
    for (let id in clients){
        var row = document.createElement("tr");
        var client = clients[id];
        row.innerHTML = "<td>"+client.name+"</td>\n<td>"+id+"</td>\n<td>"+client.redirect_uris.join("<br/>")+'</td>\n<td><button onclick="remove(\''+id+'\')" type="button">remove '+client.name+'</button></td>';
        bottom.parentNode.insertBefore(row,bottom);
    }
}

function handleRemove(response){
    redirect("clients.html");
}

function remove(clientId){
    var message = document.getElementById('message').innerHTML;
    if (confirm(message.replace("{}",clientId))) {
        fetch(api+"/client",{
            method: 'DELETE',
            body : JSON.stringify({ client_id : clientId })
        }).then(handleRemove);
    }
}

fetch(api+"/clients",{method:'POST'}).then(handleClients);