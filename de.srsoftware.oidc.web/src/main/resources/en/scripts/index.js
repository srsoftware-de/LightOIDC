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
        var lastLetter = null;
        for (let id in clients){
            var client = clients[id];
            if (client.landing_page){
                var initialLetter = client.name.charAt(0).toUpperCase();
                if (initialLetter != lastLetter) {
                    if (lastLetter) content.append(document.createElement("br"));
                    lastLetter = initialLetter;
                }
                var span = document.createElement("span");
                span.innerHTML = `<button onclick="window.location.href='${client.landing_page}';">${client.name}</button>`;
                content.append(span);
                any = true;
            }
        }
        if (any) show('client_hint');
    });
}

document.addEventListener("logged_in", function(event) { // wait until page loaded
    fetch(client_controller+"/dash").then(handleDash)
});
