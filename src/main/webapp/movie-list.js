function handleStarResult(resultData) {
    console.log("handleStarResult: populating starsssss table from resultData");
    console.log(resultData);

    const entries_per_page = 20;
    const len = Math.min(resultData.length, entries_per_page);

    let movieListBodyElement = jQuery("#movie_table_body");

    if (len === 0) {
        console.log("n/a");
        jQuery("#na").text("N/A, no results found");
        jQuery("#movie_table_body").empty();
        return;
    }

    for (let i = 0; i < len; i++) {
        let rowHTML = "";
        rowHTML += "<tr>";

        rowHTML += `<th> <a href="single-movie.html?id=${resultData[i]['movie_id']}">${resultData[i]['title']}</a> </th>`;
        rowHTML += `<th> ${resultData[i]['year']} </th>`;
        rowHTML += `<th> ${resultData[i]['director']} </th>`;
        rowHTML += `<th> ${resultData[i]['three_genres']} </th>`;

        let star_names = resultData[i]['three_stars'].split(", ");
        let star_ids = resultData[i]['three_star_ids'].split(", ");

        let star_string =
            "<th>" +
                star_names.map((name, j) =>
                    `<a href="single-star.html?id=${star_ids[j]}">${name}</a>`
                ).join(", ") +
            "</th>";

        rowHTML += star_string;
        rowHTML += `<th> ${resultData[i]['rating']} </th>`;

        rowHTML += "</tr>";
        movieListBodyElement.append(rowHTML);
    }
}


const urlParams = new URLSearchParams(window.location.search);
console.log(urlParams);
if(urlParams.size === 0){
    console.log("default");
    jQuery.ajax({
        dataType: "json",
        method: "GET",
        url: "api/movielist",
        success: (resultData) => handleStarResult(resultData)
    });
} else {
    console.log("url");
    jQuery.ajax({
        dataType: "json",
        method: "GET",
        url: "api/search",
        success: (resultData) => handleStarResult(resultData)
    });
}

