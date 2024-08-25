const urlParams = new URLSearchParams(window.location.search);
const token = urlParams.get('token');

function handlePasswordResponse(response){
    if (response.ok){
        setText('passBtn', 'saved.');
        if (response.redirected){
          redirect(response.url);
        }
    } else {
        setText('passBtn', 'Update failed!');
        response.text().then(text => {
            if (text == 'invalid token') show('invalid_token');
            if (text == 'token missing') show('missing_token');
            if (text == 'password mismatch') show('password_mismatch');
            if (text == 'weak password') show('weak_password');
        });
    }
    enable('passBtn');
    setTimeout(function(){
        setText('passBtn','Update');
    },10000);
}

function passKeyDown(ev){
   if (event.keyCode == 13) updatePass();
}

function updatePass(){
    disable('passBtn');
    hide('missing_token');
    hide('invalid_token');
    hide('password_mismatch');
    hide('weak_password');
    var newData = {
        newpass : [getValue('newpass1'),getValue('newpass2')],
        token : token
    }
    fetch(user_controller+'/reset',{
        method : 'POST',
        headers : {
           'Content-Type': 'application/json'
        },
        body : JSON.stringify(newData),
        credentials:'include'
    }).then(handlePasswordResponse);
    setText('passBtn','sent…');
}

function missingToken(){
    show('missing_token');
    disable('passBtn');
}
if (!token) setTimeout(missingToken,100);
