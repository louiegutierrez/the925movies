package movies;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.*;

@WebServlet(name = "movies.SearchingServlet", urlPatterns = "/api/search")
public class SearchingServlet extends HttpServlet {
    private DataSource dataSource;

    @Override
    public void init(ServletConfig config) {
        try {
            // Use the slave DB for read queries
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb_slave");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        HttpSession session = request.getSession();
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        boolean anyParamInRequest = false;
        for (String p : Arrays.asList("title","year","director","star","genre","letter","page","size","sort")) {
            if (request.getParameter(p) != null) {
                anyParamInRequest = true;
                break;
            }
        }

        String title, year, director, actor, genre, letter;
        List<String> titleWords = new ArrayList<>();

        if (anyParamInRequest) {
            for (int i = 1; i <= 10; i++) {
                String word = request.getParameter("title_word" + i);
                if (word != null && !word.trim().isEmpty()) {
                    titleWords.add("+" + word.trim() + "*");
                }
            }
            title = getOrEmpty(request, "title");
            year = getOrEmpty(request, "year");
            director = getOrEmpty(request, "director");
            actor = getOrEmpty(request, "star");
            genre = getOrEmpty(request, "genre");
            letter = getOrEmpty(request, "letter");
        } else {
            // Fallback to session values
            title = getSessionOrEmpty(session, "title");
            year = getSessionOrEmpty(session, "year");
            director = getSessionOrEmpty(session, "director");
            actor = getSessionOrEmpty(session, "star");
            genre = getSessionOrEmpty(session, "genre");
            letter = getSessionOrEmpty(session, "letter");
        }

        String prevTitle = getSessionOrEmpty(session, "title");
        String prevYear = getSessionOrEmpty(session, "year");
        String prevDirector = getSessionOrEmpty(session, "director");
        String prevActor = getSessionOrEmpty(session, "star");
        String prevGenre = getSessionOrEmpty(session, "genre");
        String prevLetter = getSessionOrEmpty(session, "letter");

        boolean searchChanged = anyParamInRequest && (
                !title.equals(prevTitle)
                        || !year.equals(prevYear)
                        || !director.equals(prevDirector)
                        || !actor.equals(prevActor)
                        || !genre.equals(prevGenre)
                        || !letter.equals(prevLetter)
        );

        int page, size, sortOption;
        if (searchChanged) {
            page       = 1;
            size       = 25;
            sortOption = 7;
        } else {
            page       = parseOrDefault(request.getParameter("page"),  (Integer)session.getAttribute("page"),  1);
            size       = parseOrDefault(request.getParameter("size"),  (Integer)session.getAttribute("size"),  25);
            sortOption = parseOrDefault(request.getParameter("sort"),  (Integer)session.getAttribute("sort"),  7);
        }

        // 4) Now store everything back into session so we “remember” it.
        session.setAttribute("title",    title);
        session.setAttribute("year",     year);
        session.setAttribute("director", director);
        session.setAttribute("star",     actor);
        session.setAttribute("genre",    genre);
        session.setAttribute("letter",   letter);
        session.setAttribute("page",     page);
        session.setAttribute("size",     size);
        session.setAttribute("sort",     sortOption);

        if (!genre.isEmpty() || !letter.isEmpty()) {
            titleWords.clear();
            year = "";
            director = "";
            actor = "";
        }

        try (Connection conn = dataSource.getConnection()) {
            String mainQuery = buildMainQuery(sortOption,
                    titleWords,
                    !titleWords.isEmpty(),
                    !year.isEmpty(),
                    !director.isEmpty(),
                    !actor.isEmpty(),
                    !genre.isEmpty(),
                    !letter.isEmpty());
            List<Object> mainParams = new ArrayList<>();
            if (!titleWords.isEmpty()) {
                mainParams.add(String.join(" ", titleWords));
            }
            if (!year.isEmpty()) {
                mainParams.add(Integer.parseInt(year));
            }
            if (!director.isEmpty()) {
                mainParams.add("%" + director + "%");
            }
            if (!actor.isEmpty()) {
                mainParams.add("%" + actor + "%");
            }
            if (!genre.isEmpty()) {
                mainParams.add("%" + genre + "%");
            }
            if (!letter.isEmpty() && !letter.equals("*")) {
                mainParams.add(letter + "%");
            }
            mainParams.add(size);
            mainParams.add((page - 1) * size);

            List<MovieData> movies = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(mainQuery)) {
                bindParams(ps, mainParams);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        MovieData md = new MovieData();
                        md.movieId  = rs.getString("movie_id");
                        md.title    = rs.getString("title");
                        md.year     = rs.getInt("year");
                        md.director = rs.getString("director");
                        md.rating   = rs.getFloat("rating");
                        movies.add(md);
                    }
                }
            }

