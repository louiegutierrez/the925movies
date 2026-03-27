-- =============================================================
-- FabFlix (moviedb) Database Schema
-- Reconstructed from servlet SQL queries, stored procedures,
-- and the XML data importer.
-- =============================================================

CREATE DATABASE IF NOT EXISTS moviedb;
USE moviedb;

-- -------------------------------------------------------------
-- movies
-- Primary data about each film.
-- id follows the IMDb "tt" format (e.g. tt0000001).
-- price is assigned randomly at insert time (stored procedure
-- and XML importer both set it).
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS movies (
    id        VARCHAR(10)    NOT NULL,
    title     VARCHAR(100)   NOT NULL,
    year      INT            NOT NULL,
    director  VARCHAR(100)   NOT NULL,
    price     DECIMAL(5, 2)  NOT NULL DEFAULT 0.00,
    PRIMARY KEY (id),
    FULLTEXT INDEX ft_title (title)   -- required by MATCH...AGAINST search
);

-- -------------------------------------------------------------
-- ratings
-- One row per movie; populated from the Stanford dataset.
-- numVotes is part of the original dataset even though the
-- application only displays rating.
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ratings (
    movieId   VARCHAR(10)  NOT NULL,
    rating    FLOAT        NOT NULL DEFAULT 0.0,
    numVotes  INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (movieId),
    CONSTRAINT fk_ratings_movie FOREIGN KEY (movieId) REFERENCES movies (id)
        ON DELETE CASCADE ON UPDATE CASCADE
);

-- -------------------------------------------------------------
-- stars
-- Actors / performers.
-- id follows the IMDb "nm" format (e.g. nm0000001).
-- birthYear is nullable (many entries omit it).
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS stars (
    id         VARCHAR(10)  NOT NULL,
    name       VARCHAR(100) NOT NULL,
    birthYear  INT          NULL,
    PRIMARY KEY (id)
);

-- -------------------------------------------------------------
-- genres
-- Lookup table for genre names.
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS genres (
    id    INT          NOT NULL AUTO_INCREMENT,
    name  VARCHAR(32)  NOT NULL,
    PRIMARY KEY (id)
);

-- -------------------------------------------------------------
-- genres_in_movies
-- Many-to-many join between genres and movies.
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS genres_in_movies (
    genreId  INT          NOT NULL,
    movieId  VARCHAR(10)  NOT NULL,
    PRIMARY KEY (genreId, movieId),
    CONSTRAINT fk_gim_genre FOREIGN KEY (genreId) REFERENCES genres (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_gim_movie FOREIGN KEY (movieId) REFERENCES movies (id)
        ON DELETE CASCADE ON UPDATE CASCADE
);

-- -------------------------------------------------------------
-- stars_in_movies
-- Many-to-many join between stars and movies.
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS stars_in_movies (
    starId   VARCHAR(10)  NOT NULL,
    movieId  VARCHAR(10)  NOT NULL,
    PRIMARY KEY (starId, movieId),
    CONSTRAINT fk_sim_star  FOREIGN KEY (starId)  REFERENCES stars (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_sim_movie FOREIGN KEY (movieId) REFERENCES movies (id)
        ON DELETE CASCADE ON UPDATE CASCADE
);

-- -------------------------------------------------------------
-- creditcards
-- Payment card data used during checkout.
-- expiration stored as DATE; application passes it as a string
-- (PaymentServlet compares with "expiration = ?").
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS creditcards (
    id          VARCHAR(20)  NOT NULL,
    firstName   VARCHAR(50)  NOT NULL,
    lastName    VARCHAR(50)  NOT NULL,
    expiration  DATE         NOT NULL,
    PRIMARY KEY (id)
);

-- -------------------------------------------------------------
-- customers
-- Registered shoppers.
-- Column order must match data.sql's positional INSERT VALUES.
-- password is stored as a Jasypt StrongPasswordEncryptor hash
-- at runtime; seed data contains plaintext that must be hashed
-- after import (see LOCAL_SETUP.md step 3).
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS customers (
    id        INT          NOT NULL AUTO_INCREMENT,
    firstName VARCHAR(50)  NOT NULL DEFAULT '',
    lastName  VARCHAR(50)  NOT NULL DEFAULT '',
    ccId      VARCHAR(20)  NOT NULL DEFAULT '',
    address   VARCHAR(200) NOT NULL DEFAULT '',
    email     VARCHAR(50)  NOT NULL,
    password  VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_customers_email (email),
    CONSTRAINT fk_customers_cc FOREIGN KEY (ccId) REFERENCES creditcards (id)
        ON DELETE CASCADE ON UPDATE CASCADE
);

-- -------------------------------------------------------------
-- employees
-- Dashboard / admin users.
-- password is stored as a Jasypt StrongPasswordEncryptor hash.
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS employees (
    email     VARCHAR(50)   NOT NULL,
    password  VARCHAR(255)  NOT NULL,
    PRIMARY KEY (email)
);

-- -------------------------------------------------------------
-- sales
-- One row per movie purchased per transaction.
-- saleDate is set to NOW() at insert time (PaymentServlet).
-- quantity reflects the cart quantity at checkout.
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sales (
    id          INT          NOT NULL AUTO_INCREMENT,
    customerId  INT          NOT NULL,
    movieId     VARCHAR(10)  NOT NULL,
    quantity    INT          NOT NULL DEFAULT 1,
    saleDate    DATE         NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_sales_customer FOREIGN KEY (customerId) REFERENCES customers (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_sales_movie    FOREIGN KEY (movieId)    REFERENCES movies (id)
        ON DELETE CASCADE ON UPDATE CASCADE
);
