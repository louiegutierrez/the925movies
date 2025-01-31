let letters = [0, 1, 2, 3, 4, 5, 6 ,7 ,8, 9, 'A', 'B', 'C', 'Z', '*']
let genres = []

function handleGenreResult(resultData){
    populate_letters();
    populate_genres(resultData);
}

function populate_genres(resultData){
    console.log(resultData);
    let genresElement = jQuery("#genres");
    let buttons = resultData.map(function (genre) {
        return $("<button>")
            .text(genre["name"])
            .on("click", function () {
                window.location.href = "movie-list.html?" + "browse/" + genre["name"];
                // console.log("Button clicked:", letter.name);
            });
    });
    genresElement.append(buttons);
}

function populate_letters(){
    let lettersElement = jQuery("#letters");
    let buttons = letters.map(function (letter) {
        return $("<button>")
            .text(letter)
            .on("click", function () {
                window.location.href = "movie-list.html?" + "browse/" + letter;
                console.log("Button clicked:", letter);
            });
    });
    lettersElement.append(buttons);
}

jQuery.ajax({
    dataType: "json",
    method: "GET",
    url: "api/genres",
    success: (resultData) => handleGenreResult(resultData)
});