function handleLogout(response){
    if (response.ok){
    document.body.innerHTML += 'success';
    document.location.href='index.html';
    }
}
fetch(api+"/logout").then(handleLogout)