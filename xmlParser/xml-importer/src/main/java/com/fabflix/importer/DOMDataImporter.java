import org.w3c.dom.*;
import javax.xml.parsers.*;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class DOMDataImporter {

    // --------------------------
    // Caches and Counters
    // --------------------------
    // For movies: composite key "title-year-director" → movie id
    private static final Map<String, String> movieCache = new HashMap<>();
    // For genres: genre name → genre id
    private static final Map<String, Integer> genreCache = new HashMap<>();
    // For stars: star name → star id
    private static final Map<String, String> starCache = new HashMap<>();
    // For stars_in_movies: "starId_movieId" to avoid duplicate links
    private static final Map<String, Boolean> starsInMoviesCache = new HashMap<>();

    // Summary counters
    private static int moviesAdded = 0;
    private static int genresAdded = 0;
    private static int starsAdded = 0;
    private static int starsInMoviesAdded = 0;
    private static int genresInMoviesAdded = 0;
    private static int discrepanciesCount = 0;

    // Detailed discrepancy counters (only three categories)
    private static int inconsistentMoviesCount = 0; // movies missing required fields
    private static int duplicateMoviesCount = 0;    // duplicate movies
    private static int duplicateStarsCount = 0;     // duplicate stars

    // Log files for discrepancies
    private static PrintWriter logInconsistentMovies;
    private static PrintWriter logDuplicateMovies;
    private static PrintWriter logDuplicateStars;
    private static PrintWriter logSummary;

    // Database connection
    private static Connection connection;

    // --------------------------
    // Custom ErrorHandler to log errors and continue
    // --------------------------
    private static class MyErrorHandler implements ErrorHandler {
        public void warning(SAXParseException exception) {
            logInconsistentMovies.println("WARNING: " + exception.getMessage());
        }
        public void error(SAXParseException exception) {
            logInconsistentMovies.println("ERROR: " + exception.getMessage());
        }
        public void fatalError(SAXParseException exception) {
            logInconsistentMovies.println("FATAL: " + exception.getMessage());
            // Do not throw exception in order to continue parsing.
            // (Note: This behavior is non-standard and depends on your parser.)
        }
    }

    // --------------------------
    // Main Method
    // --------------------------
    public static void main(String[] args) {
        try {
            // Initialize logs (each created in the current working directory)
            logInconsistentMovies = new PrintWriter(new FileWriter("inconsistent_movies.txt", false));
            logDuplicateMovies = new PrintWriter(new FileWriter("duplicate_movies.txt", false));
            logDuplicateStars = new PrintWriter(new FileWriter("duplicate_stars.txt", false));
            logSummary = new PrintWriter(new FileWriter("dom_import_summary.txt", false));

            // Connect to DB (adjust credentials as needed)
            String url = "jdbc:mysql://localhost:3306/moviedb?useUnicode=true&characterEncoding=ISO-8859-1";
            String user = "mytestuser";
            String password = "My6$Password";
            connection = DriverManager.getConnection(url, user, password);

            // Pre-load caches from DB
            loadCaches();

            // Process each XML file individually so errors in one don't stop others
            try {
                parseMovies("mains243.xml");
            } catch (Exception ex) {
                discrepanciesCount++;
                logInconsistentMovies.println("Error parsing mains243.xml: " + ex.getMessage());
            }
            try {
                parseActors("actors63.xml");
            } catch (Exception ex) {
                discrepanciesCount++;
                logInconsistentMovies.println("Error parsing actors63.xml: " + ex.getMessage());
            }
            try {
                parseCasts("casts124.xml");
            } catch (Exception ex) {
                discrepanciesCount++;
                logInconsistentMovies.println("Error parsing casts124.xml: " + ex.getMessage());
            }

            // Write summary log
            logSummary.println("=== DOM Import Summary ===");
            logSummary.println("Movies Added: " + moviesAdded);
            logSummary.println("  Inconsistent Movies (missing required fields): " + inconsistentMoviesCount);
            logSummary.println("  Duplicate Movies: " + duplicateMoviesCount);
            logSummary.println("Genres Added: " + genresAdded);
            logSummary.println("Stars Added: " + starsAdded);
            logSummary.println("  Duplicate Stars: " + duplicateStarsCount);
            logSummary.println("Stars_in_Movies Added: " + starsInMoviesAdded);
            logSummary.println("Genres_in_Movies Added: " + genresInMoviesAdded);
            logSummary.println("Total Discrepancies: " + discrepanciesCount);
            logSummary.close();

            // Close logs and connection
            logInconsistentMovies.close();
            logDuplicateMovies.close();
            logDuplicateStars.close();
            connection.close();
            System.out.println("DOM import completed. See log files for details.");
        } catch (Exception e) {
            e.printStackTrace();
            if (logSummary != null) {
                logSummary.println("Fatal error: " + e.getMessage());
                logSummary.close();
            }
        }
    }

    // --------------------------
    // Load caches from DB
    // --------------------------
    private static void loadCaches() throws SQLException {
        Statement stmt = connection.createStatement();

        // Load movies using composite key: title-year-director
        ResultSet rs = stmt.executeQuery("SELECT id, title, year, director FROM movies");
        while (rs.next()) {
            String key = rs.getString("title").trim() + "-" + rs.getInt("year") + "-" + rs.getString("director").trim();
            movieCache.put(key, rs.getString("id"));
        }
        rs.close();

        // Load genres
        rs = stmt.executeQuery("SELECT id, name FROM genres");
        while (rs.next()) {
            genreCache.put(rs.getString("name").trim(), rs.getInt("id"));
        }
        rs.close();

        // Load stars
        rs = stmt.executeQuery("SELECT id, name FROM stars");
        while (rs.next()) {
            starCache.put(rs.getString("name").trim(), rs.getString("id"));
        }
        rs.close();

        stmt.close();
    }

    // --------------------------
    // Parse mains243.xml (Movies & Genres)
    // --------------------------
    private static void parseMovies(String filename) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // Enable continuing after fatal errors (Xerces-specific)
            factory.setFeature("http://apache.org/xml/features/continue-after-fatal-error", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new MyErrorHandler());
            Document doc = builder.parse(new File(filename));
            doc.getDocumentElement().normalize();

            NodeList directorfilmsList = doc.getElementsByTagName("directorfilms");
            for (int i = 0; i < directorfilmsList.getLength(); i++) {
                Element directorfilmsElem = (Element) directorfilmsList.item(i);
                // Get director name from <dirname>
                String director = "";
                NodeList dirNameList = directorfilmsElem.getElementsByTagName("dirname");
                if (dirNameList.getLength() > 0) {
                    director = dirNameList.item(0).getTextContent().trim();
                }
                // For each film element
                NodeList filmList = directorfilmsElem.getElementsByTagName("film");
                for (int j = 0; j < filmList.getLength(); j++) {
                    Element filmElem = (Element) filmList.item(j);
                    String filmTitle = "";
                    String filmYear = "";
                    NodeList titleList = filmElem.getElementsByTagName("t");
                    if (titleList.getLength() > 0) {
                        filmTitle = titleList.item(0).getTextContent().trim();
                    }
                    NodeList yearList = filmElem.getElementsByTagName("year");
                    if (yearList.getLength() > 0) {
                        filmYear = yearList.item(0).getTextContent().trim();
                    }

                    // Validate required fields: if year is missing, do not insert the movie.
                    if (filmTitle.isEmpty() || filmYear.isEmpty() || director.isEmpty()) {
                        inconsistentMoviesCount++;
                        discrepanciesCount++;
                        logInconsistentMovies.println("Inconsistent movie info. Title='" + filmTitle +
                                "', Year='" + filmYear + "', Director='" + director + "'");
                        continue;
                    }

                    String compositeKey = filmTitle + "-" + filmYear + "-" + director;
                    if (movieCache.containsKey(compositeKey)) {
                        duplicateMoviesCount++;
                        logDuplicateMovies.println("Duplicate movie: " + filmTitle + " (" + filmYear + ", " + director + ")");
                        continue;
                    }

                    // Insert new movie with a random price
                    String newMovieId = "tt" + String.format("%07d", movieCache.size() + 1);
                    float randomPrice = generateRandomPrice(5f, 200f);

                    PreparedStatement ps = connection.prepareStatement(
                            "INSERT INTO movies(id, title, year, director, price) VALUES(?, ?, ?, ?, ?)"
                    );
                    ps.setString(1, newMovieId);
                    ps.setString(2, filmTitle);
                    try {
                        ps.setInt(3, Integer.parseInt(filmYear));
                    } catch (NumberFormatException nfe) {
                        discrepanciesCount++;
                        logInconsistentMovies.println("Invalid year for film: " + filmTitle + " => " + filmYear);
                        ps.setNull(3, Types.INTEGER);
                    }
                    ps.setString(4, director);
                    ps.setFloat(5, randomPrice);
                    ps.executeUpdate();
                    ps.close();

                    movieCache.put(compositeKey, newMovieId);
                    moviesAdded++;

                    // Process genres from <cats>
                    NodeList catsList = filmElem.getElementsByTagName("cats");
                    if (catsList.getLength() > 0) {
                        Element catsElem = (Element) catsList.item(0);
                        NodeList catList = catsElem.getElementsByTagName("cat");
                        for (int k = 0; k < catList.getLength(); k++) {
                            String genreName = catList.item(k).getTextContent().trim();
                            if (genreName.isEmpty()) continue;
                            int genreId;
                            if (!genreCache.containsKey(genreName)) {
                                PreparedStatement psGenre = connection.prepareStatement(
                                        "INSERT INTO genres(name) VALUES(?)", Statement.RETURN_GENERATED_KEYS
                                );
                                psGenre.setString(1, genreName);
                                psGenre.executeUpdate();
                                ResultSet rs = psGenre.getGeneratedKeys();
                                if (rs.next()) {
                                    genreId = rs.getInt(1);
                                } else {
                                    genreId = -1;
                                }
                                rs.close();
                                psGenre.close();
                                if (genreId != -1) {
                                    genreCache.put(genreName, genreId);
                                    genresAdded++;
                                } else {
                                    discrepanciesCount++;
                                    continue;
                                }
                            } else {
                                genreId = genreCache.get(genreName);
                            }
                            PreparedStatement psGIM = connection.prepareStatement(
                                    "INSERT INTO genres_in_movies(genreId, movieId) VALUES(?, ?)"
                            );
                            psGIM.setInt(1, genreId);
                            psGIM.setString(2, newMovieId);
                            psGIM.executeUpdate();
                            psGIM.close();
                            genresInMoviesAdded++;
                        }
                    }
                }
            }
        } catch (Exception e) {
            discrepanciesCount++;
            logInconsistentMovies.println("Error parsing " + filename + ": " + e.getMessage());
        }
    }

    // --------------------------
    // Parse actors63.xml (Stars)
    // --------------------------
    private static void parseActors(String filename) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // Set lenient mode to continue after fatal errors
            factory.setFeature("http://apache.org/xml/features/continue-after-fatal-error", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new MyErrorHandler());
            Document doc = builder.parse(new File(filename));
            doc.getDocumentElement().normalize();

            NodeList actorList = doc.getElementsByTagName("actor");
            for (int i = 0; i < actorList.getLength(); i++) {
                Element actorElem = (Element) actorList.item(i);
                String starName = "";
                String birthYear = "";

                NodeList stageNameList = actorElem.getElementsByTagName("stagename");
                if (stageNameList.getLength() > 0) {
                    starName = stageNameList.item(0).getTextContent().trim();
                }
                NodeList dobList = actorElem.getElementsByTagName("dob");
                if (dobList.getLength() > 0) {
                    birthYear = dobList.item(0).getTextContent().trim();
                }

                if (starName.isEmpty()) {
                    discrepanciesCount++;
                    logInconsistentMovies.println("Actor missing stagename.");
                    continue;
                }
                if (starCache.containsKey(starName)) {
                    duplicateStarsCount++;
                    logDuplicateStars.println("Duplicate star: " + starName);
                    continue;
                }

                String newStarId = "nm" + String.format("%07d", starCache.size() + 1);
                try {
                    PreparedStatement ps = connection.prepareStatement(
                            "INSERT INTO stars(id, name, birthYear) VALUES(?, ?, ?)"
                    );
                    ps.setString(1, newStarId);
                    ps.setString(2, starName);
                    if (birthYear.isEmpty()) {
                        ps.setNull(3, Types.INTEGER);
                    } else {
                        try {
                            ps.setInt(3, Integer.parseInt(birthYear));
                        } catch (NumberFormatException nfe) {
                            discrepanciesCount++;
                            logInconsistentMovies.println("Invalid birthYear for star '" + starName + "': " + birthYear);
                            ps.setNull(3, Types.INTEGER);
                        }
                    }
                    ps.executeUpdate();
                    ps.close();
                } catch (SQLException ex) {
                    duplicateStarsCount++;
                    logDuplicateStars.println("Duplicate star on insert (actors file): " + starName);
                }
                starCache.put(starName, newStarId);
                starsAdded++;
            }
        } catch (Exception e) {
            discrepanciesCount++;
            logInconsistentMovies.println("Error parsing " + filename + ": " + e.getMessage());
        }
    }

    // --------------------------
    // Parse casts124.xml (Stars_in_Movies)
    // For each <dirfilms> group, the director is given by the <is> element.
    // For each <m> element, we try to find a matching movie by title and director.
    // If no matching movie exists, we log a discrepancy and do not insert a new movie.
    // For each <m> element, if the star (by stagename) isn't in the stars table,
    // insert the star (with a NULL birthYear) and update the starsAdded counter.
    // Then link the star to the movie in stars_in_movies.
    // --------------------------
    private static void parseCasts(String filename) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/continue-after-fatal-error", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new MyErrorHandler());
            Document doc = builder.parse(new File(filename));
            doc.getDocumentElement().normalize();

            NodeList dirfilmsList = doc.getElementsByTagName("dirfilms");
            for (int i = 0; i < dirfilmsList.getLength(); i++) {
                Element dirfilmsElem = (Element) dirfilmsList.item(i);
                // Get director from <is>
                String director = "";
                NodeList isList = dirfilmsElem.getElementsByTagName("is");
                if (isList.getLength() > 0) {
                    director = isList.item(0).getTextContent().trim();
                }
                // Process each <filmc> group within this dirfilms element
                NodeList filmcList = dirfilmsElem.getElementsByTagName("filmc");
                for (int j = 0; j < filmcList.getLength(); j++) {
                    Element filmcElem = (Element) filmcList.item(j);
                    NodeList mList = filmcElem.getElementsByTagName("m");
                    for (int k = 0; k < mList.getLength(); k++) {
                        Element mElem = (Element) mList.item(k);
                        // Extract movie title and star name from the cast record
                        String movieTitle = "";
                        NodeList tList = mElem.getElementsByTagName("t");
                        if (tList.getLength() > 0) {
                            movieTitle = tList.item(0).getTextContent().trim();
                        }
                        String starName = "";
                        NodeList aList = mElem.getElementsByTagName("a");
                        if (aList.getLength() > 0) {
                            starName = aList.item(0).getTextContent().trim();
                        }

                        // Look for a movie in our cache that matches movieTitle and director.
                        // Since the casts file does not provide a year, we search keys ending with "-" + director.
                        String movieId = null;
                        for (Map.Entry<String, String> entry : movieCache.entrySet()) {
                            String key = entry.getKey(); // format: title-year-director
                            if (key.startsWith(movieTitle + "-") && key.endsWith("-" + director)) {
                                movieId = entry.getValue();
                                break;
                            }
                        }
                        if (movieId == null) {
                            discrepanciesCount++;
                            logInconsistentMovies.println("Cast record references non-existent movie: Title='" + movieTitle + "', Director='" + director + "'");
                            continue;
                        }

                        // Process star linkage: if star not found, insert it (with NULL birthYear)
                        if (!starCache.containsKey(starName)) {
                            String newStarId = "nm" + String.format("%07d", starCache.size() + 1);
                            try {
                                PreparedStatement psStar = connection.prepareStatement(
                                        "INSERT INTO stars(id, name, birthYear) VALUES(?, ?, ?)"
                                );
                                psStar.setString(1, newStarId);
                                psStar.setString(2, starName);
                                psStar.setNull(3, Types.INTEGER);
                                psStar.executeUpdate();
                                psStar.close();
                                starCache.put(starName, newStarId);
                                starsAdded++;
                            } catch (SQLException ex) {
                                duplicateStarsCount++;
                                logDuplicateStars.println("Duplicate star on insert (casts file): " + starName);
                                PreparedStatement psSelect = connection.prepareStatement("SELECT id FROM stars WHERE name = ?");
                                psSelect.setString(1, starName);
                                ResultSet rs = psSelect.executeQuery();
                                if (rs.next()) {
                                    starCache.put(starName, rs.getString("id"));
                                }
                                rs.close();
                                psSelect.close();
                            }
                        }
                        String starId = starCache.get(starName);
                        String linkKey = starId + "_" + movieId;
                        if (!starsInMoviesCache.containsKey(linkKey)) {
                            PreparedStatement psLink = connection.prepareStatement(
                                    "INSERT INTO stars_in_movies(starId, movieId) VALUES(?, ?)"
                            );
                            psLink.setString(1, starId);
                            psLink.setString(2, movieId);
                            psLink.executeUpdate();
                            psLink.close();
                            starsInMoviesCache.put(linkKey, true);
                            starsInMoviesAdded++;
                        }
                    }
                }
            }
        } catch (Exception e) {
            discrepanciesCount++;
            logInconsistentMovies.println("Error parsing " + filename + ": " + e.getMessage());
        }
    }

    // --------------------------
    // Helper: Generate random price between min and max (rounded to 2 decimals)
    // --------------------------
    private static float generateRandomPrice(float min, float max) {
        float raw = (float) (Math.random() * (max - min) + min);
        return (float) (Math.round(raw * 100.0) / 100.0);
    }
}
