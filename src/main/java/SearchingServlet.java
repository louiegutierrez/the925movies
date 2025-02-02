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

    private String handleSessionParam(HttpServletRequest request, HttpSession session, String paramName) {
        String value = request.getParameter(paramName);
        if (value != null && !value.trim().isEmpty()) {
            session.setAttribute(paramName, value);
        } else {
            Object sessionVal = session.getAttribute(paramName);
            value = (sessionVal == null) ? null : sessionVal.toString();
        }
        return value;
    }

    private int handleSessionIntParam(HttpServletRequest request, HttpSession session, String paramName, int defaultValue) {
        String paramStr = request.getParameter(paramName);
        if (paramStr != null && !paramStr.trim().isEmpty()) {
            try {
                int val = Integer.parseInt(paramStr);
                System.out.println("setting attribute " + paramName + " in functioin as " + val);
                session.setAttribute(paramName, val);
                return val;
            } catch (NumberFormatException e) {
                Object sessVal = session.getAttribute(paramName);
                if (sessVal == null) {
                    return defaultValue;
                }
                return (int) sessVal;
            }
        } else {
            Object sessVal = session.getAttribute(paramName);
            if (sessVal == null) {
                return defaultValue;
            }
            return (int) sessVal;
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        String title = handleSessionParam(request, session, "title");
        String year = handleSessionParam(request, session, "year");
        String director = handleSessionParam(request, session, "director");
        String actor = handleSessionParam(request, session, "star");
        String genre = handleSessionParam(request, session, "genre");
        String letter = handleSessionParam(request, session, "letter");

        int page = handleSessionIntParam(request, session, "page", 1);
        int size = handleSessionIntParam(request, session, "size", 25);
        int sortOption = handleSessionIntParam(request, session, "sort", 7);
        if (page < 1) {
            page = 1;
            session.setAttribute("page", page);
        }
        if (!Arrays.asList(10, 25, 50, 100).contains(size)) {
            size = 25;
            session.setAttribute("size", size);
        }
        if (sortOption < 1 || sortOption > 8) {
            sortOption = 7;
            session.setAttribute("sort", sortOption);
        }


        // Print debug
        System.out.println("title=" + title + ", year=" + year
                + ", director=" + director + ", actor=" + actor
                + ", genre=" + genre + ", letter=" + letter
                + ", page=" + page + ", size=" + size + ", sort=" + sortOption);

        // Build base query
        try (Connection conn = dataSource.getConnection()) {
            StringBuilder queryBuilder = new StringBuilder();
            queryBuilder.append("SELECT m.id AS movie_id, m.title, m.year, m.director, ");
            queryBuilder.append("(")
                    .append("  SELECT GROUP_CONCAT(gsub.genre_name SEPARATOR ', ') ")
                    .append("  FROM (")
                    .append("    SELECT g.name AS genre_name ")
                    .append("    FROM genres_in_movies gm ")
                    .append("    JOIN genres g ON gm.genreId = g.id ")
                    .append("    WHERE gm.movieId = m.id ")
                    .append("    GROUP BY g.id, g.name ")
                    .append("    ORDER BY g.name ASC ")
                    .append("    LIMIT 3 ")
                    .append("  ) AS gsub ")
                    .append(") AS genres, ");
            queryBuilder.append("(")
                    .append("  SELECT GROUP_CONCAT(sidsub.star_id SEPARATOR ', ') ")
                    .append("  FROM (")
                    .append("    SELECT ")
                    .append("      s.id AS star_id, ")
                    .append("      (SELECT COUNT(*) FROM stars_in_movies sim2 ")
                    .append("       WHERE sim2.starId = s.id) AS total_appearances ")
                    .append("    FROM stars_in_movies sm ")
                    .append("    JOIN stars s ON sm.starId = s.id ")
                    .append("    WHERE sm.movieId = m.id ")
                    .append("    GROUP BY s.id, s.name ")
                    .append("    ORDER BY total_appearances DESC, s.name ASC ")
                    .append("    LIMIT 3 ")
                    .append("  ) AS sidsub ")
                    .append(") AS star_ids, ");
            queryBuilder.append("(")
                    .append("  SELECT GROUP_CONCAT(snamesub.star_name SEPARATOR ', ') ")
                    .append("  FROM (")
                    .append("    SELECT ")
                    .append("      s.name AS star_name, ")
                    .append("      (SELECT COUNT(*) FROM stars_in_movies sim2 ")
                    .append("       WHERE sim2.starId = s.id) AS total_appearances ")
                    .append("    FROM stars_in_movies sm ")
                    .append("    JOIN stars s ON sm.starId = s.id ")
                    .append("    WHERE sm.movieId = m.id ")
                    .append("    GROUP BY s.id, s.name ")
                    .append("    ORDER BY total_appearances DESC, s.name ASC ")
                    .append("    LIMIT 3 ")
                    .append("  ) AS snamesub ")
                    .append(") AS stars, ");
            queryBuilder.append("COALESCE((")
                    .append(" SELECT r.rating FROM ratings r WHERE r.movieId = m.id")
                    .append("), 0.0) AS rating ");
            queryBuilder.append("FROM movies m ");
            queryBuilder.append("WHERE 1=1 ");

            if (title != null && !title.isEmpty()) {
                queryBuilder.append("AND m.title LIKE ? ");
            }
            if (year != null && !year.isEmpty()) {
                queryBuilder.append("AND m.year = ? ");
            }
            if (director != null && !director.isEmpty()) {
                queryBuilder.append("AND m.director LIKE ? ");
            }
            if (actor != null && !actor.isEmpty()) {
                queryBuilder.append(
                        "AND EXISTS (SELECT 1 FROM stars_in_movies sm "
                                + "JOIN stars s ON sm.starId = s.id "
                                + "WHERE sm.movieId = m.id AND s.name LIKE ?) ");
            }
            if (genre != null && !genre.isEmpty()) {
                queryBuilder.append(
                        "AND EXISTS ("
                                + "  SELECT 1 "
                                + "  FROM genres_in_movies gim "
                                + "  JOIN genres g ON gim.genreId = g.id "
                                + "  WHERE gim.movieId = m.id "
                                + "    AND g.name LIKE ?"
                                + ") "
                );
            }
            if (letter != null && !letter.isEmpty()) {
                if (letter.equals("*")) {
                    queryBuilder.append("AND m.title REGEXP '^[^a-zA-Z0-9]' ");
                } else {
                    queryBuilder.append("AND m.title LIKE ? ");
                }
            }

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
            PreparedStatement statement = conn.prepareStatement(query);

            int paramIndex = 1;

            // Bind all the search params in order
            if (title != null && !title.isEmpty()) {
                statement.setString(paramIndex++, "%" + title + "%");
            }
            if (year != null && !year.isEmpty()) {
                statement.setInt(paramIndex++, Integer.parseInt(year));
            }
            if (director != null && !director.isEmpty()) {
                statement.setString(paramIndex++, "%" + director + "%");
            }
            if (actor != null && !actor.isEmpty()) {
                statement.setString(paramIndex++, "%" + actor + "%");
            }
            if (genre != null && !genre.isEmpty()) {
                statement.setString(paramIndex++, "%" + genre + "%");
            }
            if (letter != null && !letter.isEmpty() && !letter.equals("*")) {
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
