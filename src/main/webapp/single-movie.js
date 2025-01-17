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
    singleMovieTitleElement.append($(`<h1>${resultData[0]['title']}</h1>`));

    let starTableBodyElement = jQuery("#single_movie_table");
    let rowHTML = "";
    rowHTML += "<tr>";

    rowHTML += `<th> ${resultData[0]['year']} </th>`;
    rowHTML += `<th> ${resultData[0]['director']} </th>`;
    rowHTML += `<th> ${resultData[0]['all_genres']} </th>`;

    let star_names = resultData[0]['all_star_names'].split(", ");
    let star_ids = resultData[0]['all_star_ids'].split(", ");

    let star_string =
        "<th>" +
        star_names.map((name, j) =>
            `<a href="single-star.html?id=${star_ids[j]}">${name}</a>`
        ).join(", ") +
        "</th>";

    rowHTML += star_string;
    rowHTML += `<th> ${resultData[0]['rating']} </th>`;

    rowHTML += "</tr>";

    starTableBodyElement.append(rowHTML);
}

let movieId = getParameterByName('id');
jQuery.ajax({
    dataType: "json",
    method: "GET",
    url: "api/movie?id=" + movieId,
    success: (resultData) => handleMovieResult(resultData)
});