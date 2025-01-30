function handleLoginResult(resultData) {
    console.log("handle login response");
    console.log(resultData);
    console.log(resultData["status"]);

    // If login succeeds, redirect the user to index.html
    if (resultData["status"] === "success") {
        window.location.replace("index.html");
    } else {
        // If login fails, display the error message
        console.log("show error message");
        console.log(resultData["message"]);
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
        success: handleLoginResult
    });
}

function createLoginForm() {
    let loginFormElement = $("#login_form");
    let form = $("<form>", { id: "login_form_element" });

    form.append(
        $("<label>", { for: "email", text: "Email" }),
        $("<br>"),
        $("<input>", { type: "email", id: "email", name: "email", required: "true" }),
        $("<br>"),
        $("<label>", { for: "password", text: "Password" }),
        $("<br>"),
        $("<input>", { type: "password", id: "password", name: "password", required: "true" })
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