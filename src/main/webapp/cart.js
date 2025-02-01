let cartMovies = $("#cart-movies");
//
function handleSessionData(resultDataJson) {
    console.log("handle session response");
    console.log(resultDataJson);

    $("#sessionID").text("Session ID: " + resultDataJson["sessionID"]);
    $("#lastAccessTime").text("Last access time: " + resultDataJson["lastAccessTime"]);

    cartMovies.empty();
    Object.keys(resultDataJson.quantities).forEach(movieId => {
        let quantity = resultDataJson.quantities[movieId];
        let price = resultDataJson.prices[movieId];

        let rowHTML = `<tr>
            <td>${resultDataJson["names"][movieId]}</td>
            <td>${quantity}</td>
            <td>$${(price* quantity).toFixed(2) }</td>
            <td>
                <button class="addToCart refresh" data-movie-id="${movieId}">Add</button>
                <button class="removeFromCart refresh" data-movie-id="${movieId}">Remove</button>
            </td>
        </tr>`;
        $("#cartTotal").text("Total: $" + resultDataJson["total"].toFixed(2));
        cartMovies.append(rowHTML);
    });
}
//
// function handleCartArray(resultArray) {
//     console.log(resultArray);
//     let item_list = $("#item_list");
//     // change it to html list
//     let res = "<ul>";
//     for (let i = 0; i < resultArray.length; i++) {
//         res += "<li>" + resultArray[i] + "</li>";
//     }
//     res += "</ul>";
//
//     item_list.html("");
//     item_list.append(res);
// }
//
// function handleCartInfo(cartEvent) {
//     console.log("submit cart form");
//     cartEvent.preventDefault();
//
//     $.ajax("api/index", {
//         method: "POST",
//         data: cart.serialize(),
//         success: resultDataString => {
//             let resultDataJson = JSON.parse(resultDataString);
//             handleCartArray(resultDataJson["previousMovies"]);
//         }
//     });  
//
//     cart[0].reset();
// }
//



$(document).on("click", ".addToCart", function () {
    let movieId = $(this).data("movie-id");
    console.log("WOW! Movie ID:", movieId);
    $.ajax({
        url: "api/cart",
        method: "POST",
        data: {
            quantity: 1,
            movieId: movieId
        },
        success: function (response) {
            console.log("Movie added to cart:", response);
            alert("Movie added to cart");
        },
        error: function (jqXHR, textStatus, errorThrown) {
            console.error("Error adding movie to cart:", textStatus, errorThrown);
            console.error("Error adding movie to cart");
        }
    });
});

$(document).on("click", ".removeFromCart", function () {
    let movieId = $(this).data("movie-id");
    console.log("WOW! Movie ID:", movieId);
    $.ajax({
        url: "api/cart",
        method: "POST",
        data: {
            quantity: -1,
            movieId: movieId
        },
        success: function (response) {
            console.log("Movie added to cart:", response);
            alert("Movie added to cart");
        },
        error: function (jqXHR, textStatus, errorThrown) {
            console.error("Error adding movie to cart:", textStatus, errorThrown);
            console.error("Error adding movie to cart");
        }
    });
});

$(document).on("click", ".refresh", function() {
    console.log("Refresh button clicked");
    updateCart();
});


function updateCart(){
    $.ajax("api/cart", {
        method: "GET",
        success: handleSessionData,
        error: () => {
            console.log("Failure");
        }
    });
}

updateCart();