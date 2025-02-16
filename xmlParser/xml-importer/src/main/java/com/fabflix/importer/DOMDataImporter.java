import org.w3c.dom.*;

import javax.naming.InitialContext;
import javax.sql.DataSource;
import javax.xml.parsers.*;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class DOMDataImporter {

    // In-memory caches (Optimization 1)
    // For movies: key = "title-year-director", value = movie id
    private final static Map<String, String> movieCache = new HashMap<>();
    // For genres: key = genre name, value = genre id
    private final static Map<String, Integer> genreCache = new HashMap<>();
    // For stars: key = star name, value = star id
    private final static Map<String, String> starCache = new HashMap<>();
    // For stars_in_movies: key = starId + "_" + movieId, to avoid duplicate insertions
    private final static Map<String, Boolean> starsInMoviesCache = new HashMap<>();

    // Counters for logging and summary
    private static int moviesAdded = 0;
    private static int genresAdded = 0;
    private static int starsAdded = 0;
    private static int starsInMoviesAdded = 0;
    private static int genresInMoviesAdded = 0;
    private static int discrepanciesCount = 0;

    // Detailed discrepancy counters
    private static int inconsistentMoviesCount = 0;  // movies with missing required fields
    private static int duplicateMoviesCount = 0;     // movies already in the database
    private static int moviesWithNoStarsCount = 0;     // cast entries with missing star info
    private static int moviesNotFoundCount = 0;        // movies referenced in casts not found
    private static int duplicateStarsCount = 0;        // duplicate star records in actors file
    private static int starsNotFoundCount = 0;         // stars referenced in casts not found

    // Separate log files for each category
    private static PrintWriter logInconsistentMovies;
    private static PrintWriter logDuplicateMovies;
    private static PrintWriter logMoviesWithNoStars;
    private static PrintWriter logMoviesNotFound;
    private static PrintWriter logDuplicateStars;
    private static PrintWriter logStarsNotFound;
    private static PrintWriter logSummary;

    // Database connection (adjust URL, user, and password as needed)
    private static Connection connection;

    public static void main(String[] args) {
        try {
            // Initialize individual log files
            logInconsistentMovies = new PrintWriter(new FileWriter("inconsistent_movies.txt", false));
            logDuplicateMovies = new PrintWriter(new FileWriter("duplicate_movies.txt", false));
            logMoviesWithNoStars = new PrintWriter(new FileWriter("movies_with_no_stars.txt", false));
            logMoviesNotFound = new PrintWriter(new FileWriter("movies_not_found.txt", false));
            logDuplicateStars = new PrintWriter(new FileWriter("duplicate_stars.txt", false));
            logStarsNotFound = new PrintWriter(new FileWriter("stars_not_found.txt", false));
            logSummary = new PrintWriter(new FileWriter("dom_import_summary.txt", false));

            // Connect to the database
            String url = "jdbc:mysql://localhost:3306/moviedb?useUnicode=true&characterEncoding=ISO-8859-1";
            String user = "mytestuser";
            String password = "My6$Password";
            connection = DriverManager.getConnection(url, user, password);

            // Pre-load caches from DB (Optimization 1)
            loadCaches();

            // Parse XML files using DOM
            parseMovies();
            parseActors();
            parseCasts();

            // Write summary to logSummary
            logSummary.println("=== DOM Import Summary ===");
            logSummary.println("Movies Added: " + moviesAdded);
            logSummary.println("  Inconsistent Movies: " + inconsistentMoviesCount);
            logSummary.println("  Duplicate Movies: " + duplicateMoviesCount);
            logSummary.println("Genres Added: " + genresAdded);
            logSummary.println("Stars Added: " + starsAdded);
            logSummary.println("  Duplicate Stars: " + duplicateStarsCount);
            logSummary.println("Stars_in_Movies Added: " + starsInMoviesAdded);
            logSummary.println("Genres_in_Movies Added: " + genresInMoviesAdded);
            logSummary.println("Movies with No Stars (in cast file): " + moviesWithNoStarsCount);
            logSummary.println("Movies Not Found (in cast file): " + moviesNotFoundCount);
            logSummary.println("Stars Not Found (in cast file): " + starsNotFoundCount);
            logSummary.println("Total Discrepancies: " + discrepanciesCount);
            logSummary.close();

            // Close all log files
            logInconsistentMovies.close();
            logDuplicateMovies.close();
            logMoviesWithNoStars.close();
            logMoviesNotFound.close();
            logDuplicateStars.close();
            logStarsNotFound.close();

            connection.close();
            System.out.println("DOM import completed. See the log files for details.");
        } catch (Exception e) {
            e.printStackTrace();
            if (logSummary != null) {
                logSummary.println("Fatal error: " + e.getMessage());
                logSummary.close();
            }
        }
    }

    // Pre-load caches from the database
    private static void loadCaches() throws SQLException {
        Statement stmt = connection.createStatement();

        // Load movies into movieCache (key: title-year-director)
        ResultSet rs = stmt.executeQuery("SELECT id, title, year, director FROM movies");
        while (rs.next()) {
            String key = rs.getString("title").trim() + "-" + rs.getInt("year") + "-" + rs.getString("director").trim();
            movieCache.put(key, rs.getString("id"));
        }
        rs.close();

        // Load genres
        rs = stmt.executeQuery("SELECT id, name FROM genres");
        while (rs.next()) {
            genreCache.put(rs.getString("name").trim(), Integer.valueOf(rs.getInt("id")));
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

    // Parse mains243.xml: Insert movies and their genres.
    private static void parseMovies() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // (If needed, set encoding: factory.setAttribute("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);)
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new File("mains243.xml"));
            doc.getDocumentElement().normalize();

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

                    // Validate required fields
                    if (filmTitle.isEmpty() || filmYear.isEmpty() || director.isEmpty()) {
                        inconsistentMoviesCount++;
                        discrepanciesCount++;
                        logInconsistentMovies.println("Inconsistent movie info. Title: " + filmTitle +
                                ", Year: " + filmYear + ", Director: " + director);
                        continue;
                    }

                    String key = filmTitle + "-" + filmYear + "-" + director;
                    if (movieCache.containsKey(key)) {
                        duplicateMoviesCount++;
                        logDuplicateMovies.println("Duplicate movie: " + filmTitle + " (" + filmYear + ", " + director + ")");
                        continue;
                    }

                    // Insert movie
                    String newMovieId = "tt" + (movieCache.size() + 1);
                    PreparedStatement ps = connection.prepareStatement(
                            "INSERT INTO movies(id, title, year, director) VALUES(?, ?, ?, ?)");
                    ps.setString(1, newMovieId);
                    ps.setString(2, filmTitle);
                    try {
                        ps.setInt(3, Integer.parseInt(filmYear));
                    } catch (NumberFormatException nfe) {
                        discrepanciesCount++;
                        logInconsistentMovies.println("Invalid year for film: " + filmTitle + " Year: " + filmYear);
                        ps.setNull(3, Types.INTEGER);
                    }
                    ps.setString(4, director);
                    ps.executeUpdate();
                    ps.close();

                    movieCache.put(key, newMovieId);
                    moviesAdded++;

                    // Process genres from <cats>
                    NodeList catsList = filmElem.getElementsByTagName("cats");
                    if (catsList.getLength() > 0) {
                        Element catsElem = (Element) catsList.item(0);
                        NodeList catList = catsElem.getElementsByTagName("cat");
                        for (int k = 0; k < catList.getLength(); k++) {
                            String genre = catList.item(k).getTextContent().trim();
                            if (genre.isEmpty())
                                continue;
                            int genreId;
                            if (!genreCache.containsKey(genre)) {
                                PreparedStatement psGenre = connection.prepareStatement(
                                        "INSERT INTO genres(name) VALUES(?)", Statement.RETURN_GENERATED_KEYS);
                                psGenre.setString(1, genre);
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
                                    genreCache.put(genre, Integer.valueOf(genreId));
                                    genresAdded++;
                                } else {
                                    discrepanciesCount++;
                                    // Log under general discrepancies if desired (or skip)
                                    continue;
                                }
                            } else {
                                genreId = genreCache.get(genre);
                            }
                            PreparedStatement psGIM = connection.prepareStatement(
                                    "INSERT INTO genres_in_movies(genreId, movieId) VALUES(?, ?)");
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
            logInconsistentMovies.println("Error parsing movies: " + e.getMessage());
        }
    }

    // Parse actors63.xml: Insert stars.
    private static void parseActors() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // Optionally set encoding if needed: factory.setAttribute("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new File("actors63.xml"));
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
                    logInconsistentMovies.println("Actor with missing stagename.");
                    continue;
                }
                if (starCache.containsKey(starName)) {
                    duplicateStarsCount++;
                    logDuplicateStars.println("Duplicate star: " + starName);
                    continue;
                }
                String newStarId = "nm" + (starCache.size() + 1);
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO stars(id, name, birthYear) VALUES(?, ?, ?)");
                ps.setString(1, newStarId);
                ps.setString(2, starName);
                if (birthYear.isEmpty()) {
                    ps.setNull(3, Types.INTEGER);
                } else {
                    try {
                        ps.setInt(3, Integer.parseInt(birthYear));
                    } catch (NumberFormatException nfe) {
                        discrepanciesCount++;
                        logInconsistentMovies.println("Invalid birthYear for " + starName + ": " + birthYear);
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
            logInconsistentMovies.println("Error parsing actors: " + e.getMessage());
        }
    }

    // Parse casts124.xml: Insert stars_in_movies connections.
    private static void parseCasts() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // Optionally set encoding if needed
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new File("casts124.xml"));
            doc.getDocumentElement().normalize();

            NodeList mList = doc.getElementsByTagName("m");
            for (int i = 0; i < mList.getLength(); i++) {
                Element mElem = (Element) mList.item(i);
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

                if (filmId.isEmpty() || actorName.isEmpty()) {
                    moviesWithNoStarsCount++;
                    discrepanciesCount++;
                    logMoviesWithNoStars.println("Missing film id or actor name in cast element.");
                    continue;
                }

                // For this example, we assume that the film id from casts124.xml corresponds directly to the id in movies.
                if (!movieCache.containsValue(filmId)) {
                    moviesNotFoundCount++;
                    discrepanciesCount++;
                    logMoviesNotFound.println("Movie id " + filmId + " not found for cast.");
                    continue;
                }
                if (!starCache.containsKey(actorName)) {
                    starsNotFoundCount++;
                    discrepanciesCount++;
                    logStarsNotFound.println("Star " + actorName + " not found for cast.");
                    continue;
                }
                String starId = starCache.get(actorName);
                String key = starId + "_" + filmId;
                if (starsInMoviesCache.containsKey(key)) {
                    // Duplicate cast entry – ignore.
                    continue;
                }
                try {
                    PreparedStatement ps = connection.prepareStatement(
                            "INSERT INTO stars_in_movies(starId, movieId) VALUES(?, ?)");
                    ps.setString(1, starId);
                    ps.setString(2, filmId);
                    ps.executeUpdate();
                    ps.close();
                    starsInMoviesCache.put(key, Boolean.valueOf(true));
                    starsInMoviesAdded++;
                } catch (Exception e) {
                    discrepanciesCount++;
                    logInconsistentMovies.println("Error inserting stars_in_movies: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            discrepanciesCount++;
            logInconsistentMovies.println("Error parsing casts: " + e.getMessage());
        }
    }
}
