var api = "/api";
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

function setText(id, text){
    get(id).innerHTML = text;
}


function setValue(id,newVal){
    document.getElementById(id).value = newVal;
}