            if (movies.isEmpty()) {
                // No results => return empty JSON
                JsonObject responseJson = new JsonObject();
                responseJson.addProperty("page", page);
                responseJson.addProperty("size", size);
                responseJson.addProperty("sort", sortOption);
                responseJson.add("movies", new JsonArray());
                out.write(responseJson.toString());
                response.setStatus(200);
                return;
            }

            // Next queries: gather all IDs
            Set<String> movieIds = new HashSet<>();
            for (MovieData m : movies) {
                movieIds.add(m.movieId);
            }

            // Query B: get up to 3 genres per movie
            Map<String, String> movieIdToGenres = fetchGenres(conn, movieIds);

            // Query C: top 3 stars
            Map<String, StarsData> movieIdToStars = fetchStars(conn, movieIds);

            // Combine results
            JsonObject responseJson = new JsonObject();
            responseJson.addProperty("page", page);
            responseJson.addProperty("size", size);
            responseJson.addProperty("sort", sortOption);

            JsonArray moviesArray = new JsonArray();
            for (MovieData md : movies) {
                JsonObject movieJson = new JsonObject();
                movieJson.addProperty("movie_id",  md.movieId);
                movieJson.addProperty("title",     md.title);
                movieJson.addProperty("year",      md.year);
                movieJson.addProperty("director",  md.director);
                movieJson.addProperty("rating",    md.rating);

                // three_genres
                String gVal = movieIdToGenres.get(md.movieId);
                movieJson.addProperty("three_genres", (gVal == null) ? "" : gVal);

                // three_stars
                StarsData sData = movieIdToStars.get(md.movieId);
                if (sData != null) {
                    movieJson.addProperty("three_star_ids",  (sData.starIds   == null) ? "" : sData.starIds);
                    movieJson.addProperty("three_stars",     (sData.starNames == null) ? "" : sData.starNames);
                } else {
                    movieJson.addProperty("three_star_ids", "");
                    movieJson.addProperty("three_stars", "");
                }
                moviesArray.add(movieJson);
            }
            responseJson.add("movies", moviesArray);

