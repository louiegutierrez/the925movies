DELIMITER $$
CREATE PROCEDURE add_movie(
    IN in_title VARCHAR(100),
    IN in_year INT,
    IN in_director VARCHAR(100),
    IN in_star_name VARCHAR(100),
    IN in_genre_name VARCHAR(32)
)
proc: BEGIN
    DECLARE newMovieId VARCHAR(10);
    DECLARE existingMovieId VARCHAR(10);
    DECLARE newStarId VARCHAR(10);
    DECLARE existingStarId VARCHAR(10);
    DECLARE newGenreId INT;
    DECLARE existingGenreId INT;
    DECLARE randomPrice DECIMAL(5,2);

    SELECT m.id INTO existingMovieId
    FROM movies m
    WHERE m.title = in_title AND m.year = in_year AND m.director = in_director
    LIMIT 1;

    IF existingMovieId IS NOT NULL THEN
        SELECT CONCAT("Movie already exists with id: ", existingMovieId) AS message;
        LEAVE proc;
    END IF;

    SET newMovieId = (SELECT CONCAT("tt", LPAD(CAST(SUBSTR(MAX(id), 3) AS UNSIGNED) + 1, 7, '0')) FROM movies LIMIT 1);
    IF newMovieId IS NULL THEN
        SET newMovieId = 'tt0000001';
    END IF;
    SET randomPrice = ROUND(RAND() * 195 + 5, 2);
    INSERT INTO movies(id, title, year, director, price)
    VALUES(newMovieId, in_title, in_year, in_director, randomPrice);


    SELECT s.id INTO existingStarId
    FROM stars s
    WHERE s.name = in_star_name
    LIMIT 1;

    IF existingStarId IS NULL THEN
        SET newStarId = (SELECT CONCAT("nm", LPAD(CAST(SUBSTR(MAX(id), 3) AS UNSIGNED) + 1, 7, '0')) FROM stars LIMIT 1);
        IF newStarId IS NULL THEN
            SET newStarId = 'nm0000001';
        END IF;

        INSERT INTO stars(id, name)
        VALUES(newStarId, in_star_name);
    ELSE
        SET newStarId = existingStarId;
    END IF;

    INSERT INTO stars_in_movies(starId, movieId) VALUES(newStarId, newMovieId);


    SELECT g.id INTO existingGenreId
    FROM genres g
    WHERE g.name = in_genre_name
    LIMIT 1;

    IF existingGenreId IS NULL THEN
        INSERT INTO genres(name) VALUES(in_genre_name);
        SET newGenreId = LAST_INSERT_ID();
    ELSE
        SET newGenreId = existingGenreId;
    END IF;

    INSERT INTO genres_in_movies(genreId, movieId) VALUES(newGenreId, newMovieId);

    SELECT CONCAT(
                   "New movie added with id: ", newMovieId,
                   ", GENRE_ID: ", IF(existingGenreId IS NOT NULL, existingGenreId, newGenreId),
                   ", STAR_ID: ", IF(existingStarId IS NOT NULL, existingStarId, newStarId)
           ) AS message;
END$$
DELIMITER ;
