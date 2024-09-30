var session_duration_minutes = 10;

function fillForm(){
    if (user == null){
        setTimeout(fillForm,100);
    } else {
        setValue('username',user.username);
        setValue('email',user.email);
        setValue('uuid', user.uuid);
        setValue('realname', user.realname);
        session_duration_minutes = user.session_duration | 10;
        displayDuration();

    }
}
 function handlePasswordResponse(response){
    if (response.ok){
        hide('wrong_password');
        hide('password_mismatch');
        setText('passBtn', 'saved.');
    } else {
        setText('passBtn', 'Update failed!');
        response.text().then(text => {
            if (text == 'wrong password') show('wrong_password');
            if (text == 'password mismatch') show('password_mismatch');
        });
    }
    enable('passBtn');
    setTimeout(function(){
        setText('passBtn','Update');
    },10000);
}

function handleSmtpResponse(response){
    if (response.ok){
        hide('wrong_password');
        hide('password_mismatch');
        setText('smtpBtn', 'saved.');
    } else {
        setText('smtpBtn', 'Update failed!');
        response.text().then(text => {
            if (text == 'wrong password') show('wrong_password');
            if (text == 'password mismatch') show('password_mismatch');
        });

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

function handleMailSettings(response){
    console.log('handleSettings(…)',response);
    if (response.ok){
        response.json().then(json => {
            for (var key in json) setValue(key,json[key]);
            get('start_tls').checked = json.start_tls;
            get('smtp_auth').checked = json.smtp_auth;
            show('mail_settings');
        });
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
        body : JSON.stringify(newData),
        credentials:'include'
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
        body : JSON.stringify(newData),
        credentials:'include'
    }).then(handlePasswordResponse);
    setText('passBtn','sent…');
}



function update(){
    disable('updateBtn');
    var newData = {
        username : getValue('username'),
        email : getValue('email'),
        realname : getValue('realname'),
        uuid : getValue('uuid'),
        session_duration : session_duration_minutes
    }
    fetch(user_controller+'/update',{
        method : 'POST',
        headers : {
           'Content-Type': 'application/json'
        },
        body : JSON.stringify(newData),
        credentials:'include'
    }).then(handleResponse)
    setText('updateBtn','sent…');
}

function displayDuration(){
    var mins = session_duration_minutes;
    hrs = Math.floor(mins/60);
    mins-=60*hrs;
    days = Math.floor(hrs/24);
    hrs-=24*days;
    setText('days',days);
    setText('hours',hrs);
    setText('minutes',mins);
}

function durationUpdate(){
    var raw = getValue('session_duration');
    var mins = 0;
    var hrs = 0;
    var days = 0;
    if (raw<30){
        mins = raw;
    } else if(raw<37) {
        mins=5*(raw-24);
    } else if(raw<57){
        mins=15*(raw-32);
    } else if(raw<75){
        mins=60*(raw-50);
    } else {
        mins=60*24*(raw-73);
    }
    session_duration_minutes = mins;
    displayDuration();
}


document.addEventListener("logged_in", function(event) { // wait until page loaded
    fillForm();
    fetch("/api/email/settings",{credentials:'include'}).then(handleMailSettings);
});