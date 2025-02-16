import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class DOMDataImporter {

    // -----------------------------------------------------------------------------------
    // CACHES & COUNTERS
    // -----------------------------------------------------------------------------------

    // For movies: key = "title-year-director", value = movie id
    private static final Map<String, String> movieCache = new HashMap<>();
    // For genres: key = genre name, value = genre id
    private static final Map<String, Integer> genreCache = new HashMap<>();
    // For stars: key = star name, value = star id
    private static final Map<String, String> starCache = new HashMap<>();
    // For stars_in_movies: key = starId + "_" + movieId (to avoid duplicates)
    private static final Map<String, Boolean> starsInMoviesCache = new HashMap<>();

    // Summary counters
    private static int moviesAdded = 0;
    private static int genresAdded = 0;
    private static int starsAdded = 0;
    private static int starsInMoviesAdded = 0;
    private static int genresInMoviesAdded = 0;
    private static int discrepanciesCount = 0;

    // Detailed discrepancy counters
    private static int inconsistentMoviesCount = 0;  // Movies missing required fields
    private static int duplicateMoviesCount = 0;     // Movies already in DB
    private static int duplicateStarsCount = 0;      // Stars already in DB

    // Logs for each category
    private static PrintWriter logInconsistentMovies; // Missing required fields
    private static PrintWriter logDuplicateMovies;    // Duplicate movies
    private static PrintWriter logDuplicateStars;     // Duplicate stars
    private static PrintWriter logSummary;            // Overall summary

    // Database connection
    private static Connection connection;

    // -----------------------------------------------------------------------------------
    // MAIN METHOD
    // -----------------------------------------------------------------------------------

    public static void main(String[] args) {
        try {
            // Initialize log files
            logInconsistentMovies = new PrintWriter(new FileWriter("inconsistent_movies.txt", false));
            logDuplicateMovies = new PrintWriter(new FileWriter("duplicate_movies.txt", false));
            logDuplicateStars = new PrintWriter(new FileWriter("duplicate_stars.txt", false));
            logSummary = new PrintWriter(new FileWriter("dom_import_summary.txt", false));

            // Connect to DB (adjust credentials)
            String url = "jdbc:mysql://localhost:3306/moviedb?useUnicode=true&characterEncoding=ISO-8859-1";
            String user = "mytestuser";
            String password = "My6$Password";
            connection = DriverManager.getConnection(url, user, password);

            // Preload caches
            loadCaches();

            // Parse XML files
            parseMovies("mains243.xml");
            parseActors("actors63.xml");
            parseCasts("casts124.xml");

            // Write summary
            logSummary.println("=== DOM Import Summary ===");
            logSummary.println("Movies Added: " + moviesAdded);
            logSummary.println("  Inconsistent Movies: " + inconsistentMoviesCount);
            logSummary.println("  Duplicate Movies: " + duplicateMoviesCount);
            logSummary.println("Genres Added: " + genresAdded);
            logSummary.println("Stars Added: " + starsAdded);
            logSummary.println("  Duplicate Stars: " + duplicateStarsCount);
            logSummary.println("Stars_in_Movies Added: " + starsInMoviesAdded);
            logSummary.println("Genres_in_Movies Added: " + genresInMoviesAdded);
            logSummary.println("Total Discrepancies: " + discrepanciesCount);
            logSummary.close();

            // Close other logs
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

    // -----------------------------------------------------------------------------------
    // LOAD CACHES
    // -----------------------------------------------------------------------------------

    private static void loadCaches() throws SQLException {
        Statement stmt = connection.createStatement();

        // Load movies
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

    // -----------------------------------------------------------------------------------
    // PARSE MOVIES
    // -----------------------------------------------------------------------------------

    private static void parseMovies(String filename) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new File(filename));
            doc.getDocumentElement().normalize();

            NodeList directorfilmsList = doc.getElementsByTagName("directorfilms");
            for (int i = 0; i < directorfilmsList.getLength(); i++) {
                Element directorfilmsElem = (Element) directorfilmsList.item(i);

                // Get <dirname>
                String director = "";
                NodeList dirNameList = directorfilmsElem.getElementsByTagName("dirname");
                if (dirNameList.getLength() > 0) {
                    director = dirNameList.item(0).getTextContent().trim();
                }

                // For each <film>
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

                    // Check required fields
                    if (filmTitle.isEmpty() || filmYear.isEmpty() || director.isEmpty()) {
                        // Missing required fields => inconsistent
                        inconsistentMoviesCount++;
                        discrepanciesCount++;
                        logInconsistentMovies.println(
                                "Missing fields for movie. Title='" + filmTitle + "', Year='" + filmYear + "', Director='" + director + "'"
                        );
                        continue;
                    }

                    // Build composite key
                    String key = filmTitle + "-" + filmYear + "-" + director;
                    if (movieCache.containsKey(key)) {
                        // Duplicate movie
                        duplicateMoviesCount++;
                        logDuplicateMovies.println("Duplicate movie: " + filmTitle + " (" + filmYear + ", " + director + ")");
                        continue;
                    }

                    // Insert new movie
                    String newMovieId = "tt" + String.format("%07d", movieCache.size() + 1);

                    // Generate random price
                    float randomPrice = generateRandomPrice(5f, 200f);

                    PreparedStatement ps = connection.prepareStatement(
                            "INSERT INTO movies(id, title, year, director, price) VALUES(?, ?, ?, ?, ?)"
                    );
                    ps.setString(1, newMovieId);
                    ps.setString(2, filmTitle);
                    try {
                        ps.setInt(3, Integer.parseInt(filmYear));
                    } catch (NumberFormatException nfe) {
                        // If year is invalid, treat as null
                        discrepanciesCount++;
                        logInconsistentMovies.println("Invalid year for film: " + filmTitle + " => " + filmYear);
                        ps.setNull(3, Types.INTEGER);
                    }
                    ps.setString(4, director);
                    ps.setFloat(5, randomPrice);
                    ps.executeUpdate();
                    ps.close();

                    movieCache.put(key, newMovieId);
                    moviesAdded++;

                    // Insert genres from <cats>
                    NodeList catsList = filmElem.getElementsByTagName("cats");
                    if (catsList.getLength() > 0) {
                        Element catsElem = (Element) catsList.item(0);
                        NodeList catList = catsElem.getElementsByTagName("cat");
                        for (int k = 0; k < catList.getLength(); k++) {
                            String genreName = catList.item(k).getTextContent().trim();
                            if (genreName.isEmpty()) {
                                continue;
                            }
                            int genreId;
                            if (!genreCache.containsKey(genreName)) {
                                // Insert new genre
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
                                    // We skip if we couldn't insert
                                    continue;
                                }
                            } else {
                                genreId = genreCache.get(genreName);
                            }
                            // Insert into genres_in_movies
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
            logInconsistentMovies.println("Error parsing '" + filename + "': " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------------------------
    // PARSE ACTORS
    // -----------------------------------------------------------------------------------

    private static void parseActors(String filename) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
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

                // If stagename is missing => treat as missing required field
                if (starName.isEmpty()) {
                    discrepanciesCount++;
                    logInconsistentMovies.println("Actor with missing stagename (required field).");
                    continue;
                }

                // Check if star is already known => duplicate star
                if (starCache.containsKey(starName)) {
                    duplicateStarsCount++;
                    logDuplicateStars.println("Duplicate star: " + starName);
                    continue;
                }

                // Insert new star
                String newStarId = "nm" + String.format("%07d", starCache.size() + 1);
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

                starCache.put(starName, newStarId);
                starsAdded++;
            }
        } catch (Exception e) {
            discrepanciesCount++;
            logInconsistentMovies.println("Error parsing '" + filename + "': " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------------------------
    // PARSE CASTS
    // -----------------------------------------------------------------------------------

    private static void parseCasts(String filename) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new File(filename));
            doc.getDocumentElement().normalize();

            NodeList mList = doc.getElementsByTagName("m");
            for (int i = 0; i < mList.getLength(); i++) {
                Element mElem = (Element) mList.item(i);

                // Extract film id <f> and actor name <a>
                String filmId = "";
                String actorName = "";

                NodeList fList = mElem.getElementsByTagName("f");
                if (fList.getLength() > 0) {
                    filmId = fList.item(0).getTextContent().trim();
                }
                NodeList aList = mElem.getElementsByTagName("a");
                if (aList.getLength() > 0) {
                    actorName = aList.item(0).getTextContent().trim();
                }

                // If missing filmId or actorName => skip silently (no extra logs)
                if (filmId.isEmpty() || actorName.isEmpty()) {
                    continue;
                }

                // Must exist in movieCache + starCache to proceed
                if (!movieCache.containsValue(filmId)) {
                    // The cast references a movie id not in the DB => skip
                    continue;
                }
                if (!starCache.containsKey(actorName)) {
                    // The cast references a star name not in the DB => skip
                    continue;
                }

                String starId = starCache.get(actorName);
                String key = starId + "_" + filmId;

                // If not already linked, link star -> movie
                if (!starsInMoviesCache.containsKey(key)) {
                    PreparedStatement ps = connection.prepareStatement(
                            "INSERT INTO stars_in_movies(starId, movieId) VALUES(?, ?)"
                    );
                    ps.setString(1, starId);
                    ps.setString(2, filmId);
                    ps.executeUpdate();
                    ps.close();
                    starsInMoviesCache.put(key, true);
                    starsInMoviesAdded++;
                }
            }
        } catch (Exception e) {
            discrepanciesCount++;
            logInconsistentMovies.println("Error parsing '" + filename + "': " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------------------------
    // HELPER: RANDOM PRICE
    // -----------------------------------------------------------------------------------

    /**
     * Generates a random float between min and max, inclusive of min, exclusive of max,
     * rounded to 2 decimal places.
     */
    private static float generateRandomPrice(float min, float max) {
        float raw = (float) (Math.random() * (max - min) + min);  // e.g. [5, 200)
        // Round to 2 decimals
        return (float) (Math.round(raw * 100.0) / 100.0);
    }
}
