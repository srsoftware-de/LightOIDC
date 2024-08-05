var user = null;
async function handleUser(response){
    if (response.status == UNAUTHORIZED) {
        login();
        return;
    }
    if (response.ok){
        user = await response.json();
        fetch(web+"/navigation.html").then(handleNavigation);
    }
}

async function handleNavigation(response){
    if (response.ok){
        var content = await response.text();
        var nav = document.getElementsByTagName('nav')[0];
        nav.innerHTML = content;
        var links = nav.getElementsByTagName('a');
        for (var index = links.length; index > 0; index--){
            var link = links[index-1];
            var clazz = link.hasAttribute('class') ? link.getAttribute("class") : null;
            if (clazz != null && !user.permissions.includes(clazz)) nav.removeChild(link);
        }
    }
}

fetch(user_controller+"/",{method:'POST'}).then(handleUser);