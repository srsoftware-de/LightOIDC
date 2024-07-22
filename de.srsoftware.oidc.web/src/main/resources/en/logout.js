function handleLogout(response){
    if (response.ok) document.body.innerHTML += 'success';
    redirect('index.html')
}
fetch(api+"/logout").then(handleLogout)