# Seed Data

This folder holds the SQL dump and Stanford XML files used to populate the `moviedb` database locally. These files are **not committed** to git because of their size (~26 MB).

## Obtaining the files

### `data.sql`

The SQL dump was originally provided as part of the UCI CS122B course materials. If you have access to the course resources, download it and place it here as `data/data.sql`.

### `stanford-movies/`

The Stanford movie XML files can be downloaded directly:

```bash
mkdir -p data/stanford-movies
curl -o data/stanford-movies/mains243.xml  http://infolab.stanford.edu/pub/movies/mains243.xml
curl -o data/stanford-movies/actors63.xml  http://infolab.stanford.edu/pub/movies/actors63.xml
curl -o data/stanford-movies/casts124.xml  http://infolab.stanford.edu/pub/movies/casts124.xml
```

Source: [Stanford Movie Database DTD references](http://infolab.stanford.edu/pub/movies/dtd.html)

## Usage

Once both files are in place, follow **step 3** in [`LOCAL_SETUP.md`](../LOCAL_SETUP.md) to load the schema and seed the database.
