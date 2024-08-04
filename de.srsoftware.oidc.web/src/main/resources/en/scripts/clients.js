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
        row.innerHTML = "<td>"+client.name+"</td>\n<td>"+id+"</td>\n<td>"+client.redirect_uris.join("<br/>")+'</td>\n<td><button class="danger" onclick="remove(\''+id+'\')" type="button">remove&nbsp;'+client.name+'</button><button type="button" onclick="edit(\''+id+'\')">Edit</button></td>';
        bottom.parentNode.insertBefore(row,bottom);
    }
}

function handleRemove(response){
    redirect("clients.html");
}

function remove(clientId){
    var message = document.getElementById('message').innerHTML;
    if (confirm(message.replace("{}",clientId))) {
        fetch(client_controller+"/delete",{
            method: 'DELETE',
            body : JSON.stringify({ client_id : clientId })
        }).then(handleRemove);
    }
}

function edit(clientId){
    redirect("edit_client.html?id="+clientId);
}

fetch(client_controller+"/list",{method:'POST'}).then(handleClients);