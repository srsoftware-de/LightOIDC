function edit(clientId){
    redirect("edit_client.html?id="+clientId);
}

function handleClients(response){
    if (response.status == UNAUTHORIZED) {
        redirect('login.html?return_to='+encodeURI(window.location.href))
        return;
    }
    var clients = response.json().then(clients => {
        var bottom = document.getElementById('bottom');
        for (let id in clients){
            var row = document.createElement("tr");
            var client = clients[id];
            row.innerHTML = `<td>${client.name}</td>
            <td>${id}</td>
            <td>${client.redirect_uris.join("<br/>")}</td>
            <td>
                <button type="button" onclick="edit('${id}')">Edit</button>
                <button class="danger" onclick="remove('${id}')" type="button">Remove</button>
            </td>`;
            bottom.parentNode.insertBefore(row,bottom);
        }
    });
}

function handleRemove(response){
    redirect("clients.html");
}

function remove(clientId){
    var message = document.getElementById('message').innerHTML;
    if (confirm(message.replace("{}",clientId))) {
        fetch(client_controller+"/delete",{
            method: 'DELETE',
            body : JSON.stringify({ client_id : clientId }),
            credentials:'include'
        }).then(handleRemove);
    }
}

fetch(client_controller+"/list",{method:'POST',credentials:'include'}).then(handleClients);