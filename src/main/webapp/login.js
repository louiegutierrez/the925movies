function handleLoginResult(resultData) {
    console.log("handle login response");
    console.log(resultData);

    // If login succeeds, redirect the user to browse.html
    if (resultData["status"] === "success") {
        window.location.replace("browse.html");
        alert("Logged in Successfully");

    } else {
        // If login fails, display the error message
        console.log("show error message");
        console.log(resultData["message"]);
        alert(resultData["message"]);
        $("#login_error_message").text(resultData["message"]);
    }
}

function submitLoginForm(formSubmitEvent) {
    console.log("submit login form");
    formSubmitEvent.preventDefault();

    let email = $("#email").val();
    let password = $("#password").val();

    console.log("Email:", email);
    console.log("Password:", password);

    $.ajax({
        url: "api/login",
        method: "POST",
        data: $("#login_form_element").serialize(),
        success: handleLoginResult,
        error: function (jqXHR, textStatus, errorThrown) {
            console.error("Login request failed:", textStatus, errorThrown);
            $("#login_error_message").text("Login failed. Please try again.");
        }
    });
}

function createLoginForm() {
    let loginFormElement = $("#login_form");
    let form = $("<form>", { id: "login_form_element" });

    form.append(
        $("<div>", { class: "form-group" }).append(
            $("<label>", { for: "email", text: "Email" }),
            $("<input>", { type: "email", id: "email", name: "email", class: "form-control", required: "true" })
        ),
        $("<div>", { class: "form-group" }).append(
            $("<label>", { for: "password", text: "Password" }),
            $("<input>", { type: "password", id: "password", name: "password", class: "form-control", required: "true" })
        )
    );

    let button = $("<button>", {
        text: "Submit",
        type: "submit",
        id: "submitButton"
    });

    form.append(button);
    loginFormElement.append(form);

    form.submit(submitLoginForm);
}

$(document).ready(function () {
    createLoginForm();
});