$(document).ready(function () {
    $("#searchForm").on("submit", function (event) {
        event.preventDefault();

        let formData = getFormData();
        let queryString = $.param(formData);
        window.location.href = "movie-list.html?" + queryString;
    });
});

function getFormData() {
    let titleInput = $("input[name='title']").val().trim();
    let titleWords = titleInput.split(/\s+/).filter(word => word.length > 0); // Tokenize input

    let formData = {
        year: $("input[name='year']").val(),
        director: $("input[name='director']").val(),
        star: $("input[name='star']").val()
    };

    // Add each word as title_word1, title_word2, etc.
    titleWords.forEach((word, index) => {
        formData[`title_word${index + 1}`] = word;
    });

    return formData;
}

/*
 * Handle Autocomplete Lookup
 */
function handleLookup(query, doneCallback) {
    console.log("Autocomplete initiated with query:", query);

    let formData = getFormData(); // Tokenized query
    let queryString = $.param(formData);

    console.log("Query sent to backend:", queryString);

    jQuery.ajax({
        method: "GET",
        url: "api/search?" + queryString,
        success: function (data) {
            handleLookupAjaxSuccess(data, query, doneCallback);
        },
        error: function (errorData) {
            console.log("Lookup AJAX error:", errorData);
        }
    });
}

/*
 * Handle AJAX Success and Format Suggestions
 */
function handleLookupAjaxSuccess(data, query, doneCallback) {
    console.log("Lookup AJAX successful");

    var jsonData = (typeof data === "string") ? JSON.parse(data) : data;

    var suggestions = jsonData["movies"].map(movie => ({
        value: movie.title ? movie.title.toString() : "Unknown Title",
        data: movie
    })).slice(0, 10); // Limit to 10 results

    console.log("Using suggestions:", suggestions);

    doneCallback({ suggestions: suggestions });
}

/*
 * Handle Selection of Suggestion
 */
function handleSelectSuggestion(suggestion) {
    console.log("Selected:", suggestion);
    console.log("Redirecting to single movie page with ID:", suggestion["data"]["movie_id"]);

    // Update input field
    $('#autocomplete').val(suggestion.value);

    // Redirect to single movie page
    window.location.href = "single-movie.html?id=" + suggestion["data"]["movie_id"];
}

/*
 * Initialize Autocomplete with All Required Features
 */
$('#autocomplete').autocomplete({
    minChars: 3, // Do not perform search unless at least 3 characters are entered
    lookup: function (query, doneCallback) {
        handleLookup(query, doneCallback);
    },
    onSelect: function (suggestion) {
        handleSelectSuggestion(suggestion);
    },
    deferRequestBy: 300, // Wait 300ms after typing stops before making a request
    preserveInput: true, // Maintain input text when navigating suggestions
    showNoSuggestionNotice: true, // Show "No suggestions found" if no results
    noSuggestionNotice: "No results found" // Custom message
});

/*
 * Handle Normal Search When Enter Key is Pressed Without Selecting a Suggestion
 */
function handleNormalSearch(query) {
    console.log("Performing normal search with query:", query);
    if (query.length >= 3) {
        window.location.href = "movie-list.html?title=" + encodeURIComponent(query);
    }
}

// Bind "Enter" key event to normal search when no selection is made
$('#autocomplete').keypress(function (event) {
    if (event.keyCode === 13) { // Enter key
        handleNormalSearch($('#autocomplete').val());
    }
});
