CREATE DATABASE IF NOT EXISTS moviedb;
USE moviedb;

CREATE TABLE IF NOT EXISTS movies(
                                     id VARCHAR(10) PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    year INTEGER NOT NULL,
    director VARCHAR(100) NOT NULL
    );

CREATE TABLE IF NOT EXISTS stars(
                                    id VARCHAR(10) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    birthYear INTEGER
    );

CREATE TABLE IF NOT EXISTS stars_in_movies(
                                              starId VARCHAR(10) not null,
    movieId VARCHAR(10) not null,
    FOREIGN KEY (starId) REFERENCES stars(id),
    FOREIGN KEY (movieId) REFERENCES movies(id)
    );

CREATE TABLE IF NOT EXISTS genres(
                                     id INTEGER PRIMARY KEY AUTO_INCREMENT,
                                     name VARCHAR(32) NOT NULL
    );

CREATE TABLE IF NOT EXISTS genres_in_movies(
                                               genreId INTEGER NOT NULL,
                                               movieId VARCHAR(10) NOT NULL,
    FOREIGN KEY (genreId) REFERENCES genres(id),
    FOREIGN KEY (movieId) REFERENCES movies(id)
    );

CREATE TABLE IF NOT EXISTS creditcards(
                                          id VARCHAR(20) PRIMARY KEY,
    firstName VARCHAR(50) NOT NULL,
    lastName VARCHAR(50) NOT NULL,
    expiration date NOT NULL
    );

CREATE TABLE IF NOT EXISTS customers(
                                        id INTEGER PRIMARY KEY AUTO_INCREMENT,
                                        firstName VARCHAR(50) NOT NULL,
    lastName VARCHAR(50) NOT NULL,
    ccId VARCHAR(20) NOT NULL,
    address VARCHAR(200) NOT NULL,
    email VARCHAR(50) NOT NULL,
    password VARCHAR(20) NOT NULL,
    FOREIGN KEY (ccId) REFERENCES creditcards(id)
    );

CREATE TABLE IF NOT EXISTS sales(
                                    id INTEGER PRIMARY KEY AUTO_INCREMENT,
                                    customerId INTEGER NOT NULL,
                                    movieId VARCHAR(10) NOT NULL,
    saleDate date NOT NULL,
    foreign key (customerId) references customers(id),
    foreign key (movieId) references movies(id)
    );

CREATE TABLE IF NOT EXISTS ratings(
                                      movieId VARCHAR(10) NOT NULL,
    rating FLOAT NOT NULL,
    numVotes INTEGER NOT NULL,
    foreign key (movieId) references movies(id)
    );
