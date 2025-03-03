$(document).ready(function () {

    $("#paymentForm").submit(function (event) {
        event.preventDefault();
        $.ajax({
            url: "api/payment",
            method: "POST",
            data: $(this).serialize(),
            success: function (response) {
                window.location.href = "confirmation.html";
                console.log("SUCCESS!", response);
            },
            error: function (jqXHR, textStatus, errorThrown) {
                console.error("Payment request failed:", textStatus, errorThrown);
                $("#error-message").text("The credit card information is wrong. Please try again.");
            }
        });
    });
});
