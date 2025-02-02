let cartMovies = $("#cart_table_body"); // Change this to target the <tbody>

function handleSessionData(resultDataJson) {
    console.log("handle session response");
    console.log(resultDataJson);

    cartMovies.empty();
    if(Object.keys(resultDataJson["names"]).length === 0) {
        $("#cartTotal").text("Total: $0.00");
        return;
    }
    Object.keys(resultDataJson.quantities).forEach(movieId => {
        let quantity = resultDataJson.quantities[movieId];
        let price = resultDataJson.prices[movieId];

        let rowHTML = `<tr>
            <td>${resultDataJson["names"][movieId]}</td>
            <td>${quantity}</td>
            <td>$${(price * quantity).toFixed(2)}</td>
            <td>
                <button class="btn btn-success btn-sm addToCart" data-movie-id="${movieId}">+1</button>
                <button class="btn btn-danger btn-sm removeFromCart" data-movie-id="${movieId}">-1</button>
            </td>
        </tr>`;
        $("#cartTotal").text("Total: $" + resultDataJson["total"].toFixed(2));
        cartMovies.append(rowHTML);
    });


}


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
            updateCart();
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
            console.log("Movie removed to cart:", response);
            alert("Movie removed to cart");
            updateCart();
        },
        error: function (jqXHR, textStatus, errorThrown) {
            console.error("Error adding movie to cart:", textStatus, errorThrown);
            console.error("Error adding movie to cart");
        }
    });
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