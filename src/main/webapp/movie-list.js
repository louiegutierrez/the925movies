let currentPage = 0;
let currentSize = 0;
let currentSort = 0;

function buildQueryURL() {
    let urlParams = new URLSearchParams(window.location.search);

    // Tokenize title input
    if (urlParams.has("title")) {
        let title = urlParams.get("title").trim();
        if (title.length > 0) {
            urlParams.delete("title");
            let words = title.split(/\s+/);
            words.forEach((word, index) => {
                urlParams.append(`title_word${index + 1}`, word);
            });
        }
    }
    return "api/search?" + urlParams.toString();
}

function handleSearchResult(resultData) {
    currentPage = resultData.page || 1;
    currentSize = resultData.size || 25;
    currentSort = resultData.sort || 7;

    jQuery("#pageSizeSelect").val(currentSize);
    jQuery("#sortSelect").val(currentSort);

    if (currentPage <= 1) {
        jQuery("#prevButton").hide();
    } else {
        jQuery("#prevButton").show();
    }
    if (resultData.movies.length < currentSize) {
        jQuery("#nextButton").hide();
    } else {
        jQuery("#nextButton").show();
    }

    jQuery("#numEntries").text(
        `Showing Results ${(currentPage - 1) * currentSize + 1} - ${currentPage * currentSize}`
    );

    let movieListBodyElement = jQuery("#movie_table_body");
    movieListBodyElement.empty();
    if (resultData.movies.length === 0) {
        jQuery("#na").text("No results found.");
        jQuery("#nextButton").hide();
        return;
    }
    jQuery("#na").text("");
    for (let i = 0; i < resultData.movies.length; i++) {
        let m = resultData.movies[i];
        let rowHTML = "<tr>";
        rowHTML += `<td><a href="single-movie.html?id=${m.movie_id}">${m.title}</a></td>`;
        rowHTML += `<td>${m.year}</td>`;
        rowHTML += `<td>${m.director}</td>`;
        let genreName = m.three_genres.split(", ");
        let genresHTML = genreName.map(genre_name => `<a href="movie-list.html?genre=${genre_name}">${genre_name}</a>`).join(", ");
        rowHTML += `<td>${genresHTML}</td>`;
        let starNames = m.three_stars.split(", ");
        let starIds = m.three_star_ids.split(", ");
        let starsHTML = starNames.map((name, j) => `<a href="single-star.html?id=${starIds[j]}">${name}</a>`).join(", ");
        rowHTML += `<td>${starsHTML}</td>`;
        rowHTML += `<td>${m.rating}</td>`;
        rowHTML += `<td><button class="addToCart" data-movie-id="${m.movie_id}">Add to Cart</button></td>`;
        rowHTML += "</tr>";
        movieListBodyElement.append(rowHTML);
    }
}

function fetchMovieList() {
    let url = buildQueryURL();

    // Show a loading spinner
    jQuery("#loadingIndicator").show();
    jQuery("#movie_table").hide();

    jQuery.ajax({
        dataType: "json",
        method: "GET",
        url: url,
        success: (resultData) => {
            handleSearchResult(resultData);

            // Hide spinner, show table
            jQuery("#loadingIndicator").hide();
            jQuery("#movie_table").show();
        },
        error: (xhr, status, error) => {
            console.log("Error fetching:", status, error);
            jQuery("#loadingIndicator").hide();
            jQuery("#movie_table").show();
        }
    });
}

function storeCurrentQueryInLocalStorage() {
    // e.g. if the current browser URL is "movie-list.html?genre=Drama&page=2"
    // we want to store "?genre=Drama&page=2"
    let currentSearch = window.location.search;
    localStorage.setItem("lastQueryString", currentSearch);
}

function initializePage() {
    let urlParams = new URLSearchParams(window.location.search);
    if (urlParams.has("page")) {
        let p = parseInt(urlParams.get("page"));
        if (!isNaN(p) && p > 0) {
            currentPage = p;
        }
    }
    if (urlParams.has("size")) {
        let s = parseInt(urlParams.get("size"));
        if ([10, 25, 50, 100].includes(s)) {
            currentSize = s;
        }
    }
    if (urlParams.has("sort")) {
        let so = parseInt(urlParams.get("sort"));
        if (so >= 1 && so <= 8) {
            currentSort = so;
        }
    }
    fetchMovieList();

    // After fetching, store the final query in localStorage
    storeCurrentQueryInLocalStorage();
}

jQuery(() => {
    initializePage();

    jQuery("#pageSizeSelect").change(function() {
        currentSize = parseInt(jQuery(this).val());
        currentPage = 1;
        let urlParams = new URLSearchParams(window.location.search);
        urlParams.set("size", currentSize);
        urlParams.set("page", currentPage);
        window.history.replaceState({}, "", "movie-list.html?" + urlParams.toString());
        fetchMovieList();
        storeCurrentQueryInLocalStorage();
    });

    jQuery("#sortSelect").change(function() {
        currentSort = parseInt(jQuery(this).val());
        currentPage = 1;
        let urlParams = new URLSearchParams(window.location.search);
        urlParams.set("sort", currentSort);
        urlParams.set("page", currentPage);
        window.history.replaceState({}, "", "movie-list.html?" + urlParams.toString());
        fetchMovieList();
        storeCurrentQueryInLocalStorage();
    });

    jQuery("#prevButton").click(function() {
        if (currentPage > 1) {
            currentPage--;
            let urlParams = new URLSearchParams(window.location.search);
            urlParams.set("page", currentPage);
            window.history.replaceState({}, "", "movie-list.html?" + urlParams.toString());
            fetchMovieList();
            storeCurrentQueryInLocalStorage();
        }
    });

    jQuery("#nextButton").click(function() {
        currentPage++;
        let urlParams = new URLSearchParams(window.location.search);
        urlParams.set("page", currentPage);
        window.history.replaceState({}, "", "movie-list.html?" + urlParams.toString());
        fetchMovieList();
        storeCurrentQueryInLocalStorage();
    });
});
