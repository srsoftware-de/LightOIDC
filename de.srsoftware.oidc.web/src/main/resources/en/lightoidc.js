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
function submitForm(formId){
    var data = Object.fromEntries(new FormData(document.getElementById(formId)));
    fetch(api+"/login",{
        headers: {
          'login-username': data.user,
          'login-password': data.pass, // TODO: send via body?
          Accept: 'application/json',
          'Content-Type': 'application/json'
        }
    });
}