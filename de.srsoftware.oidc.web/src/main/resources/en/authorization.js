var params = new URLSearchParams(window.location.search)
var json = Object.fromEntries(params);

fetch(api+"/authorize",{
    method: 'POST',
    body: JSON.stringify(json),
    headers: {
        'Content-Type': 'application/json'
    }
})

