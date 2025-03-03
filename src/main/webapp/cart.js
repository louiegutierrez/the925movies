let cartMovies = $("#cart_table_body"); // Change this to target the <tbody>

function handleSessionData(resultDataJson) {

    cartMovies.empty();
    let cartTotal = resultDataJson["total"] || 0; // Ensure cartTotal is defined

    if (cartTotal === 0) {
        $("#cartTotal").text("Total: $0.00");
        $("#checkout").prop("disabled", true).addClass("disabled"); // Disable checkout button
        return;
    }

    $("#checkout").prop("disabled", false).removeClass("disabled"); // Enable checkout button
    $("#cartTotal").text("Total: $" + cartTotal.toFixed(2));

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
                <button class="btn btn-danger btn-sm deleteFromCart" data-movie-id="${movieId}" data-quantity="${quantity}"> DELETE </button>
            </td>
        </tr>`;
        cartMovies.append(rowHTML);
    });
}


$(document).on("click", ".addToCart", function () {
    let movieId = $(this).data("movie-id");
    $.ajax({
        url: "api/cart",
        method: "POST",
        data: {
            quantity: 1,
            movieId: movieId
        },
        success: function (response) {
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
    $.ajax({
        url: "api/cart",
        method: "POST",
        data: {
            quantity: -1,
            movieId: movieId
        },
        success: function (response) {
            alert("Movie removed to cart");
            updateCart();
        },
        error: function (jqXHR, textStatus, errorThrown) {
            console.error("Error adding movie to cart:", textStatus, errorThrown);
            console.error("Error adding movie to cart");
        }
    });
});

$(document).on("click", ".deleteFromCart", function () {
    let movieId = $(this).data("movie-id");
    let quantity = $(this).data("quantity");
    $.ajax({
        url: "api/cart",
        method: "POST",
        data: {
            quantity: -quantity,
            movieId: movieId
        },
        success: function (response) {
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

document.addEventListener("DOMContentLoaded", function() {
    let lastQuery = localStorage.getItem("lastQueryString");

    if (lastQuery) {
        let backLink = document.getElementById("back");
        if (backLink) {
            backLink.href = "movie-list.html" + lastQuery;
        }
    }
});