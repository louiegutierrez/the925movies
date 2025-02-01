let cartMovies = $("#cart-movies");
//
function handleSessionData(resultDataString) {
    let resultDataJson = JSON.parse(resultDataString);

    console.log("handle session response");
    console.log(resultDataJson);
    console.log(resultDataJson["sessionID"]);

    // show the session information
    $("#sessionID").text("Session ID: " + resultDataJson["sessionID"]);
    $("#lastAccessTime").text("Last access time: " + resultDataJson["lastAccessTime"]);


    let previousMovies = resultDataJson["previousMovies"];
    console.log(previousMovies);

    Object.keys(previousMovies).forEach(movieId => {
        let quantity = previousMovies[movieId];
        let rowHTML = `<tr>
            <td>${movieId}</td>
            <td>${quantity}</td>
            <td>$price</td>
            <td>$total</td>
            <td>
                <button class="addButton">Add</button>
                <button class="removeButton">Remove</button>
            </td>
        </tr>`;
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

$.ajax("api/cart", {
    method: "GET",
    success: handleSessionData,
    error: () => {
        console.log("Failure");
    }
});

$(document).on("click", ".addToCart", function () {
    let movieId = $(this).data("movie-id");
    console.log("WOW! Movie ID:", movieId);
    $.ajax({
        url: "api/cart",
        method: "POST",
        data: {
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
