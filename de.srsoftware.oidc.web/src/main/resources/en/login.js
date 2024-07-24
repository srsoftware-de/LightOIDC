async function handleLogin(response){
    if (response.ok){
        var body = await response.json();
        hide('error');
        setTimeout(doRedirect,100);
    } else {
        show('error');
    }
}

function doRedirect(){
        let params = new URL(document.location.toString()).searchParams;
        redirect( params.get("return_to") || 'index.html');
        return false;
}

function tryLogin(){
    var username = getValue('username');
    var password = getValue('password');
    fetch(user_controller+"/login",{
        method: 'POST',
        headers: {
          Accept: 'application/json',
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            username : username,
            password : password
        })
    }).then(handleLogin);
    return false;
}

function keyDown(ev){
   if (event.keyCode == 13) tryLogin();
}