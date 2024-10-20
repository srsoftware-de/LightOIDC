function editPermission(userId,permission,drop){
    fetch(user_controller+"/permission",{
        method : drop ? 'DELETE' : 'POST',
        credentials:'include',
        body : JSON.stringify({ user_id : userId, permission : permission }),
    }).then(handleEdit);
}

function handleEdit(response){
    redirect('users.html');
}

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
        var arr = [];
        for (let id in users) arr.push(users[id]);
        arr.sort((a,b) => a.username < b.username ? -1 : 1);
        var bottom = document.getElementById('bottom');
        for (var u of arr){
            var row = document.createElement("tr");
            //var u = users[id];
            var manage = {
                clients : u.permissions.includes('MANAGE_CLIENTS'),
                perms : u.permissions.includes('MANAGE_PERMISSIONS'),
                smtp : u.permissions.includes('MANAGE_SMTP'),
                users : u.permissions.includes('MANAGE_USERS')
            };
            row.innerHTML = `<td>${u.username}</td>
            <td>${u.realname}</td>
            <td>${u.email}</td>
            <td>${u.uuid}</td>
            <td style="display: none" class="permissions">
                <button onclick="editPermission('${u.uuid}','MANAGE_CLIENTS',${manage.clients})">${manage.clients ?'-':'+'} <span>Manage</span> Clients</button>
                <button onclick="editPermission('${u.uuid}','MANAGE_PERMISSIONS',${manage.perms})">${manage.perms ?'-':'+'} <span>Manage</span> Permissions</button>
                <button onclick="editPermission('${u.uuid}','MANAGE_SMTP',${manage.smtp})">${manage.smtp ?'-':'+'} <span>Manage</span> SMTP</button>
                <button onclick="editPermission('${u.uuid}','MANAGE_USERS',${manage.users})">${manage.users ?'-':'+'} <span>Manage</span> Users</button>
            </td>
            <td>
                <button type="button" onclick="reset_password('${u.uuid}')" id="reset-${u.uuid}">Reset password</button>
                <button id="remove-${u.uuid}" class="danger" onclick="remove('${u.uuid}','${u.realname}')" type="button">Remove</button>
            </td>`;
            bottom.parentNode.insertBefore(row,bottom);
            if (user.permissions.includes('MANAGE_PERMISSIONS')) showAll('permissions');
        }
    });

}

function handleRemove(response){
    if (response.ok){
        redirect("users.html");
    } else {
        response.text().then(info => {
            show(info);
        });
    }
}

function remove(userId,name){
    disable(`remove-${userId}`);
    if (userId == user.uuid) {
        return;
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

document.addEventListener("logged_in", function(event) { // wait until page loaded
       fetch(user_controller+"/list",{method:'POST',credentials:'include'}).then(handleUsers);
});

