$(document).ready(function () {
    // Add a click event listener to the logout button (with id="logout")
    $("#logout").click(function () {
        console.log("Logout button clicked");

        // Send a GET request to the /api/logout endpoint
        $.ajax({
            url: "api/logout", // Ensure this matches your LogoutServlet URL pattern
            method: "GET",
            success: function (data, textStatus, jqXHR) {
                console.log("Server session invalidated. Clearing localStorage...");
                localStorage.clear();
                window.location.replace("login.html");
            },
            error: function (jqXHR, textStatus, errorThrown) {
                console.error("Logout failed:", textStatus, errorThrown);

                if (jqXHR.status === 401) {
                    // If the user is already logged out, redirect to login page
                    window.location.replace("login.html");
                } else {
                    alert("Logout failed. Please try again.");
                }
            }
        });
    });
});
