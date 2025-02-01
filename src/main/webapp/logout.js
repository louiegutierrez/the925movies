$(document).ready(function () {
    // Add a click event listener to the logout button
    $("#logout").click(function () {
        console.log("Logout button clicked");

        // Send a GET request to the /api/logout endpoint
        $.ajax({
            url: "api/logout", // Ensure this matches the URL pattern in LogoutServlet
            method: "GET",
            success: function (data, textStatus, jqXHR) {
                // Redirect to the login page after successful logout
                window.location.replace("login.html");
            },
            error: function (jqXHR, textStatus, errorThrown) {
                console.error("Logout failed:", textStatus, errorThrown);
                alert("Logout failed. Please try again.");
            }
        });
    });
});