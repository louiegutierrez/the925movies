import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;

@WebServlet(name = "SearchingServlet", urlPatterns = "/api/search")
public class SearchingServlet extends HttpServlet {
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext()
                    .lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        boolean hasParam = (request.getParameter("title") != null) ||
                (request.getParameter("year") != null) ||
                (request.getParameter("director") != null) ||
                (request.getParameter("star") != null) ||
                (request.getParameter("genre") != null) ||
                (request.getParameter("letter") != null);

        String title, year, director, actor, genre, letter;
        if (hasParam) {
            title = request.getParameter("title") == null ? "" : request.getParameter("title").trim();
            year = request.getParameter("year") == null ? "" : request.getParameter("year").trim();
            director = request.getParameter("director") == null ? "" : request.getParameter("director").trim();
            actor = request.getParameter("star") == null ? "" : request.getParameter("star").trim();
            genre = request.getParameter("genre") == null ? "" : request.getParameter("genre").trim();
            letter = request.getParameter("letter") == null ? "" : request.getParameter("letter").trim();
        } else {
            title = session.getAttribute("title") == null ? "" : session.getAttribute("title").toString();
            year = session.getAttribute("year")  == null ? "" : session.getAttribute("year").toString();
            director = session.getAttribute("director") == null ? "" : session.getAttribute("director").toString();
            actor = session.getAttribute("star") == null ? "" : session.getAttribute("star").toString();
            genre = session.getAttribute("genre") == null ? "" : session.getAttribute("genre").toString();
            letter = session.getAttribute("letter") == null ? "" : session.getAttribute("letter").toString();
        }
        // if genre or letter are not null, then title, year, director, and actor are null
        // you cannot search for a Title Year Director and Actor with a genre/letter
        if (!genre.isEmpty() || !letter.isEmpty()) {
            title = "";
            year = "";
            director = "";
            actor = "";
        } else {
            genre = "";
            letter = "";
        }

        // previousSessionValues
        String prevTitle = session.getAttribute("title") == null ? "" : session.getAttribute("title").toString();
        String prevYear = session.getAttribute("year") == null ? "" : session.getAttribute("year").toString();
        String prevDirector = session.getAttribute("director") == null ? "" : session.getAttribute("director").toString();
        String prevActor = session.getAttribute("star") == null ? "" : session.getAttribute("star").toString();
        String prevGenre = session.getAttribute("genre") == null ? "" : session.getAttribute("genre").toString();
        String prevLetter = session.getAttribute("letter") == null ? "" : session.getAttribute("letter").toString();

        // check values
        boolean searchChanged = false;
        if (hasParam) {
            searchChanged = !title.equals(prevTitle) ||
                    !year.equals(prevYear) ||
                    !director.equals(prevDirector) ||
                    !actor.equals(prevActor) ||
                    !genre.equals(prevGenre) ||
                    !letter.equals(prevLetter);
        }


        // if the previous doesn't match the new, set page, size, sort to default
        // special cases for first if are when session is empty so no previous
        // also when there's no passed parameters so it should just take it from
        // session (if it's both null it'll be top 25 by default)
        int defaultPage = 1, defaultSize = 25, defaultSort = 7;
        int page, size, sortOption;
        if (searchChanged) {
            page = defaultPage;
            size = defaultSize;
            sortOption = defaultSort;
        } else {
            // else page, size, sort should be the ones given by parameter/session
            // special cases for else: if there's no parameter, you should get from session
            // these should only update when the previous session matches current queries, else
            // it should be set to 1, 25, 7 which are the default values

            page = (request.getParameter("page") != null)
                    ? Integer.parseInt(request.getParameter("page"))
                    : (session.getAttribute("page") == null ? defaultPage : (int) session.getAttribute("page"));
            size = (request.getParameter("size") != null)
                    ? Integer.parseInt(request.getParameter("size"))
                    : (session.getAttribute("size") == null ? defaultSize : (int) session.getAttribute("size"));
            sortOption = (request.getParameter("sort") != null)
                    ? Integer.parseInt(request.getParameter("sort"))
                    : (session.getAttribute("sort") == null ? defaultSort : (int) session.getAttribute("sort"));
        }

