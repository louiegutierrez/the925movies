$(document).ready(function () {
    // Insert Star Form
    let insertStarForm = $("<form>", { id: "insert_star_form" }).append(
        $("<h2>", { text: "Insert a New Star" }),
        $("<div>", { class: "form-group" }).append(
            $("<label>", { for: "star_name", text: "Star Name (required)" }),
            $("<input>", { type: "text", id: "star_name", name: "star_name", class: "form-control", required: true })
        ),
        $("<div>", { class: "form-group" }).append(
            $("<label>", { for: "birth_year", text: "Birth Year (optional)" }),
            $("<input>", { type: "number", id: "birth_year", name: "birth_year", class: "form-control" })
        ),
        $("<button>", { text: "Add Star", type: "submit", class: "btn btn-primary" })
    );

    $("#insert_star").append(insertStarForm);

    // Add Movie Form
    let addMovieForm = $("<form>", { id: "add_movie_form" }).append(
        $("<h2>", { text: "Add a Movie" }),
        $("<div>", { class: "form-group" }).append(
            $("<label>", { for: "movie_title", text: "Movie Title (required)" }),
            $("<input>", { type: "text", id: "movie_title", name: "movie_title", class: "form-control", required: true })
        ),
        $("<div>", { class: "form-group" }).append(
            $("<label>", { for: "release_year", text: "Release Year (required)" }),
            $("<input>", { type: "number", id: "release_year", name: "release_year", class: "form-control", required: true})
        ),
        $("<div>", { class: "form-group" }).append(
            $("<label>", { for: "director", text: "Director (required)" }),
            $("<input>", { type: "text", id: "director", name: "director", class: "form-control", required: true })
        ),
        $("<div>", { class: "form-group" }).append(
            $("<label>", { for: "star_name_movie", text: "Star Name (required)" }),
            $("<input>", { type: "text", id: "star_name_movie", name: "star_name_movie", class: "form-control", required: true})
        ),
        $("<div>", { class: "form-group" }).append(
            $("<label>", { for: "genre_name", text: "Genre (required)" }),
            $("<input>", { type: "text", id: "genre_name", name: "genre_name", class: "form-control", required: true})
        ),
        $("<button>", { text: "Add Movie", type: "submit", class: "btn btn-primary" })
    );

    $("#add_movie").append(addMovieForm);

    // Metadata Section
    let metadataSection = $("<div>").append(
        $("<h2>", { text: "Database Metadata" }),
        $("<button>", { text: "Fetch Metadata", id: "fetch_metadata", class: "btn btn-secondary" }),
        $("<div>", { id: "metadata_output" })
    );

    $("#metadata").append(metadataSection);

    // Event Handlers
    $("#insert_star_form").submit(function (event) {
        event.preventDefault();
        let starName = $("#star_name").val().trim();
        let birthYear = $("#birth_year").val().trim();

        $.ajax({
            url: "api/insert_star",
            method: "POST",
            data: { star_name: starName, birth_year: birthYear },
            success: function (response) {
                alert(response.message);
                $("#add_movie_form")[0].reset();
            },
            error: function (jqXHR, textStatus, errorThrown) {
                console.error("Insert star failed:", textStatus, errorThrown);
                alert("Failed to add star. Please try again.");
            }
        });
    });

    $("#add_movie_form").submit(function (event) {
        event.preventDefault();
        let movieTitle = $("#movie_title").val().trim();
        let releaseYear = $("#release_year").val().trim();
        let director = $("#director").val().trim();
        let starName = $("#star_name_movie").val().trim();
        let genreName = $("#genre_name").val().trim();

        $.ajax({
            url: "api/add_movie",
            method: "POST",
            data: { title: movieTitle, year: releaseYear, director: director, star_name: starName, genre_name: genreName },
            success: function (response) {
                alert(response.message);
                $("#insert_star_form")[0].reset();
            },
            error: function (jqXHR, textStatus, errorThrown) {
                console.error("Add movie failed:", textStatus, errorThrown);
                alert("Failed to add movie. Please try again.");
            }
        });
    });

    $("#fetch_metadata").click(function () {
        $.ajax({
            url: "api/get_metadata",
            method: "GET",
            success: function (response) {
                let metadataHtml = "<ul>";
                response.tables.forEach(table => {
                    metadataHtml += `<li><strong>${table.name}</strong><ul>`;
                    table.columns.forEach(column => {
                        metadataHtml += `<li>${column.name}: ${column.type}</li>`;
                    });
                    metadataHtml += "</ul></li>";
                });
                metadataHtml += "</ul>";
                $("#metadata_output").html(metadataHtml);
            },
            error: function (jqXHR, textStatus, errorThrown) {
                console.error("Metadata fetch failed:", textStatus, errorThrown);
                alert("Failed to fetch metadata.");
            }
        });
    });
});

document.addEventListener("DOMContentLoaded", function() {
    let lastQuery = localStorage.getItem("lastQueryString");
    console.log("Retrieved lastQueryString =>", lastQuery);

    if (lastQuery) {
        let backLink = document.getElementById("back");
        if (backLink) {
            backLink.href = "movie-list.html" + lastQuery;
        }
    }
});
