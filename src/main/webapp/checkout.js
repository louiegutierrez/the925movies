$(document).ready(function () {
    $.ajax({
        url: "api/cart",
        method: "GET",
        error: function () {
            console.log("Failed to fetch total price.");
        }
    });

    $("#paymentForm").submit(function (event) {
        event.preventDefault();
        console.log("Form submitted via AJAX");
        console.log($(this).serialize());
        $.ajax({
            url: "api/payment",
            method: "POST",
            data: $(this).serialize(),
            success: function (response) {
                console.log("SUCCESS!", response);
            },
            error: function (jqXHR, textStatus, errorThrown) {
                console.error("Payment request failed:", textStatus, errorThrown);
                $("#message").text("An error occurred. Please try again.").css("color", "red");
            }
        });
    });
});
