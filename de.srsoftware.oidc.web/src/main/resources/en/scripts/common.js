var api = "/api";
var client_controller = "/api/client"
var user_controller = "/api/user"
var web = "/web";

const UNAUTHORIZED = 401;

function get(id){
    return document.getElementById(id);
}

function disable(id){
    get(id).setAttribute('disabled','disabled');
}

function enable(id){
    get(id).removeAttribute('disabled');
}

function getValue(id){
    return get(id).value;
}

function hide(id){
    var elem = get(id);
    if (elem) elem.style.display = 'none';
}

function hideAll(clazz){
    var elems = document.getElementsByTagName('*'), i;
    for (i in elems) {
        if((' ' + elems[i].className + ' ').indexOf(' ' + clazz + ' ') > -1)  elems[i].style.display = 'none';
    }
}

function isChecked(id){
    return get(id).checked;
}

function login(){
    redirect('login.html?return_to='+encodeURIComponent(window.location.href));
}

// Replacement for Object.toEntries(…)
function paramsToObject(entries) {
  const result = {};
  for(var key of entries) { // each 'entry' is a [key, value] tupple
    result[key[0]] = key[1];
  }
  return result;
}

function redirect(page){
    window.location.href = page;
}

function setText(id, text){
    get(id).innerHTML = text;
}


function setValue(id,newVal){
    var elem = get(id);
    if (elem) elem.value = newVal;
}

function show(id){
    var elem = get(id);
    if (elem) elem.style.display = '';
}
