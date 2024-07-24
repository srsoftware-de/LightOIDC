function handleLogout(response){
    if (response.ok) document.body.innerHTML += 'success';
    redirect('index.html')
}
fetch(user_controller+"/logout").then(handleLogout)