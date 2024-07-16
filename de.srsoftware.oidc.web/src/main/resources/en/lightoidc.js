const UNAUTHORIZED = 401;

function handleCheckUser(response){
    console.log(window.location.href);
    if (response.status == UNAUTHORIZED){
        window.location.href = "login.html";
        return;
    }
}
function checkUser(){
    fetch(api+"/user")
        .then(handleCheckUser)
        .catch((err) => console.log(err));
}

function handleLogin(response){
    if (response.status == 401){
        loadError("login-failed");
        return;
    }
    console.log(response);
}

function loadError(page){
    fetch(web+"/"+page+".txt").then(resp => resp.text()).then(showError);
}

function showError(content){
      document.getElementById("error").innerHTML = content;
}

function tryLogin(){
    document.getElementById("error").innerHTML = "";
    var data = Object.fromEntries(new FormData(document.getElementById('login')));
    fetch(api+"/login",{
        headers: {
          'login-username': data.user,
          'login-password': data.pass, // TODO: send via body?
          Accept: 'application/json',
          'Content-Type': 'application/json'
        }
    }).then(handleLogin);
}