function addUser(){
    var pw = getValue('pw');
    var pw2 = getValue('pw2');
    if (pw != pw2) {
        show('pw-mismatch');
        return;
    }
    var msg = {
        username : getValue('username'),
        realname : getValue('realname'),
        email : getValue('email'),
        password : pw
    };
    fetch(user_controller+"/add",{
        method:'POST',
        header: {
            'Content-Type':'application/json'
        },
        body: JSON.stringify(msg)
    }).then(() => location.reload())
}

async function handleUsers(response){
    if (response.status == UNAUTHORIZED) {
        redirect('login.html?return_to='+encodeURI(window.location.href))
        return;
    }
    var users = await response.json();
    var bottom = document.getElementById('bottom');
    for (let id in users){
        var row = document.createElement("tr");
        var u = users[id];
        row.innerHTML = `<td>${u.username}</td>
        <td>${u.realname}</td>
        <td>${u.email}</td>
        <td>${id}</td>
        <td>
            <button type="button" onclick="reset_password('${id}')" id="reset-${id}">Reset password</button>
            <button id="remove-${u.uuid}" class="danger" onclick="remove('${id}','${u.realname}')" type="button">Remove</button>
        </td>`;
        bottom.parentNode.insertBefore(row,bottom);
    }
}

async function handleRemove(response){
    if (response.ok){
        redirect("users.html");
    } else {
        var info = await response.text();
        console.log(info);
        show(info);
    }

}

function remove(userId,name){
    disable(`remove-${userId}`);
    if (userId == user.uuid) {
        //return;
    }
    setText(`remove-${userId}`,"sent…");
    hideAll('error');
    var message = document.getElementById('message').innerHTML;
    if (confirm(message.replace("{}",name))) {
        fetch(user_controller+"/delete",{
            method: 'DELETE',
            body : JSON.stringify({ user_id : userId, confirmed : true })
        }).then(handleRemove);
    }
}

function reset_password(userid){
    fetch(user_controller+"/reset?user="+userid).then(() => {
        disable('reset-'+userid);
    });
}

fetch(user_controller+"/list",{method:'POST'}).then(handleUsers);
