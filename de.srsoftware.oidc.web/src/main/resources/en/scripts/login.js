function doRedirect(){
    let params = new URL(document.location.toString()).searchParams;
    redirect( params.get("return_to") || 'index.html');
    return false;
}

function handleLogin(response){
    hideAll('warn');
    if (response.ok){ 
        response.headers.forEach(function(val, key) {
            console.log('header: '+key+' → '+val);
            // in newer browsers, the cookie is set from fetch response. In older browsers this does not seem to work
            if (key == 'session') {
                val = 'sessionToken='+val;
                console.log('setting cookie: '+val);
                document.cookie = val;
            }
        });
       response.json().then(body => setTimeout(doRedirect,100));
    } else {
        response.json().then(json => {
            if (json.metadata.release) get('release').innerHTML = new Date(json.metadata.release).toLocaleString();
            show(json.error);
        });
    }
}

function keyDown(ev){
   if (event.keyCode == 13) tryLogin();
}

function resetPw(){
    var user = getValue('username');
    if (!user) {
        show('bubble');
        return;
    }
    hide('bubble');
    disable('resetBtn');
    setText('resetBtn','sending…');
    fetch(user_controller+"/reset?user="+user).then(() => {
        hide('login');
        show('sent');
    });
}

function tryLogin(){
    var username = getValue('username');
    var password = getValue('password');
    var trust = !get('trust').checked;
    fetch(user_controller+"/login",{
        method: 'POST',
        headers: {
          Accept: 'application/json',
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            username : username,
            password : password,
            trust: trust
        })
    }).then(handleLogin);
    return false;
}
