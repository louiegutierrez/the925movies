$(document).ready(function () {
    $.ajax({
        url: "api/cart",
        method: "GET",
        success: function (response) {
            let total = response.total.toFixed(2);
            $("#totalPrice").text("Total: $" + total);

            if (parseFloat(total) === 0) {
                $("#submitBtn").prop("disabled", true); // Disable button
            } else {
                $("#submitBtn").prop("disabled", false); // Enable button if there's a total
            }
        },
        error: function () {
            console.log("Failed to fetch total price.");
        }
    });

    $("#paymentForm").submit(function (event) {
        event.preventDefault();

        let totalPriceText = $("#totalPrice").text();
        let totalPriceValue = parseFloat(totalPriceText.replace(/[^0-9.]/g, ''));

        if (totalPriceValue === 0) {
            alert("Cannot submit empty cart");
            return;
        }

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
