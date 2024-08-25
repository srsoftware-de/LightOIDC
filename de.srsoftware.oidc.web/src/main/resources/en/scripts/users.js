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
        body: JSON.stringify(msg),
        credentials:'include'
    }).then(() => location.reload())
}

function handleUsers(response){
    if (response.status == UNAUTHORIZED) {
        redirect('login.html?return_to='+encodeURI(window.location.href))
        return;
    }
    response.json().then(users => {
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
    });

}

function handleRemove(response){
    if (response.ok){
        redirect("users.html");
    } else {
        response.text().then(info => {
            console.log(info);
            show(info);
        });
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
            body : JSON.stringify({ user_id : userId, confirmed : true }),
            credentials:'include'
        }).then(handleRemove);
    }
}

function reset_password(userid){
    fetch(user_controller+"/reset?user="+userid,{credentials:'include'}).then(() => { disable('reset-'+userid); });
}

fetch(user_controller+"/list",{method:'POST',credentials:'include'}).then(handleUsers);