        // set to session for these queries
        session.setAttribute("title", title);
        session.setAttribute("year", year);
        session.setAttribute("director", director);
        session.setAttribute("star", actor);
        session.setAttribute("genre", genre);
        session.setAttribute("letter", letter);
        session.setAttribute("page", page);
        session.setAttribute("size", size);
        session.setAttribute("sort", sortOption);

        // Print debug
        System.out.println("title=" + title + ", year=" + year
                + ", director=" + director + ", actor=" + actor
                + ", genre=" + genre + ", letter=" + letter
                + ", page=" + page + ", size=" + size + ", sort=" + sortOption);

        // Build base query
        try (Connection conn = dataSource.getConnection()) {
            StringBuilder queryBuilder = new StringBuilder();
            queryBuilder.append("SELECT m.id AS movie_id, m.title, m.year, m.director, ");
            queryBuilder.append("genre_info.genres, ");
            queryBuilder.append("SUBSTRING_INDEX(star_info.star_ids, ', ', 3) AS star_ids, ");
            queryBuilder.append("SUBSTRING_INDEX(star_info.stars, ', ', 3) AS stars, ");
            queryBuilder.append("COALESCE(r.rating, 0.0) AS rating ");
            queryBuilder.append("FROM movies m ");
            queryBuilder.append("LEFT JOIN ratings r ON r.movieId = m.id ");
            queryBuilder.append("LEFT JOIN ( ");
            queryBuilder.append("    SELECT sm.movieId, ");
            queryBuilder.append("           GROUP_CONCAT(s.id ORDER BY star_counts.star_count DESC, s.name ASC SEPARATOR ', ') AS star_ids, ");
            queryBuilder.append("           GROUP_CONCAT(s.name ORDER BY star_counts.star_count DESC, s.name ASC SEPARATOR ', ') AS stars ");
            queryBuilder.append("    FROM stars_in_movies sm ");
            queryBuilder.append("    JOIN stars s ON sm.starId = s.id ");
            queryBuilder.append("    LEFT JOIN ( ");
            queryBuilder.append("         SELECT starId, COUNT(*) AS star_count ");
            queryBuilder.append("         FROM stars_in_movies ");
            queryBuilder.append("         GROUP BY starId ");
            queryBuilder.append("    ) star_counts ON star_counts.starId = s.id ");
            queryBuilder.append("    GROUP BY sm.movieId ");
            queryBuilder.append(") star_info ON star_info.movieId = m.id ");
            queryBuilder.append("LEFT JOIN ( ");
            queryBuilder.append("    SELECT gim.movieId, ");
            queryBuilder.append("           SUBSTRING_INDEX(GROUP_CONCAT(g.name ORDER BY g.name ASC SEPARATOR ', '), ', ', 3) AS genres ");
            queryBuilder.append("    FROM genres_in_movies gim ");
            queryBuilder.append("    JOIN genres g ON gim.genreId = g.id ");
            queryBuilder.append("    GROUP BY gim.movieId ");
            queryBuilder.append(") genre_info ON genre_info.movieId = m.id ");
            queryBuilder.append("WHERE 1=1 ");

            if (!title.isEmpty()) {
                queryBuilder.append("AND m.title LIKE ? ");
            }
            if (!year.isEmpty()) {
                queryBuilder.append("AND m.year = ? ");
            }
            if (!director.isEmpty()) {
                queryBuilder.append("AND m.director LIKE ? ");
            }
            if (!actor.isEmpty()) {
                queryBuilder.append("AND EXISTS (SELECT 1 FROM stars_in_movies sm ");
                queryBuilder.append("JOIN stars s ON sm.starId = s.id ");
                queryBuilder.append("WHERE sm.movieId = m.id AND s.name LIKE ?) ");
            }
            if (!genre.isEmpty()) {
                queryBuilder.append("AND EXISTS (SELECT 1 FROM genres_in_movies gim ");
                queryBuilder.append("JOIN genres g ON gim.genreId = g.id ");
                queryBuilder.append("WHERE gim.movieId = m.id AND g.name LIKE ?) ");
            }
            if (!letter.isEmpty()) {
                if (letter.equals("*")) {
                    queryBuilder.append("AND m.title REGEXP '^[^a-zA-Z0-9]' ");
                } else {
                    queryBuilder.append("AND m.title LIKE ? ");
                }
            }

            // Sorting
            switch (sortOption) {
                case 1:
                    queryBuilder.append("ORDER BY m.title ASC, rating ASC ");
                    break;
                case 2:
                    queryBuilder.append("ORDER BY m.title ASC, rating DESC ");
                    break;
                case 3:
                    queryBuilder.append("ORDER BY m.title DESC, rating ASC ");
                    break;
                case 4:
                    queryBuilder.append("ORDER BY m.title DESC, rating DESC ");
                    break;
                case 5:
                    queryBuilder.append("ORDER BY rating ASC, m.title ASC ");
                    break;
                case 6:
                    queryBuilder.append("ORDER BY rating ASC, m.title DESC ");
                    break;
                case 7:
                    queryBuilder.append("ORDER BY rating DESC, m.title ASC ");
                    break;
                case 8:
                    queryBuilder.append("ORDER BY rating DESC, m.title DESC ");
                    break;
                default:
                    queryBuilder.append("ORDER BY m.title ASC, rating ASC ");
                    break;
            }

            queryBuilder.append("LIMIT ? OFFSET ? ");

            String query = queryBuilder.toString();
            System.out.println(query);
            PreparedStatement statement = conn.prepareStatement(query);

            int paramIndex = 1;

            // Bind all the search params in order
            if (!title.isEmpty()) {
                statement.setString(paramIndex++, "%" + title + "%");
            }
            if (!year.isEmpty()) {
                statement.setInt(paramIndex++, Integer.parseInt(year));
            }
            if (!director.isEmpty()) {
                statement.setString(paramIndex++, "%" + director + "%");
            }
            if (!actor.isEmpty()) {
                statement.setString(paramIndex++, "%" + actor + "%");
            }
            if (!genre.isEmpty()) {
                statement.setString(paramIndex++, "%" + genre + "%");
            }
            if (!letter.isEmpty() && !letter.equals("*")) {
                statement.setString(paramIndex++, letter + "%");
            }

            int offset = (page - 1) * size;
            statement.setInt(paramIndex++, size);   // LIMIT ?
            statement.setInt(paramIndex++, offset); // OFFSET ?

            ResultSet rs = statement.executeQuery();

            JsonObject responseJson = new JsonObject();
            responseJson.addProperty("page", page);
            responseJson.addProperty("size", size);
            responseJson.addProperty("sort", sortOption);

            JsonArray moviesArray = new JsonArray();

            while (rs.next()) {
                JsonObject movieJson = new JsonObject();
                movieJson.addProperty("movie_id", rs.getString("movie_id"));
                movieJson.addProperty("title", rs.getString("title"));
                movieJson.addProperty("year", rs.getInt("year"));
                movieJson.addProperty("director", rs.getString("director"));
                movieJson.addProperty("three_genres", rs.getString("genres"));
                movieJson.addProperty("three_stars", rs.getString("stars"));
                movieJson.addProperty("three_star_ids",
                        rs.getString("star_ids") == null ? "" : rs.getString("star_ids"));
                movieJson.addProperty("rating", rs.getFloat("rating"));
                moviesArray.add(movieJson);
            }

            responseJson.add("movies", moviesArray);

            out.write(responseJson.toString());
            response.setStatus(200);

        } catch (Exception e) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("errorMessage", e.getMessage());
            out.write(jsonObject.toString());
            request.getServletContext().log("Error:", e);
            response.setStatus(500);
        } finally {
            out.close();
        }
    }
}
