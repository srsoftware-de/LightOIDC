async function handleLogin(response){
    if (response.ok){
        var body = await response.json();

        setTimeout(doRedirect,100);
    }
    return false;
}

function doRedirect(){
        let params = new URL(document.location.toString()).searchParams;
        redirect( params.get("return_to") || 'index.html');
        return false;
}

function tryLogin(){
    document.getElementById("error").innerHTML = "";
    var username = getValue('username');
    var password = getValue('password');
    fetch(api+"/login",{
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