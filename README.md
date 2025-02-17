# CS122B PROJECT 3
## BY LEONARDO GUTIERREZ && MARTIN KIMBALL

#### VIDEO URL : https://www.youtube.com/watch?v=nhcdHmuJ6-U

#### MARTIN CONTRIBUTIONS:
- Frontend for dashboard
- Updated LoginFilter
- Updated Login to include employee/user
- Recaptcha implementation

#### LEONARDO CONTRIBUTIONS:
- Majority of XML Parsing
- Added https support
- Custom domain implementation
- Handled encrypted passwords
- Dashboard backend

#### FILES W/ PreparedStatement
- CartServlet (shopping cart)
- FormServlet (Login Form)
- GenreServlet (Getting Genres)
- GetMetadataServlet (MetaData)
- InsertStarServlet (Inserting New Star)
- PaymentServlet (Making Payment to Sales)
- SearchingServlet (Main Searching)
- SingleMovieServlet (Getting Single Movie Info)
- SingleStarServlet (Getting Single Star info)

#### XML SPEED OPTIMIZATIONS:
- Caching database into a map to do one query per movie, genres, stars
- Batch inserts to keep sql inserts at a minimum and inserting at large amounts


#### XML REPORT:
=== DOM Import Summary ===
Movies Added: 12058
Inconsistent Movies (missing required fields): 13
Duplicate Movies: 28
Genres Added: 123
Stars Added: 6006
Duplicate Stars: 857
Stars_in_Movies Added: 28137
Genres_in_Movies Added: 9809
Total Discrepancies: 26220