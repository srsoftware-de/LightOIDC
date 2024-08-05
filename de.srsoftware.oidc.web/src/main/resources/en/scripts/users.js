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
        var user = users[id];
        row.innerHTML = `<td>${user.username}</td>
        <td>${user.realname}</td>
        <td>${user.email}</td>
        <td>${id}</td>
        <td>
            <button type="button" onclick="reset_password('${id}')" id="reset-${id}">Reset password</button>
            <button class="danger" onclick="remove('${id}')" type="button">Remove</button>
        </td>`;
        bottom.parentNode.insertBefore(row,bottom);
    }
}

function handleRemove(response){
    redirect("users.html");
}

function remove(userId){
    var message = document.getElementById('message').innerHTML;
    if (confirm(message.replace("{}",userId))) {
        fetch(user_controller+"/delete",{
            method: 'DELETE',
            body : JSON.stringify({ userId : userId })
        }).then(handleRemove);
    }
}

function reset_password(userid){
    fetch(user_controller+"/reset",{
        method: 'POST',
        body:userid
    }).then(() => {
        disable('reset-'+userid);
    });
}

fetch(user_controller+"/list",{method:'POST'}).then(handleUsers);