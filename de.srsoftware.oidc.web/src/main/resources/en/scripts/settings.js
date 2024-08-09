function fillForm(){
    if (user == null){
        setTimeout(fillForm,100);
    } else {
        console.log(user);
        setValue('username',user.username);
        setValue('email',user.email);
        setValue('uuid', user.uuid);
        setValue('realname', user.realname);
    }
}

async function handlePasswordResponse(response){
    if (response.ok){
        hide('wrong_password');
        hide('password_mismatch');
        setText('passBtn', 'saved.');
    } else {
        setText('passBtn', 'Update failed!');
        var text = await response.text();
        if (text == 'wrong password') show('wrong_password');
        if (text == 'password mismatch') show('password_mismatch');

    }
    enable('passBtn');
    setTimeout(function(){
        setText('passBtn','Update');
    },10000);
}

async function handleSmtpResponse(response){
    if (response.ok){
        hide('wrong_password');
        hide('password_mismatch');
        setText('smtpBtn', 'saved.');
    } else {
        setText('smtpBtn', 'Update failed!');
        var text = await response.text();
        if (text == 'wrong password') show('wrong_password');
        if (text == 'password mismatch') show('password_mismatch');

    }
    setTimeout(function(){
        enable('smtpBtn');
        setText('smtpBtn','Update');
    },10000);
}

function handleResponse(response){
    if (response.ok){
        hide('update_error')
        setText('updateBtn', 'saved.');
    } else {
        show('update_error');
        setText('updateBtn', 'Update failed!');
    }
    setTimeout(function(){
        enable('updateBtn');
        setText('updateBtn','Update');
    },10000);
}

async function handleSettings(response){
    console.log('handleSettings(…)',response);
    if (response.ok){
        var json = await response.json();
        for (var key in json){
            setValue(key,json[key]);
        }
        get('start_tls').checked = json.start_tls;
        get('smtp_auth').checked = json.smtp_auth;
        show('mail_settings');
    } else {
      hide('mail_settings');
    }
}

function passKeyDown(ev){
   if (event.keyCode == 13) updatePass();
}

function updateSmtp(){
    disable('smtpBtn');
    var newData = {
        smtp_host : getValue('smtp_host'),
        smtp_port : getValue('smtp_port'),
        smtp_user : getValue('smtp_user'),
        smtp_pass : getValue('smtp_pass'),
        smtp_auth : isChecked('smtp_auth'),
        start_tls : isChecked('start_tls')
    }
    fetch("/api/email/settings",{
        method : 'POST',
        headers : {
           'Content-Type': 'application/json'
        },
        body : JSON.stringify(newData)
    }).then(handleSmtpResponse);
    setText('smtpBtn','sent…');
}


function updatePass(){
    disable('passBtn');
    var newData = {
        oldpass : getValue('oldpass'),
        newpass : [getValue('newpass1'),getValue('newpass2')],
        uuid : getValue('uuid')
    }
    fetch(user_controller+'/password',{
        method : 'POST',
        headers : {
           'Content-Type': 'application/json'
        },
        body : JSON.stringify(newData)
    }).then(handlePasswordResponse);
    setText('passBtn','sent…');
}



function update(){
    disable('updateBtn');
    var newData = {
        username : getValue('username'),
        email : getValue('email'),
        realname : getValue('realname'),
        uuid : getValue('uuid')
    }
    fetch(user_controller+'/update',{
        method : 'POST',
        headers : {
           'Content-Type': 'application/json'
        },
        body : JSON.stringify(newData)
    }).then(handleResponse)
    setText('updateBtn','sent…');
}

setTimeout(fillForm,100);
fetch("/api/email/settings").then(handleSettings);
