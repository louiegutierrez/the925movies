function getParameterByName(target) {
    let url = window.location.href;
    target = target.replace(/[\[\]]/g, "\\$&");

    let regex = new RegExp("[?&]" + target + "(=([^&#]*)|&|#|$)"),
        results = regex.exec(url);
    if (!results) return null;
    if (!results[2]) return '';

    return decodeURIComponent(results[2].replace(/\+/g, " "));
}

// ids & names not mapped properly
// does not show all movies

function handleStarResult(resultData) {
    console.log("handleStarResult: populating star table from resultData");
    console.log(resultData);
    let singleStarNameElement = jQuery("#single_star_name");
    let singleStarBirthElement = jQuery("#single_star_birth");

    singleStarNameElement.append($(`<h1>${resultData[0]['name']}</h1>`));
    singleStarBirthElement.append($(`<h2>Year of Birth: ${resultData[0]['birth_year']}</h2>`));

    let starTableBodyElement = jQuery("#single_star_table");
    for (let i = 0; i < resultData.length; i++) {
        let movieId = resultData[i]["movie_id"];
        let movieTitle = resultData[i]["movie_title"];
        let rowHTML = "";
        rowHTML += "<tr>";
        rowHTML += `<td><a href="single-movie.html?id=${movieId}">${movieTitle}</a></td>`;
        rowHTML += "</tr>";
        starTableBodyElement.append(rowHTML);
    }
}

let starId = getParameterByName('id');
console.log(starId);
jQuery.ajax({
    dataType: "json",
    method: "GET",
    url: "api/star?id=" + starId,
    success: (resultData) => handleStarResult(resultData)
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