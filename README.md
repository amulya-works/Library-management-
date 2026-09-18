# 📚 Library Management System

A Java based Library Management System integrated with MySQL to manage books, members, librarians, borrowings, and ratings. The project includes a desktop application and a web-based librarian administration portal.

## 🚀 Features

* **Book Management** – View, add, update, and delete books.
* **Member Management** – Store and manage library member details.
* **Librarian Management** – Maintain librarian information.
* **Borrowing Management** – Track book issues, due dates, returns, and fines.
* **Rating System** – Store member ratings and reviews for librarians.
* **Desktop Application** – View book records through a Java Swing interface.
* **Web Admin Portal** – Manage library records through a browser-based dashboard.
* **MySQL Database** – Store and retrieve library data using JDBC.

## 🛠️ Technologies Used

| Technology       | Purpose                   |
| ---------------- | ------------------------- |
| Java             | Application development   |
| Java Swing       | Desktop GUI               |
| Java HTTP Server | Web administration portal |
| JDBC             | Database connectivity     |
| MySQL            | Database management       |
| HTML & CSS       | Web dashboard interface   |
| VS Code          | Development environment   |

## 📂 Project Structure

```text
Library-management/
│
├── DatabaseConnection.java
├── LibraryApp.java
├── LibrarianWebServer.java
├── library_management.sql
├── .gitignore
└── .vscode/
    └── settings.json
```

## 🗄️ Database Design

The MySQL database is named `library_management`.

### Tables

* `MEMBER` – Stores member information.
* `LIBRARIAN` – Stores librarian information.
* `PUBLISHER` – Stores publisher details.
* `BOOK` – Stores book information.
* `BORROWING` – Tracks book borrowing and returns.
* `RATING` – Stores librarian ratings and reviews.

The database uses primary keys, foreign keys, unique constraints, and a rating validation constraint.

## ⚙️ Requirements

Before running the project, install:

1. Java JDK 8 or later.
2. MySQL Server.
3. MySQL Connector/J.
4. VS Code with Java Extension Pack (optional).

## 🔧 Setup Instructions

### 1. Clone the Repository

```bash
git clone https://github.com/amulya-works/Library-management.git
cd Library-management
```

### 2. Create the Database

Open MySQL and run:

```sql
SOURCE library_management.sql;
```

Alternatively, open `library_management.sql` in MySQL Workbench and execute it.

This creates the database, tables, relationships, and sample records.

### 3. Configure Database Connection

Open `DatabaseConnection.java` and update the MySQL credentials:

```java
private static final String URL =
    "jdbc:mysql://localhost:3306/library_management";

private static final String USER = "root";

private static final String PASSWORD =
    "YOUR_MYSQL_PASSWORD";
```

Use your own MySQL password.

### 4. Add MySQL Connector/J

Download MySQL Connector/J and add the JAR file to your Java classpath.

For VS Code, the project settings reference:

```text
mysql-connector-j-8.3.0.jar
```

Make sure the JAR is available in the project or configured library path.

## ▶️ How to Run

### Desktop Application

Compile the Java files:

```bash
javac -cp ".;mysql-connector-j-8.3.0.jar" *.java
```

Run the desktop application:

```bash
java -cp ".;mysql-connector-j-8.3.0.jar" LibraryApp
```

The application opens a Java Swing window displaying book records from the MySQL database.

### Web Administration Portal

Start the web server:

```bash
java -cp ".;mysql-connector-j-8.3.0.jar" LibrarianWebServer
```

Open your browser and visit:

```text
http://localhost:8080/
```

The librarian dashboard provides access to book records, borrowings, ratings, and management operations.

## 📊 Main Components

### LibraryApp.java

A Java Swing desktop application that connects to MySQL and displays book details in a table.

### LibrarianWebServer.java

A Java HTTP server that provides a web-based librarian dashboard with book management, borrowing updates, and ratings.

### DatabaseConnection.java

Handles JDBC connectivity between the Java application and MySQL database.

### library_management.sql

Contains the database schema, relationships, constraints, and sample data.

## 🔐 Security Note

This project is developed for educational purposes.

* Keep database credentials private.
* Do not commit real passwords to GitHub.
* Use environment variables or a secure configuration for production applications.
* Additional authentication and authorization can be implemented for a production-ready system.

## 🔮 Future Enhancements

* Member login and registration.
* Librarian authentication.
* Search and filter books.
* Book availability tracking.
* Automated fine calculation.
* Book issue and return interface.
* Improved input validation.
* Responsive web design.
* Secure password and database configuration.
* Deployment to a cloud server.

## 📄 License

This project is intended for educational and academic purposes.
