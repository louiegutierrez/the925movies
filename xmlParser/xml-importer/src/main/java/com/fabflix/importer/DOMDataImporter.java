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

    // For movies: composite key "title-year-director" → movie id (director stored as-is)
    private static Map<String, String> movieCache = new HashMap<>();
    // For genres: genre name → genre id
    private static Map<String, Integer> genreCache = new HashMap<>();
    // For stars: star name → star id
    private static Map<String, String> starCache = new HashMap<>();
    // For stars_in_movies: "starId_movieId" to avoid duplicate links
    private static Map<String, Boolean> starsInMoviesCache = new HashMap<>();

    // Summary counters
    private static int moviesAdded = 0;
    private static int genresAdded = 0;
    private static int starsAdded = 0;
    private static int starsInMoviesAdded = 0;
    private static int genresInMoviesAdded = 0;
    private static int discrepanciesCount = 0;

    private static int inconsistentMoviesCount = 0; // movies missing required fields
    private static int duplicateMoviesCount = 0;    // duplicate movies
    private static int duplicateStarsCount = 0;     // duplicate stars

    // Log files for duplicates (common across files)
    private static PrintWriter logDuplicateMovies;
    private static PrintWriter logDuplicateStars;
    // Separate log files for inconsistencies for each XML file:
    private static PrintWriter logInconsMains;   // for mains243.xml
    private static PrintWriter logInconsActors;  // for actors63.xml
    private static PrintWriter logInconsCasts;   // for casts124.xml
    // Overall summary log:
    private static PrintWriter logSummary;

    // Database connection
    private static Connection connection;

    // --------------------------
    // Custom ErrorHandler to log errors and continue
    // --------------------------
    private static class MyErrorHandler implements ErrorHandler {
        private PrintWriter log;
        public MyErrorHandler(PrintWriter log) {
            this.log = log;
        }
        public void warning(SAXParseException exception) {
            log.println("WARNING: " + exception.getMessage());
        }
        public void error(SAXParseException exception) {
            log.println("ERROR: " + exception.getMessage());
        }
        public void fatalError(SAXParseException exception) {
            log.println("FATAL: " + exception.getMessage());
        }
    }

    // --------------------------
    // Main Method
    // --------------------------
    public static void main(String[] args) {
        try {
            // Initialize logs for each file and overall summary
            logInconsMains = new PrintWriter(new FileWriter("inconsistent_mains243.txt", false));
            logInconsActors = new PrintWriter(new FileWriter("inconsistent_actors63.txt", false));
            logInconsCasts = new PrintWriter(new FileWriter("inconsistent_casts124.txt", false));
            logDuplicateMovies = new PrintWriter(new FileWriter("duplicate_movies.txt", false));
            logDuplicateStars = new PrintWriter(new FileWriter("duplicate_stars.txt", false));
            logSummary = new PrintWriter(new FileWriter("dom_import_summary.txt", false));

            // Connect to DB (adjust credentials as needed)
            String url = "jdbc:mysql://localhost:3306/moviedb?useUnicode=true&characterEncoding=ISO-8859-1";
            String user = "mytestuser";
            String password = "My6$Password";
            connection = DriverManager.getConnection(url, user, password);
            connection.setAutoCommit(false);

            // Pre-load caches from DB
            loadCaches();

            // Process each XML file individually so errors in one don't stop others.
            try {
                parseMovies("standford-movies/mains243.xml", logInconsMains);
            } catch (Exception ex) {
                discrepanciesCount++;
                logInconsMains.println("Error parsing mains243.xml: " + ex.getMessage());
            }
            try {
                parseActors("standford-movies/actors63.xml", logInconsActors);
            } catch (Exception ex) {
                discrepanciesCount++;
                logInconsActors.println("Error parsing actors63.xml: " + ex.getMessage());
            }
            try {
                parseCasts("standford-movies/casts124.xml", logInconsCasts);
            } catch (Exception ex) {
                discrepanciesCount++;
                logInconsCasts.println("Error parsing casts124.xml: " + ex.getMessage());
            }

            // Commit all batch inserts
            connection.commit();

            // Write overall summary log
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

            // Close individual logs and connection
            logInconsMains.close();
            logInconsActors.close();
            logInconsCasts.close();
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

        // Load movies using composite key: title-year-director (director stored as-is)
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
    // Parse mains243.xml (Movies & Genres) with batch insertion and bulk insert for genres_in_movies
    // --------------------------
    private static void parseMovies(String filename, PrintWriter log) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/continue-after-fatal-error", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new MyErrorHandler(log));
            Document doc = builder.parse(new File(filename));
            doc.getDocumentElement().normalize();

            // Prepare a batch for movie inserts.
            PreparedStatement psMovie = connection.prepareStatement(
                    "INSERT INTO movies(id, title, year, director, price) VALUES(?, ?, ?, ?, ?)"
            );
            // Accumulate bulk insert rows for genres_in_movies.
            StringBuilder bulkGIM = new StringBuilder();
            int bulkCount = 0;

            NodeList directorfilmsList = doc.getElementsByTagName("directorfilms");
            for (int i = 0; i < directorfilmsList.getLength(); i++) {
                Element directorfilmsElem = (Element) directorfilmsList.item(i);
                String director = "";
                NodeList dirNameList = directorfilmsElem.getElementsByTagName("dirname");
                if (dirNameList.getLength() > 0) {
                    director = dirNameList.item(0).getTextContent().trim();
                }
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
                    // Validate required fields.
                    if (filmTitle.isEmpty() || filmYear.isEmpty() || director.isEmpty()) {
                        inconsistentMoviesCount++;
                        discrepanciesCount++;
                        log.println("Inconsistent movie info. Title='" + filmTitle +
                                "', Year='" + filmYear + "', Director='" + director + "'");
                        continue;
                    }
                    try {
                        Integer.parseInt(filmYear);
                    } catch (NumberFormatException nfe) {
                        discrepanciesCount++;
                        log.println("Invalid year for film: " + filmTitle + " => " + filmYear);
                        continue;
                    }
                    String compositeKey = filmTitle + "-" + filmYear + "-" + director;
                    if (movieCache.containsKey(compositeKey)) {
                        duplicateMoviesCount++;
                        logDuplicateMovies.println("Duplicate movie: " + filmTitle + " (" + filmYear + ", " + director + ")");
                        continue;
                    }
                    String newMovieId = "tt" + String.format("%07d", movieCache.size() + 1);
                    float randomPrice = generateRandomPrice(5f, 200f);

                    // Add movie to batch.
                    psMovie.setString(1, newMovieId);
                    psMovie.setString(2, filmTitle);
                    psMovie.setInt(3, Integer.parseInt(filmYear));
                    psMovie.setString(4, director);
                    psMovie.setFloat(5, randomPrice);
                    psMovie.addBatch();

                    movieCache.put(compositeKey, newMovieId);
                    moviesAdded++;

                    // Process genres and accumulate linking rows.
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
                            // Accumulate the row for bulk insert.
                            if (bulkGIM.length() == 0) {
                                bulkGIM.append("INSERT INTO genres_in_movies(genreId, movieId) VALUES ");
                            } else {
                                bulkGIM.append(", ");
                            }
                            bulkGIM.append("(").append(genreId).append(", '").append(newMovieId).append("')");
                            bulkCount++;
                            genresInMoviesAdded++;
                        }
                    }
                }
            }
            // Execute the movies batch first.
            psMovie.executeBatch();
            psMovie.close();

            // Now, if we have accumulated any genres_in_movies rows, execute the bulk insert.
            if (bulkGIM.length() > 0) {
                executeBulkInsert(bulkGIM, log);
            }
        } catch (Exception e) {
            discrepanciesCount++;
            log.println("Error parsing " + filename + ": " + e.getMessage());
        }
    }

    // Helper method for bulk inserting into genres_in_movies.
    private static void executeBulkInsert(StringBuilder bulkGIM, PrintWriter log) {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(bulkGIM.toString());
        } catch (SQLException ex) {
            discrepanciesCount++;
            log.println("Error executing bulk insert for genres_in_movies: " + ex.getMessage());
        }
    }

    // --------------------------
    // Parse actors63.xml (Stars) with batch insertion
    // --------------------------
    private static void parseActors(String filename, PrintWriter log) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/continue-after-fatal-error", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new MyErrorHandler(log));
            Document doc = builder.parse(new File(filename));
            doc.getDocumentElement().normalize();

            PreparedStatement psStar = connection.prepareStatement(
                    "INSERT INTO stars(id, name, birthYear) VALUES(?, ?, ?)"
            );

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
                    log.println("Actor missing stagename.");
                    continue;
                }
                if (starCache.containsKey(starName)) {
                    duplicateStarsCount++;
                    logDuplicateStars.println("Duplicate star: " + starName);
                    continue;
                }

                String newStarId = "nm" + String.format("%07d", starCache.size() + 1);
                psStar.setString(1, newStarId);
                psStar.setString(2, starName);
                if (birthYear.isEmpty()) {
                    psStar.setNull(3, Types.INTEGER);
                } else {
                    try {
                        psStar.setInt(3, Integer.parseInt(birthYear));
                    } catch (NumberFormatException nfe) {
                        discrepanciesCount++;
                        log.println("Invalid birthYear for star '" + starName + "': " + birthYear);
                        psStar.setNull(3, Types.INTEGER);
                    }
                }
                psStar.addBatch();
                starCache.put(starName, newStarId);
                starsAdded++;
            }
            psStar.executeBatch();
            psStar.close();
        } catch (Exception e) {
            discrepanciesCount++;
            log.println("Error parsing " + filename + ": " + e.getMessage());
        }
    }

    // --------------------------
    // Parse casts124.xml (Stars_in_Movies) with batch insertion for linkings
    // --------------------------
    private static void parseCasts(String filename, PrintWriter log) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/continue-after-fatal-error", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new MyErrorHandler(log));
            Document doc = builder.parse(new File(filename));
            doc.getDocumentElement().normalize();

            PreparedStatement psLink = connection.prepareStatement(
                    "INSERT INTO stars_in_movies(starId, movieId) VALUES(?, ?)"
            );

            NodeList dirfilmsList = doc.getElementsByTagName("dirfilms");
            for (int i = 0; i < dirfilmsList.getLength(); i++) {
                Element dirfilmsElem = (Element) dirfilmsList.item(i);
                String director = "";
                NodeList isList = dirfilmsElem.getElementsByTagName("is");
                if (isList.getLength() > 0) {
                    director = isList.item(0).getTextContent().trim();
                }
                NodeList filmcList = dirfilmsElem.getElementsByTagName("filmc");
                for (int j = 0; j < filmcList.getLength(); j++) {
                    Element filmcElem = (Element) filmcList.item(j);
                    NodeList mList = filmcElem.getElementsByTagName("m");
                    for (int k = 0; k < mList.getLength(); k++) {
                        Element mElem = (Element) mList.item(k);
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
                        // Look for a movie in our cache that exactly matches "movieTitle-year-director"
                        String compositeKeyPrefix = movieTitle + "-";
                        String movieId = null;
                        for (Map.Entry<String, String> entry : movieCache.entrySet()) {
                            String key = entry.getKey();
                            if (key.startsWith(compositeKeyPrefix) && key.endsWith("-" + director)) {
                                movieId = entry.getValue();
                                break;
                            }
                        }
                        if (movieId == null) {
                            discrepanciesCount++;
                            log.println("Cast record references non-existent movie: Title='" + movieTitle + "', Director='" + director + "'");
                            continue;
                        }
                        if (!starCache.containsKey(starName)) {
                            discrepanciesCount++;
                            log.println("Cast record references non-existent star: " + starName + " for movie '" + movieTitle + "'");
                            continue;
                        }
                        String starId = starCache.get(starName);
                        String linkKey = starId + "_" + movieId;
                        if (!starsInMoviesCache.containsKey(linkKey)) {
                            psLink.setString(1, starId);
                            psLink.setString(2, movieId);
                            psLink.addBatch();
                            starsInMoviesCache.put(linkKey, true);
                            starsInMoviesAdded++;
                        }
                    }
                }
            }
            psLink.executeBatch();
            psLink.close();
        } catch (Exception e) {
            discrepanciesCount++;
            log.println("Error parsing " + filename + ": " + e.getMessage());
        }
    }

    private static float generateRandomPrice(float min, float max) {
        float raw = (float) (Math.random() * (max - min) + min);
        return (float) (Math.round(raw * 100.0) / 100.0);
    }
}
