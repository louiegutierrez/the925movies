// Global state for pagination
let currentPage = 1;
let currentSize = 25;
let currentSort = 7;

function buildQueryURL() {

    let urlParams = new URLSearchParams(window.location.search);

    urlParams.set("page", currentPage);
    urlParams.set("size", currentSize);
    urlParams.set("sort", currentSort);

    return "api/search?" + urlParams.toString();
}

function handleStarResult(resultData) {
    console.log("handleStarResult: populating table from resultData", resultData);

    if (currentPage <= 1) {
        jQuery("#prevButton").hide();
    } else {
        jQuery("#prevButton").show();
    }

    if (resultData.length < currentSize) {
        jQuery("#nextButton").hide();
    } else {
        jQuery("#nextButton").show();
    }
    jQuery("#numEntries").text(`Showing Results ${(currentPage - 1) * currentSize + 1} - ${currentPage * currentSize}`);
    let movieListBodyElement = jQuery("#movie_table_body");
    // Clear any old table rows
    movieListBodyElement.empty();

    if (resultData.length === 0) {
        console.log("No results found for this page.");
        jQuery("#na").text("No results found.");
        // Possibly hide Next button
        jQuery("#nextButton").hide();
        return;
    }

    jQuery("#na").text("");

    for (let i = 0; i < resultData.length; i++) {
        let rowHTML = "<tr>";
        rowHTML += `<td><a href="single-movie.html?id=${resultData[i]['movie_id']}">${resultData[i]['title']}</a></td>`;
        rowHTML += `<td>${resultData[i]['year']}</td>`;
        rowHTML += `<td>${resultData[i]['director']}</td>`;
        rowHTML += `<td>${resultData[i]['three_genres']}</td>`;

        let starNames = resultData[i]['three_stars'].split(", ");
        let starIds = resultData[i]['three_star_ids'].split(", ");
        let starsHTML = starNames.map((name, j) =>
            `<a href="single-star.html?id=${starIds[j]}">${name}</a>`
        ).join(", ");
        rowHTML += `<td>${starsHTML}</td>`;

        rowHTML += `<td>${resultData[i]['rating']}</td>`;

        rowHTML += `<td> <button> Add to Cart </button>  </td>`;

        rowHTML += "</tr>";
        movieListBodyElement.append(rowHTML);
    }
}

function fetchMovieList() {
    let url = buildQueryURL();
    console.log("Fetching from:", url);

    let queryPart = url.split("?")[1]; // everything after the "?"
    history.replaceState({}, "", "movie-list.html?" + queryPart);

    jQuery.ajax({
        dataType: "json",
        method: "GET",
        url: url,
        success: (resultData) => handleStarResult(resultData),
        error: (xhr, status, error) => {
            console.log("Error fetching:", status, error);
        }
    });
}

function initializePage() {
    const urlParams = new URLSearchParams(window.location.search);

    // If "page" param is present, parse it
    if (urlParams.has("page")) {
        currentPage = parseInt(urlParams.get("page")) || 1;
    }
    if (currentPage < 1) currentPage = 1;

    // If "size" param is present
    if (urlParams.has("size")) {
        let s = parseInt(urlParams.get("size"));
        if ([10,25,50,100].includes(s)) {
            currentSize = s;
        }
    }

    // If "sort" param is present
    if (urlParams.has("sort")) {
        let so = parseInt(urlParams.get("sort"));
        if ([1,2,3,4,5,6,7,8].includes(so)) {
            currentSort = so;
        }
    }

    jQuery("#pageSizeSelect").val(currentSize);
    jQuery("#sortSelect").val(currentSort);

    fetchMovieList();
}

jQuery(() => {
    initializePage();

    jQuery("#pageSizeSelect").change(function() {
        console.log("pageSizeSelect changed! New value=", jQuery(this).val());
        currentSize = parseInt(jQuery(this).val());
        currentPage = 1;
        fetchMovieList();
    });

    jQuery("#sortSelect").change(function() {
        console.log("sortSelect changed! New value=", jQuery(this).val());
        currentSort = parseInt(jQuery(this).val());
        currentPage = 1;
        fetchMovieList();
    });

    jQuery("#prevButton").click(function() {
        console.log("prevButton changed! New value=", jQuery(this).val());
        if (currentPage > 1) {
            currentPage--;
            fetchMovieList();
        }
    });
    jQuery("#nextButton").click(function() {
        console.log("nextButton changed! New value=", jQuery(this).val());
        currentPage++;
        fetchMovieList();
    });
});
