function handleStarResult(resultData) {
    console.log("handleStarResult: populating star table from resultData");
    console.log(resultData);
    let starTableBodyElement = jQuery("#movie_table_body");

    //map first 20 entries
    for (let i = 0; i < Math.min(20, resultData.length); i++) {
        let rowHTML = "";
        rowHTML += "<tr>";

        // rowHTML += `<th> ${resultData[i]['title']} </th>`;
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

        starTableBodyElement.append(rowHTML);
    }
}

jQuery.ajax({
    dataType: "json",
    method: "GET",
    url: "api/movielist",
    success: (resultData) => handleStarResult(resultData)
});