function handleDash(response){
    if (response.status == UNAUTHORIZED) {
        redirect('login.html?return_to='+encodeURI(window.location.href))
        return;
    }
    var clients = response.json().then(data => {
        var name = data.name;
        var welcome = get('welcome');
        welcome.innerHTML = welcome.innerHTML.replace('{}',name);

        var clients = data.authorized;
        var content = document.getElementById('content');
        var any = false;
        for (let id in clients){
            var client = clients[id];
                if (client.landing_page){
                var div = document.createElement("div");
                div.innerHTML = `<button onclick="window.location.href='${client.landing_page}';">${client.name}</button>`;
                content.append(div);
                any = true;
            }
        }
        if (any) show('client_hint');
    });
}

fetch(client_controller+"/dash").then(handleDash)