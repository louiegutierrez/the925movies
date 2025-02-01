$(document).ready(function () {
    $.ajax({
        url: "api/cart",
        method: "GET",
        success: function (response) {
            $("#totalPrice").text("Total: $" + response.total.toFixed(2));
        },
        error: function () {
            console.log("Failed to fetch total price.");
        }
    });

    $("#paymentForm").submit(function (event) {
        event.preventDefault();

        $.ajax({
            url: "api/payment",
            method: "POST",
            data: $(this).serialize(),
            success: function (response) {
                if (response.status === "success") {
                    $("#message").text(response.message).css("color", "green");
                } else {
                    $("#message").text(response.errorMessage).css("color", "red");
                }
            },
            error: function (jqXHR, textStatus, errorThrown) {
                $("#message").text("An error occurred. Please try again.").css("color", "red");
            }
        });
    });
});