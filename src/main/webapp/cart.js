// let cart = $("#cart");
//
// function handleSessionData(resultDataString) {
//     let resultDataJson = JSON.parse(resultDataString);
//
//     console.log("handle session response");
//     console.log(resultDataJson);
//     console.log(resultDataJson["sessionID"]);
//
//     // show the session information
//     $("#sessionID").text("Session ID: " + resultDataJson["sessionID"]);
//     $("#lastAccessTime").text("Last access time: " + resultDataJson["lastAccessTime"]);
//
//     // show cart information
//     handleCartArray(resultDataJson["previousMovies"]);
// }
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
// $.ajax("api/cart", {
//     method: "GET",
//     success: handleSessionData
// });
//
// cart.submit(handleCartInfo);



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
            alert("Movie added to cart:" + response);
        },
        error: function (jqXHR, textStatus, errorThrown) {
            console.error("Error adding movie to cart:", textStatus, errorThrown);
            console.error("Error adding movie to cart");
        }
    });
});
