const UNAUTHORIZED = 401;

function handleUser(response){
    console.log(response);
}

fetch(api+"/user").then(handleUser);