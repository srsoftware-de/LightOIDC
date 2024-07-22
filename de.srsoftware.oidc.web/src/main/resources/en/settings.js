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
    setText('updateBtn',response.ok ? 'saved.' : 'failed!');
    setTimeout(function(){
        setText('updateBtn','Update');
        enable('updateBtn');
    },10000);
}

function handlePasswordResponse(response){
    setText('passBtn',response.ok ? 'saved.' : 'failed!');
    setTimeout(function(){
        setText('passBtn','Update');
        enable('passBtn');
    },10000);
}

function update(){
    disable('updateBtn');
    var newData = {
        username : getValue('username'),
        email : getValue('email'),
        uuid : getValue('uuid')
    }
    fetch(api+'/update/user',{
        method : 'POST',
        headers : {
           'Content-Type': 'application/json'
        },
        body : JSON.stringify(newData)
    }).then(handleResponse)
    setText('updateBtn','sent…');
}

function updatePass(){
    disable('passBtn');
    var newData = {
        oldpass : getValue('oldpass'),
        newpass : [getValue('newpass1'),getValue('newpass2')],
        uuid : getValue('uuid')
    }
    fetch(api+'/update/password',{
        method : 'POST',
        headers : {
           'Content-Type': 'application/json'
        },
        body : JSON.stringify(newData)
    }).then(handlePasswordResponse);
    setText('passBtn','sent…');
    setTimeout(function(){
        setText('passBtn','Update');
        enable('passBtn');
    },10000);
}

function passKeyDown(ev){
   if (event.keyCode == 13) updatePass();
}

setTimeout(fillForm,100);