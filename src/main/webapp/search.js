$(document).ready(function () {
    $("#searchForm").on("submit", function (event) {
        event.preventDefault();

        let formData = {
            title: $("input[name='title']").val(),
            year: $("input[name='year']").val(),
            director: $("input[name='director']").val(),
            star: $("input[name='star']").val()
        };

        let queryString = $.param(formData);

        window.location.href = "movie-list.html?" + queryString;
    });
});
