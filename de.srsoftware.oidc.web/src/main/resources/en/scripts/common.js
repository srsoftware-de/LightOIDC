var api = "/api";
var client_controller = "/api/client"
var user_controller = "/api/user"
var web = "/web";

const UNAUTHORIZED = 401;

function get(id){
    return document.getElementById(id);
}

function disable(id){
    get(id).setAttribute('disabled',true);
}

function enable(id){
    get(id).removeAttribute('disabled');
}

function getValue(id){
    return get(id).value;
}

function hide(id){
    get(id).style.display = 'none';
}

function login(){
    redirect('login.html?return_to='+encodeURIComponent(window.location.href));
}

function redirect(page){
    window.location.href = page;
}

function setText(id, text){
    get(id).innerHTML = text;
}


function setValue(id,newVal){
    get(id).value = newVal;
}

function show(id){
    get(id).style.display = '';
}