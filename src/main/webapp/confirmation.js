$.ajax({
    url: "api/payment",
    method: "GET",
    success: (resultDataJson) => {
        let tableBody = $("#salesTableBody");

        // Loop through sales data and append to table
        resultDataJson["sales"].forEach(sale => {
            let rowHTML = `<tr>
                <td>${sale.saleID}</td>
                <td>${sale.movieTitle}</td>
                <td>${sale.quantity}</td>
            </tr>`;
            tableBody.append(rowHTML);
        });

        // Update total price textbox
        $("#totalPriceBox").val(`$${resultDataJson.totalPrice.toFixed(2)}`);
    },
    error: function () {
        console.log("Failed to fetch total price.");
    }
});
