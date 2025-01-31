let letters = '0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ*'.split('')
let genres = []

function handleGenreResult(resultData){
    populate_letters();
    populate_genres(resultData);
}

function populate_letters() {
    let lettersElement = jQuery("#letters");
    let buttons = letters.map(function (letter) {
        return $("<button>")
            .text(letter)
            .addClass("btn btn-primary m-2") // Add Bootstrap classes
            .on("click", function () {
                // Redirect to movie-list.html, with ?letter=...
                window.location.href = "movie-list.html?letter=" + letter;
            });
    });
    lettersElement.append(buttons);
}

function populate_genres(resultData) {
    console.log(resultData);
    let genresElement = jQuery("#genres");
    let buttons = resultData.map(function (genre) {
        return $("<button>")
            .text(genre["name"])
            .addClass("btn btn-success m-2") // Add Bootstrap classes
            .on("click", function () {
                // Redirect to movie-list.html, with ?genre=...
                window.location.href = "movie-list.html?genre=" + genre["name"];
            });
    });
    genresElement.append(buttons);
}

jQuery.ajax({
    dataType: "json",
    method: "GET",
    url: "api/genres",
    success: (resultData) => handleGenreResult(resultData)
});