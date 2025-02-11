function handleLoginResult(resultData) {
    console.log("handle login response", resultData);

    if (resultData["status"] === "success") {
        alert("Logged in Successfully");
        window.location.replace("browse.html");
    } else {
        alert(resultData["message"]);
        $("#login_error_message").text(resultData["message"]);
    }
}

function submitLoginForm(event) {
    event.preventDefault(); // Prevent default form submission

    let email = $("#email").val();
    let password = $("#password").val();
    let recaptchaResponse = grecaptcha.getResponse(); // Get reCAPTCHA response

    if (!recaptchaResponse) {
        alert("Please complete the reCAPTCHA verification.");
        return;
    }

    console.log("Submitting login form with reCAPTCHA");

    $.ajax({
        url: "api/login",
        method: "POST",
        data: {
            email: email,
            password: password,
            "g-recaptcha-response": recaptchaResponse
        },
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
            $("<input>", { type: "email", id: "email", name: "email", class: "form-control", required: true })
        ),
        $("<div>", { class: "form-group" }).append(
            $("<label>", { for: "password", text: "Password" }),
            $("<input>", { type: "password", id: "password", name: "password", class: "form-control", required: true })
        ),
        $("<div>", { class: "g-recaptcha", "data-sitekey": "6LfpZtMqAAAAALDMnx3Frw2kDYGYIvslpdAyaTCy" }),
        $("<button>", { text: "Submit", type: "submit", id: "submitButton" })
    );

    loginFormElement.append(form);
    form.submit(submitLoginForm);
}

$(document).ready(function () {
    createLoginForm();
});
