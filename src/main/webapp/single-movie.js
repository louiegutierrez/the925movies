function getParameterByName(target) {
    let url = window.location.href;
    target = target.replace(/[\[\]]/g, "\\$&");

    let regex = new RegExp("[?&]" + target + "(=([^&#]*)|&|#|$)"),
        results = regex.exec(url);
    if (!results) return null;
    if (!results[2]) return '';

    return decodeURIComponent(results[2].replace(/\+/g, " "));
}

function handleMovieResult(resultData) {
    console.log("handleStarResult: populating star table from resultData");
    console.log(resultData);
    let singleMovieTitleElement = jQuery("#single_movie_title");
    let singleMovieYearElement = jQuery("#single_movie_year");
    let singleMovieDirectorElement = jQuery("#single_movie_director");
    let singleMovieGenreElement = jQuery("#single_movie_genre");
    let singleMovieRatingElement = jQuery("#single_movie_rating");

    singleMovieTitleElement.append($(`<h1>${resultData[0]['title']}</h1>`));
    singleMovieYearElement.append($(`<h2>Made in ${resultData[0]['year']}</h2>`));
    singleMovieDirectorElement.append($(`<h2>Directed by ${resultData[0]['director']}</h2>`));
    singleMovieGenreElement.append($(`<h2>Genre(s): ${resultData[0]['all_genres']}</h2>`));
    singleMovieRatingElement.append($(`<h2>Rating: ${resultData[0]['rating']}</h2>`));


    let starTableBodyElement = jQuery("#single_movie_table");
    let star_names = resultData[0]['all_star_names'].split(", ");
    let star_ids = resultData[0]['all_star_ids'].split(", ");

    for (let i = 0; i < star_ids.length; i++) {
        console.log(star_ids[i]);
        console.log(star_names[i]);
        let star_id = star_ids[i];
        let star_name = star_names[i];

        let rowHTML = "";
        rowHTML += "<tr>";
        let star_string = `<th> <a href="single-star.html?id=${star_id}">${star_name}</a></th>`;
        rowHTML += star_string;
        rowHTML += "</tr>";
        starTableBodyElement.append(rowHTML);
    }
    let cartButton = jQuery("#cartButton");
    cartButton.append(`<button class="addToCart refresh" data-movie-id="${resultData[0]['movie_id']}">Add To Cart</button>`);

}

let movieId = getParameterByName('id');
jQuery.ajax({
    dataType: "json",
    method: "GET",
    url: "api/movie?id=" + movieId,
    success: (resultData) => handleMovieResult(resultData)
});

document.addEventListener("DOMContentLoaded", function() {
    let lastQuery = localStorage.getItem("lastQueryString");
    console.log("Retrieved lastQueryString =>", lastQuery);

    if (lastQuery) {
        let backLink = document.getElementById("back");
        if (backLink) {
            // If lastQuery is "?genre=Drama&page=2", then final link is "movie-list.html?genre=Drama&page=2"
            backLink.href = "movie-list.html" + lastQuery;
        }
    }
});