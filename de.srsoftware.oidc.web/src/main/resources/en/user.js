var user = null;
async function handleUser(response){
    if (response.status == UNAUTHORIZED) {
        window.location.href = 'login.html?return_to='+encodeURI(window.location.href);
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
        for (var index = 0; index < links.length; index++){
            var link = links[index];
            var clazz = link.hasAttribute('class') ? link.getAttribute("class") : null;
            if (clazz != null && !user.permissions.includes(clazz)) nav.removeChild(link);
        }
    }
}

fetch(api+"/user",{method:'POST'}).then(handleUser);