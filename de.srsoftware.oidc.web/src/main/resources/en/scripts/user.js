var user = null;

function handleUser(response){
    if (response.status == UNAUTHORIZED) {
        login();
        return;
    }
    if (response.ok){
        response.json().then(u => {
            user = u;
            fetch(web+"/navigation.html",{credentials:'include'}).then(handleNavigation);
            document.dispatchEvent(new Event('logged_in'));
        });
    }
}

function handleNavigation(response){
    if (response.ok){
        response.text().then(content => {
            var nav = document.getElementsByTagName('nav')[0];
            nav.innerHTML = content;
            var links = nav.getElementsByTagName('a');

            for (var index = links.length; index > 0; index--){
                var link = links[index-1];
                var clazz = link.hasAttribute('class') ? link.getAttribute("class") : null;
                if (clazz != null && !user.permissions.includes(clazz)) nav.removeChild(link);
            }
        });
    }
}

document.addEventListener("DOMContentLoaded", function(event) { // wait until page loaded
    fetch(user_controller+"/",{
        method:'POST',
        credentials:'include'
    }).then(handleUser);
});
