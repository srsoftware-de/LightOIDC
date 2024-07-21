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
    setText('updateBtn','sent…');
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
}

function updatePass(){
    disable('passBtn');
    setText('passBtn','sent…');
    var newData = {
        oldpass : [getValue('oldpass1'),getValue('oldpass2')],
        newpass : getValue('newpass'),
        uuid : getValue('uuid')
    }
    fetch(api+'/update/password',{
        method : 'POST',
        headers : {
           'Content-Type': 'application/json'
        },
        body : JSON.stringify(newData)
    }).then(handlePasswordResponse);

        setTimeout(function(){
            setText('passBtn','Update');
            enable('passBtn');
        },10000);
}

setTimeout(fillForm,100);