            out.write(responseJson.toString());
            response.setStatus(200);

        } catch (Exception e) {
            JsonObject errorJson = new JsonObject();
            errorJson.addProperty("errorMessage", e.getMessage());
            out.write(errorJson.toString());
            request.getServletContext().log("Error:", e);
            response.setStatus(500);
        } finally {
            out.close();
        }
    }

    // Build the main query with dynamic WHERE clauses
    private String buildMainQuery(
            int sortOption,
            List<String> titleWords,
            boolean hasTitleWords,
            boolean hasYear,
            boolean hasDirector,
            boolean hasActor,
            boolean hasGenre,
            boolean hasLetter
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT m.id AS movie_id, m.title, m.year, m.director, ")
                .append("COALESCE(r.rating, 0.0) AS rating ")
                .append("FROM movies m ")
                .append("LEFT JOIN ratings r ON r.movieId = m.id ")
                .append("WHERE 1=1 ");

        // Full-text
        if (hasTitleWords) {
            sb.append("AND MATCH(m.title) AGAINST(? IN BOOLEAN MODE) ");
        }
        if (hasYear) {
            sb.append("AND m.year = ? ");
        }
        if (hasDirector) {
            sb.append("AND m.director LIKE ? ");
        }
        if (hasActor) {
            sb.append("AND EXISTS ( ")
                    .append("  SELECT 1 FROM stars_in_movies sm2 ")
                    .append("  JOIN stars s2 ON sm2.starId = s2.id ")
                    .append("  WHERE sm2.movieId = m.id AND s2.name LIKE ? ")
                    .append(") ");
        }
        if (hasGenre) {
            sb.append("AND EXISTS ( ")
                    .append("  SELECT 1 FROM genres_in_movies gim2 ")
                    .append("  JOIN genres g2 ON gim2.genreId = g2.id ")
                    .append("  WHERE gim2.movieId = m.id AND g2.name LIKE ? ")
                    .append(") ");
        }
        if (hasLetter) {
            sb.append("AND m.title LIKE ? ");
        }

        // Sorting
        switch (sortOption) {
            case 2:
                sb.append("ORDER BY m.title ASC, r.rating DESC ");
                break;
            case 3:
                sb.append("ORDER BY m.title DESC, r.rating ASC ");
                break;
            case 4:
                sb.append("ORDER BY m.title DESC, r.rating DESC ");
                break;
            case 5:
                sb.append("ORDER BY r.rating ASC, m.title ASC ");
                break;
            case 6:
                sb.append("ORDER BY r.rating ASC, m.title DESC ");
                break;
            case 7:
                sb.append("ORDER BY r.rating DESC, m.title ASC ");
                break;
            case 8:
                sb.append("ORDERBY r.rating DESC, m.title DESC ");
                break;
            default:
                sb.append("ORDER BY m.title ASC, r.rating ASC ");
                break;
        }
        sb.append("LIMIT ? OFFSET ? ");
        return sb.toString();
    }

    private Map<String, String> fetchGenres(Connection conn, Set<String> movieIds) throws SQLException {
        String sql =
                "SELECT gim.movieId, " +
                        "       SUBSTRING_INDEX(GROUP_CONCAT(g.name ORDER BY g.name ASC SEPARATOR ', '), ', ', 3) AS genres " +
                        "FROM genres_in_movies gim " +
                        "JOIN genres g ON g.id = gim.genreId " +
                        "WHERE gim.movieId IN (" + makeInClause(movieIds.size()) + ") " +
                        "GROUP BY gim.movieId";

        Map<String,String> result = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            for (String mid : movieIds) {
                ps.setString(idx++, mid);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getString("movieId"), rs.getString("genres"));
                }
            }
        }
        return result;
    }

    private Map<String, StarsData> fetchStars(Connection conn, Set<String> movieIds) throws SQLException {
        String sql =
                "SELECT sm.movieId, " +
                        "  SUBSTRING_INDEX(" +
                        "    GROUP_CONCAT(s.id ORDER BY sc.star_count DESC, s.name ASC SEPARATOR ', ')," +
                        "    ', ', 3" +
                        "  ) AS star_ids, " +
                        "  SUBSTRING_INDEX(" +
                        "    GROUP_CONCAT(s.name ORDER BY sc.star_count DESC, s.name ASC SEPARATOR ', ')," +
                        "    ', ', 3" +
                        "  ) AS stars " +
                        "FROM stars_in_movies sm " +
                        "JOIN stars s ON s.id = sm.starId " +
                        "LEFT JOIN ( " +
                        "  SELECT starId, COUNT(*) AS star_count FROM stars_in_movies GROUP BY starId " +
                        ") sc ON sc.starId = s.id " +
                        "WHERE sm.movieId IN (" + makeInClause(movieIds.size()) + ") " +
                        "GROUP BY sm.movieId";

        Map<String, StarsData> result = new HashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            for (String mid : movieIds) {
                ps.setString(idx++, mid);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    StarsData sd = new StarsData();
                    sd.starIds   = rs.getString("star_ids");
                    sd.starNames = rs.getString("stars");
                    result.put(rs.getString("movieId"), sd);
                }
            }
        }
        return result;
    }

    private String makeInClause(int count) {
        // e.g. if count=3 => "?, ?, ?"
        String[] marks = new String[count];
        Arrays.fill(marks, "?");
        return String.join(", ", marks);
    }

    private int parseOrDefault(String strVal, Integer sessVal, int def) {
        if (strVal != null) {
            try {
                return Integer.parseInt(strVal);
            } catch (NumberFormatException ignore) {
                return def;
            }
        }
        if (sessVal != null) {
            return sessVal;
        }
        return def;
    }

    private String getOrEmpty(HttpServletRequest req, String param) {
        String val = req.getParameter(param);
        return (val == null) ? "" : val.trim();
    }

    private String getSessionOrEmpty(HttpSession sess, String key) {
        Object val = sess.getAttribute(key);
        return (val == null) ? "" : val.toString().trim();
    }

    private void bindParams(PreparedStatement ps, List<Object> params) throws SQLException {
        int idx = 1;
        for (Object obj : params) {
            if (obj instanceof Integer) {
                ps.setInt(idx, (Integer) obj);
            } else {
                ps.setString(idx, obj.toString());
            }
            idx++;
        }
    }

    private static class MovieData {
        String movieId;
        String title;
        int year;
        String director;
        float rating;
    }

    private static class StarsData {
        String starIds;
        String starNames;
    }
}
