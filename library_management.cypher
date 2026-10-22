
CREATE CONSTRAINT member_id IF NOT EXISTS FOR (n:Member) REQUIRE n.MemberID IS UNIQUE;
CREATE CONSTRAINT librarian_id IF NOT EXISTS FOR (n:Librarian) REQUIRE n.LibrarianID IS UNIQUE;
CREATE CONSTRAINT publisher_id IF NOT EXISTS FOR (n:Publisher) REQUIRE n.PublisherID IS UNIQUE;
CREATE CONSTRAINT book_id IF NOT EXISTS FOR (n:Book) REQUIRE n.BookID IS UNIQUE;
CREATE CONSTRAINT borrowing_id IF NOT EXISTS FOR (n:Borrowing) REQUIRE n.BorrowingID IS UNIQUE;
CREATE CONSTRAINT rating_id IF NOT EXISTS FOR (n:Rating) REQUIRE n.RatingID IS UNIQUE;

LOAD CSV WITH HEADERS FROM 'file:///MEMBER.csv' AS row
CREATE (:Member {
  MemberID: toInteger(row.MemberID),
  FirstName: row.FirstName,
  MiddleName: CASE WHEN row.MiddleName = '' THEN null ELSE row.MiddleName END,
  LastName: row.LastName,
  MemberPhone: row.MemberPhone,
  MemberEmail: row.MemberEmail
});

LOAD CSV WITH HEADERS FROM 'file:///LIBRARIAN.csv' AS row
CREATE (:Librarian {
  LibrarianID: toInteger(row.LibrarianID),
  LibrarianName: row.LibrarianName,
  LibrarianPhone: row.LibrarianPhone,
  LibrarianEmail: row.LibrarianEmail
});

LOAD CSV WITH HEADERS FROM 'file:///PUBLISHER.csv' AS row
CREATE (:Publisher {
  PublisherID: toInteger(row.PublisherID),
  PublisherName: row.PublisherName,
  PublisherAddress: row.PublisherAddress,
  PublisherPhone: row.PublisherPhone
});

LOAD CSV WITH HEADERS FROM 'file:///BOOK.csv' AS row
CREATE (:Book {
  BookID: toInteger(row.BookID),
  BookTitle: row.BookTitle,
  ISBN: row.ISBN,
  PublisherID: CASE WHEN row.PublisherID = '' THEN null ELSE toInteger(row.PublisherID) END
});

LOAD CSV WITH HEADERS FROM 'file:///BORROWING.csv' AS row
CREATE (:Borrowing {
  BorrowingID: toInteger(row.BorrowingID),
  MemberID: toInteger(row.MemberID),
  BookID: toInteger(row.BookID),
  IssueDate: date(row.IssueDate),
  LibrarianID: toInteger(row.LibrarianID),
  DueDate: date(row.DueDate),
  ReturnDate: CASE WHEN row.ReturnDate = '' THEN null ELSE date(row.ReturnDate) END,
  Status: row.Status,
  FineAmount: toFloat(row.FineAmount)
});

LOAD CSV WITH HEADERS FROM 'file:///RATING.csv' AS row
CREATE (:Rating {
  RatingID: toInteger(row.RatingID),
  MemberID: toInteger(row.MemberID),
  LibrarianID: toInteger(row.LibrarianID),
  Rating: CASE WHEN row.Rating = '' THEN null ELSE toInteger(row.Rating) END,
  Review: row.Review,
  RatingDate: CASE WHEN row.RatingDate = '' THEN null ELSE date(row.RatingDate) END
});

MATCH (b:Book), (p:Publisher)
WHERE b.PublisherID = p.PublisherID
CREATE (b)-[:PUBLISHED_BY]->(p);

MATCH (br:Borrowing), (m:Member)
WHERE br.MemberID = m.MemberID
CREATE (m)-[:MADE_BORROWING]->(br);

MATCH (br:Borrowing), (b:Book)
WHERE br.BookID = b.BookID
CREATE (br)-[:BORROWED_BOOK]->(b);

MATCH (br:Borrowing), (l:Librarian)
WHERE br.LibrarianID = l.LibrarianID
CREATE (br)-[:PROCESSED_BY]->(l);

MATCH (r:Rating), (m:Member)
WHERE r.MemberID = m.MemberID
CREATE (m)-[:GAVE_RATING]->(r);

MATCH (r:Rating), (l:Librarian)
WHERE r.LibrarianID = l.LibrarianID
CREATE (r)-[:RATES_LIBRARIAN]->(l);


MATCH (n)
RETURN labels(n)[0] AS NodeType, count(*) AS Total
ORDER BY NodeType;

MATCH (n) RETURN count(n) AS TotalNodes;
