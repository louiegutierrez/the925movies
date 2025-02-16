function handleLoginResult(resultData) {
    console.log("handle login response", resultData);

    if (resultData["status"] === "success") {
        let redirectUrl = resultData["role"] === "employee" ? "_dashboard.html" : "browse.html";
        alert(`Logged in successfully as ${resultData["role"]}`);
        window.location.replace(redirectUrl);
    } else {
        alert(resultData["message"] || "Login failed. Please try again.");
    }
}

function submitLoginForm(event) {
    event.preventDefault(); // Prevent default form submission

    let email = $("#email").val().trim();
    let password = $("#password").val().trim();
    let recaptchaResponse = grecaptcha.getResponse(); // Get reCAPTCHA response

    if (!email || !password) {
        alert("Please fill in all fields.");
        return;
    }

    if (!recaptchaResponse) {
        alert("Please complete the reCAPTCHA verification.");
        return;
    }

    $.ajax({
        url: "api/login",
        method: "POST",
        data: {
            email: email,
            password: password,
            "g-recaptcha-response": recaptchaResponse,
        },
        success: handleLoginResult,
        error: function (jqXHR, textStatus, errorThrown) {
            console.error("Login request failed:", textStatus, errorThrown);
            alert("Login request failed. Please check your connection and try again.");
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
        $("<div>", { class: "form-group" }).append(
            $("<div>", { class: "g-recaptcha", "data-sitekey": "6LfpZtMqAAAAALDMnx3Frw2kDYGYIvslpdAyaTCy" })
        ),
        $("<button>", { text: "Login", type: "submit", class: "btn btn-primary" })
    );

    loginFormElement.append(form);

    form.submit(submitLoginForm);
}

$(document).ready(function () {
    createLoginForm();
});
