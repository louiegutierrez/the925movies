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
    singleStarNameElement.append($(`<h1>${resultData[0]['name']}</h1>`));
    //
    let starTableBodyElement = jQuery("#single_star_table");
    let rowHTML = "";
    rowHTML += "<tr>";
    rowHTML += `<th> ${resultData[0]['birth_year']} </th>`;
    rowHTML += `<th> <a href="single-movie.html?id=${resultData[0]['movie_id']}"> ${resultData[0]['movie_title']} </a> </th>`;
    // let movie_string =
    //     "<th>" +
    //     resultData[0]['movie_titles'].map((name, j) =>
    //         `<a href="single-star.html?id=${resultData[0]['movie_ids'][j]}">${name}</a>`
    //     ).join(", ") +
    //     "</th>";

    // rowHTML += movie_string;

    rowHTML += "</tr>";
    starTableBodyElement.append(rowHTML);
}

let starId = getParameterByName('id');
console.log(starId);
jQuery.ajax({
    dataType: "json",
    method: "GET",
    url: "api/star?id=" + starId,
    success: (resultData) => handleStarResult(resultData)
});