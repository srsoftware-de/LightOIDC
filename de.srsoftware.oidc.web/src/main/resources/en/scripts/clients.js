function edit(clientId){
    redirect("edit_client.html?id="+clientId);
}

function handleClients(response){
    if (response.status == UNAUTHORIZED) {
        redirect('login.html?return_to='+encodeURI(window.location.href))
        return;
    }
    var clients = response.json().then(clients => {
        var arr = [];
        for (let id in clients) arr.push(clients[id]);
        arr.sort((a,b) => a.name < b.name ? -1 : 1);
        var bottom = document.getElementById('bottom');
        for (let client of arr){
            var row = document.createElement("tr");
            row.innerHTML = `<td>${client.name}</td>
            <td>${client.client_id}</td>
            <td>${client.redirect_uris.join("<br/>")}</td>
            <td>${link(client.landing_page)}</td>
            <td>
                <button type="button" onclick="edit('${client.client_id}')">Edit</button>
                <button class="danger" onclick="remove('${client.client_id}')" type="button">Remove</button>
            </td>`;
            bottom.parentNode.insertBefore(row,bottom);
        }
    });
}

function link(url){
    return url ? '<a href="'+url+'" target="_blank">'+url.split('?')[0]+'</a>': "";
}

function handleRemove(response){
    redirect("clients.html");
}

function remove(clientId){
    var message = document.getElementById('message').innerHTML;
    if (confirm(message.replace("{}",clientId))) {
        fetch(client_controller,{
            method: 'DELETE',
            body : JSON.stringify({ client_id : clientId }),
            credentials:'include'
        }).then(handleRemove);
    }
}
document.addEventListener("logged_in", function(event) { // wait until page loaded
    fetch(client_controller+"/list").then(handleClients);
});