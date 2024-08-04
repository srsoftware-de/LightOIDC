function fillForm(){
    if (user == null){
        setTimeout(fillForm,100);
    } else {
        console.log(user);
        setValue('username',user.username);
        setValue('email',user.email);
        setValue('uuid', user.uuid);
    }
}


function handleResponse(response){
    if (response.ok){
        hide('update_error')
        setText('updateBtn', 'saved.');
    } else {
        show('update_error');
        setText('updateBtn', 'Update failed!');
    }
    enable('updateBtn');
    setTimeout(function(){
        setText('updateBtn','Update');
    },10000);
}

function update(){
    disable('updateBtn');
    var newData = {
        username : getValue('username'),
        email : getValue('email'),
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

function passKeyDown(ev){
   if (event.keyCode == 13) updatePass();
}

setTimeout(fillForm,100);