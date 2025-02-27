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

@WebServlet(name = "SearchingServlet", urlPatterns = "/api/search")
public class SearchingServlet extends HttpServlet {
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext()
                    .lookup("java:comp/env/jdbc/moviedb");
            System.out.println("DataSource initialized successfully.");

            // Add full-text index to the `title` column if it doesn't exist
            try (Connection conn = dataSource.getConnection()) {
                String addFullTextIndexQuery = "ALTER TABLE movies ADD FULLTEXT(title);";
                try (PreparedStatement statement = conn.prepareStatement(addFullTextIndexQuery)) {
                    statement.executeUpdate();
                    System.out.println("Full-text index added to the `title` column.");
                } catch (Exception e) {
                    System.out.println("Full-text index already exists or could not be added: " + e.getMessage());
                }
            }
        } catch (NamingException e) {
            e.printStackTrace();
            System.err.println("Error initializing DataSource: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error adding full-text index: " + e.getMessage());
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        System.out.println("Received request with parameters: " + request.getQueryString());

        boolean hasParam = (request.getParameter("title") != null) ||
                (request.getParameter("year") != null) ||
                (request.getParameter("director") != null) ||
                (request.getParameter("star") != null);

        String title, year, director, star;
        if (hasParam) {
            title = request.getParameter("title") == null ? "" : request.getParameter("title").trim();
            year = request.getParameter("year") == null ? "" : request.getParameter("year").trim();
            director = request.getParameter("director") == null ? "" : request.getParameter("director").trim();
            star = request.getParameter("star") == null ? "" : request.getParameter("star").trim();
            System.out.println("Parameters from request: title=" + title + ", year=" + year +
                    ", director=" + director + ", star=" + star);
        } else {
            title = session.getAttribute("title") == null ? "" : session.getAttribute("title").toString();
            year = session.getAttribute("year") == null ? "" : session.getAttribute("year").toString();
            director = session.getAttribute("director") == null ? "" : session.getAttribute("director").toString();
            star = session.getAttribute("star") == null ? "" : session.getAttribute("star").toString();
            System.out.println("Parameters from session: title=" + title + ", year=" + year +
                    ", director=" + director + ", star=" + star);
        }

        // Check if search parameters have changed
        boolean searchChanged = hasParam && (
                !title.equals(session.getAttribute("title")) ||
                        !year.equals(session.getAttribute("year")) ||
                        !director.equals(session.getAttribute("director")) ||
                        !star.equals(session.getAttribute("star"))
        );

        System.out.println("Search parameters changed: " + searchChanged);

        // Set default values for pagination and sorting
        int defaultPage = 1, defaultSize = 25, defaultSort = 7;
        int page, size, sortOption;
        if (searchChanged) {
            page = defaultPage;
            size = defaultSize;
            sortOption = defaultSort;
            System.out.println("Search parameters changed. Resetting to default: page=" + page +
                    ", size=" + size + ", sort=" + sortOption);
        } else {
            page = (request.getParameter("page") != null)
                    ? Integer.parseInt(request.getParameter("page"))
                    : (session.getAttribute("page") == null ? defaultPage : (int) session.getAttribute("page"));
            size = (request.getParameter("size") != null)
                    ? Integer.parseInt(request.getParameter("size"))
                    : (session.getAttribute("size") == null ? defaultSize : (int) session.getAttribute("size"));
            sortOption = (request.getParameter("sort") != null)
                    ? Integer.parseInt(request.getParameter("sort"))
                    : (session.getAttribute("sort") == null ? defaultSort : (int) session.getAttribute("sort"));
            System.out.println("Using existing parameters: page=" + page +
                    ", size=" + size + ", sort=" + sortOption);
        }

        // Update session attributes
        session.setAttribute("title", title);
        session.setAttribute("year", year);
        session.setAttribute("director", director);
        session.setAttribute("star", star);
        session.setAttribute("page", page);
        session.setAttribute("size", size);
        session.setAttribute("sort", sortOption);

        System.out.println("Session attributes updated.");

        // Debug print
        System.out.println("Executing query with parameters: title=" + title + ", year=" + year
                + ", director=" + director + ", star=" + star
                + ", page=" + page + ", size=" + size + ", sort=" + sortOption);

        // Build base query
        try (Connection conn = dataSource.getConnection()) {
            System.out.println("Database connection established.");

            StringBuilder queryBuilder = new StringBuilder();
            queryBuilder.append("SELECT m.id AS movie_id, m.title, m.year, m.director, ");
            queryBuilder.append("SUBSTRING_INDEX(star_info.stars, ', ', 3) AS stars, ");
            queryBuilder.append("COALESCE(r.rating, 0.0) AS rating ");
            queryBuilder.append("FROM movies m ");
            queryBuilder.append("LEFT JOIN ratings r ON r.movieId = m.id ");
            queryBuilder.append("LEFT JOIN ( ");
            queryBuilder.append("    SELECT sm.movieId, ");
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
            queryBuilder.append("WHERE 1=1 ");

            // Handle title prefix search using full-text search
            if (!title.isEmpty()) {
                // Split the title into individual keywords
                String[] keywords = title.split("\\s+");
                if (keywords.length > 0) {
                    // Construct the full-text search query
                    StringBuilder fullTextQuery = new StringBuilder();
                    for (String keyword : keywords) {
                        if (fullTextQuery.length() > 0) {
                            fullTextQuery.append(" ");
                        }
                        fullTextQuery.append("+").append(keyword).append("*");
                    }
                    queryBuilder.append("AND MATCH(m.title) AGAINST(? IN BOOLEAN MODE) ");
                    System.out.println("Full-text search query: " + fullTextQuery);
                }
            }

            // Handle other search parameters
            if (!year.isEmpty()) {
                queryBuilder.append("AND m.year = ? ");
            }
            if (!director.isEmpty()) {
                queryBuilder.append("AND m.director LIKE ? ");
            }
            if (!star.isEmpty()) {
                queryBuilder.append("AND EXISTS (SELECT 1 FROM stars_in_movies sm ");
                queryBuilder.append("JOIN stars s ON sm.starId = s.id ");
                queryBuilder.append("WHERE sm.movieId = m.id AND s.name LIKE ?) ");
            }

            // Sorting
            switch (sortOption) {
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

            // Pagination
            queryBuilder.append("LIMIT ? OFFSET ? ");

            String query = queryBuilder.toString();
            System.out.println("Final query: " + query);

            PreparedStatement statement = conn.prepareStatement(query);

            int paramIndex = 1;

            // Bind title keywords for full-text search
            if (!title.isEmpty()) {
                String[] keywords = title.split("\\s+");
                StringBuilder fullTextQuery = new StringBuilder();
                for (String keyword : keywords) {
                    if (fullTextQuery.length() > 0) {
                        fullTextQuery.append(" ");
                    }
                    fullTextQuery.append("+").append(keyword).append("*");
                }
                System.out.println("Binding full-text search query: " + fullTextQuery);
                statement.setString(paramIndex++, fullTextQuery.toString());
            }

            // Bind other search parameters
            if (!year.isEmpty()) {
                System.out.println("Binding year: " + year);
                statement.setInt(paramIndex++, Integer.parseInt(year));
            }
            if (!director.isEmpty()) {
                System.out.println("Binding director: " + director + "%");
                statement.setString(paramIndex++, "%" + director + "%");
            }
            if (!star.isEmpty()) {
                System.out.println("Binding star: " + star + "%");
                statement.setString(paramIndex++, "%" + star + "%");
            }

            // Bind pagination parameters
            int offset = (page - 1) * size;
            System.out.println("Binding limit: " + size + ", offset: " + offset);
            statement.setInt(paramIndex++, size);   // LIMIT ?
            statement.setInt(paramIndex++, offset); // OFFSET ?

            ResultSet rs = statement.executeQuery();
            System.out.println("Query executed successfully.");

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
                String starsValue = rs.getString("stars");
                if (starsValue == null) {
                    starsValue = "";
                }
                movieJson.addProperty("three_stars", starsValue);
                movieJson.addProperty("rating", rs.getFloat("rating"));
                moviesArray.add(movieJson);
            }

            responseJson.add("movies", moviesArray);

            System.out.println("Query results: " + responseJson.toString());

            out.write(responseJson.toString());
            response.setStatus(200);

        } catch (Exception e) {
            System.err.println("Error executing query: " + e.getMessage());
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("errorMessage", e.getMessage());
            out.write(jsonObject.toString());
            request.getServletContext().log("Error:", e);
            response.setStatus(500);
        } finally {
            out.close();
            System.out.println("Response sent.");
        }
    }
}