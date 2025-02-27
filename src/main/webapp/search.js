$(document).ready(function () {
    $("#searchForm").on("submit", function (event) {
        event.preventDefault();

        let formData = getFormData();
        let queryString = $.param(formData);
        window.location.href = "movie-list.html?" + queryString;
    });
});

function getFormData(){
    return {
        title: $("input[name='title']").val(),
        year: $("input[name='year']").val(),
        director: $("input[name='director']").val(),
        star: $("input[name='star']").val()
    };
}

/*
 * CS 122B Project 4. Autocomplete Example.
 *
 * This Javascript code uses this library: https://github.com/devbridge/jQuery-Autocomplete
 *
 * This example implements the basic features of the autocomplete search, features that are
 *   not implemented are mostly marked as "TODO" in the codebase as a suggestion of how to implement them.
 *
 * To read this code, start from the line "$('#autocomplete').autocomplete" and follow the callback functions.
 *
 */


/*
 * This function is called by the library when it needs to lookup a query.
 *
 * The parameter query is the query string.
 * The doneCallback is a callback function provided by the library, after you get the
 *   suggestion list from AJAX, you need to call this function to let the library know.
 */
function handleLookup(query, doneCallback) {
    console.log("autocomplete initiated")
    console.log("sending AJAX request to backend Java Servlet")

    // TODO: if you want to check past query results first, you can do it here

    // sending the HTTP GET request to the Java Servlet endpoint hero-suggestion
    // with the query data
    let formData = getFormData();
    let queryString = $.param(formData);
    console.log(queryString);
        // title=good%20h&year=&director=&star=
        // title=good%20h&year=&director=&star=

    jQuery.ajax({
        "method": "GET",
        "url": "api/search?" + queryString,
        "success": function(data) {
            handleLookupAjaxSuccess(data, query, doneCallback)
        },
        "error": function(errorData) {
            console.log("lookup ajax error")
            console.log(errorData)
        }
    })
}


/*
 * This function is used to handle the ajax success callback function.
 * It is called by our own code upon the success of the AJAX request
 *
 * data is the JSON data string you get from your Java Servlet
 *
 */
function handleLookupAjaxSuccess(data, query, doneCallback) {
    console.log("lookup ajax successful")
    var jsonData = (typeof data === "string") ? JSON.parse(data) : data;

    var suggestions = jsonData["movies"].map(movie => ({
        value: movie.title ? movie.title.toString() : "Unknown Title", // Ensure value is a string
        data: movie
    })).slice(0,10);

    console.log(suggestions);

    doneCallback({ suggestions: suggestions });
}


/*
 * This function is the select suggestion handler function.
 * When a suggestion is selected, this function is called by the library.
 *
 * You can redirect to the page you want using the suggestion data.
 */
function handleSelectSuggestion(suggestion) {
    console.log(suggestion);
    console.log("you select " + suggestion["value"] + " with ID " + suggestion["data"]["movie_id"])
    window.location.href = "single-movie.html?id="+suggestion["data"]["movie_id"]

}

/*
 * This statement binds the autocomplete library with the input box element and
 *   sets necessary parameters of the library.
 *
 * The library documentation can be find here:
 *   https://github.com/devbridge/jQuery-Autocomplete
 *   https://www.devbridge.com/sourcery/components/jquery-autocomplete/
 *
 */
// $('#autocomplete') is to find element by the ID "autocomplete"
$('#autocomplete').autocomplete({
    // documentation of the lookup function can be found under the "Custom lookup function" section
    minChars: 3,

    lookup: function (query, doneCallback) {
        handleLookup(query, doneCallback)
    },
    onSelect: function(suggestion) {
        handleSelectSuggestion(suggestion)
    },
    // set delay time
    deferRequestBy: 300,
    // TODO: add other parameters, such as minimum characters
});


/*
 * do normal full text search if no suggestion is selected
 */
function handleNormalSearch(query) {
    console.log("doing normal search with query: " + query);
    // TODO: you should do normal search here
}

// bind pressing enter key to a handler function
$('#autocomplete').keypress(function(event) {
    // keyCode 13 is the enter key
    if (event.keyCode == 13) {
        // pass the value of the input box to the handler function
        handleNormalSearch($('#autocomplete').val())
    }
})

// TODO: if you have a "search" button, you may want to bind the onClick event as well of that button


