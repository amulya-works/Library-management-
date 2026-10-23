const pageNames = {
    dashboard: "Library Management Dashboard",
    books: "Book Catalog",
    "add-book": "Add New Book",
    "update-book": "Update Book",
    borrowings: "Borrowing Tracker",
    reviews: "Ratings & Reviews"
};

function showPage(pageId) {
    if (!pageNames[pageId]) return;

    document.querySelectorAll(".page").forEach(page => {
        page.classList.toggle("active", page.id === pageId);
    });

    document.querySelectorAll(".nav-item").forEach(button => {
        button.classList.toggle("active", button.dataset.page === pageId);
    });

    document.getElementById("page-title").textContent = pageNames[pageId];

    if (pageId === "books") {
    loadBooks();
}

if (pageId === "borrowings") {
    loadBorrowings();
}
if (pageId === "reviews") {
    loadRatings();
}
}

document.querySelectorAll("[data-page]").forEach(button => {
    button.addEventListener("click", () => showPage(button.dataset.page));
});

async function loadDashboardStats() {
    try {
        const response = await fetch("/api/stats");
        if (!response.ok) throw new Error("Could not load statistics.");

        const stats = await response.json();

        document.getElementById("total-books").textContent = stats.books;
        document.getElementById("total-members").textContent = stats.members;
        document.getElementById("total-ratings").textContent = stats.ratings;
    } catch (error) {
        console.error("Dashboard error:", error);
    }
}

async function loadBooks() {
    const search = document.getElementById("book-search").value.trim();
    const table = document.getElementById("books-table");

    table.innerHTML = '<tr><td colspan="5" class="empty">Loading books...</td></tr>';

    try {
        const response = await fetch("/api/books?search=" + encodeURIComponent(search));
        const books = await response.json();

        if (!response.ok) {
            throw new Error(books.error || "Could not load books.");
        }

        if (books.length === 0) {
            table.innerHTML = '<tr><td colspan="5" class="empty">No books found.</td></tr>';
            return;
        }

        table.innerHTML = "";

        books.forEach(book => {
            const row = document.createElement("tr");

            [book.id, book.title, book.isbn, book.publisherId].forEach(value => {
                const cell = document.createElement("td");
                cell.textContent = value ?? "";
                row.appendChild(cell);
            });

            const actionCell = document.createElement("td");

const updateButton = document.createElement("button");
updateButton.textContent = "Update";
updateButton.className = "secondary-button";

updateButton.addEventListener("click", () => {
    showPage("update-book");
    document.getElementById("update-book-id").value = book.id;
    document.getElementById("update-book-title").value = book.title;
    document.getElementById("update-book-message").textContent = "";
});

const deleteButton = document.createElement("button");
deleteButton.textContent = "Delete";
deleteButton.className = "delete-button";

deleteButton.addEventListener("click", async () => {
    const confirmed = confirm(
        `Are you sure you want to delete "${book.title}" (Book ID: ${book.id})? This action cannot be undone.`
    );

    if (!confirmed) {
        return;
    }

    deleteButton.disabled = true;
    deleteButton.textContent = "Deleting...";

    try {
        const response = await fetch("/api/books/delete", {
            method: "POST",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded"
            },
            body: new URLSearchParams({ bookId: book.id })
        });

        const result = await response.json();

        if (!response.ok) {
            throw new Error(result.error || "Could not delete book.");
        }

        await loadBooks();
        await loadDashboardStats();
        alert(result.message);
    } catch (error) {
        alert(error.message);
        deleteButton.disabled = false;
        deleteButton.textContent = "Delete";
    }
});

actionCell.appendChild(updateButton);
actionCell.appendChild(deleteButton);
row.appendChild(actionCell);
table.appendChild(row);
        });
    } catch (error) {
        table.innerHTML = '<tr><td colspan="5" class="empty">Could not load books. Check the server terminal.</td></tr>';
        console.error("Book catalog error:", error);
    }
}

document.getElementById("search-books").addEventListener("click", loadBooks);

document.getElementById("book-search").addEventListener("keydown", event => {
    if (event.key === "Enter") {
        event.preventDefault();
        loadBooks();
    }
});

document.getElementById("update-book-form").addEventListener("submit", async event => {
    event.preventDefault();

    const bookId = document.getElementById("update-book-id").value.trim();
    const newTitle = document.getElementById("update-book-title").value.trim();
    const message = document.getElementById("update-book-message");

    message.textContent = "Updating book title...";

    try {
        const response = await fetch("/api/books/update", {
            method: "POST",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded"
            },
            body: new URLSearchParams({ bookId, newTitle })
        });

        const result = await response.json();
        message.textContent = result.message || result.error || "Request completed.";

        if (response.ok) {
            document.getElementById("update-book-form").reset();
            loadDashboardStats();
        }
    } catch (error) {
        message.textContent = "Unable to connect to the server.";
        console.error("Update error:", error);
    }
});

document.getElementById("add-book-form").addEventListener("submit", async event => {
    event.preventDefault();

    const bookId = document.getElementById("book-id").value.trim();
    const title = document.getElementById("book-title").value.trim();
    const isbn = document.getElementById("book-isbn").value.trim();
    const publisherId = document.getElementById("publisher-id").value.trim();
    const message = document.getElementById("book-form-message");

    message.textContent = "Adding book...";

    try {
        const response = await fetch("/api/books/add", {
            method: "POST",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded"
            },
            body: new URLSearchParams({
                bookId,
                title,
                isbn,
                publisherId
            })
        });

        const result = await response.json();
        message.textContent = result.message || result.error || "Request completed.";

        if (response.ok) {
            document.getElementById("add-book-form").reset();
            loadDashboardStats();
        }
    } catch (error) {
        message.textContent = "Unable to connect to the server.";
        console.error("Add book error:", error);
    }
});

document.getElementById("review-form").addEventListener("submit", event => {
    event.preventDefault();
    document.getElementById("review-form-message").textContent =
        "Review submission will be connected next.";
});

loadDashboardStats();
async function loadBorrowings() {
const table = document.getElementById("borrowings-table");


table.innerHTML = '<tr><td colspan="10" class="empty">Loading borrowing records...</td></tr>';

try {
    const response = await fetch("/api/borrowings");
    const borrowings = await response.json();

    if (!response.ok) {
        throw new Error(borrowings.error || "Could not load borrowing records.");
    }

    if (borrowings.length === 0) {
        table.innerHTML = '<tr><td colspan="10" class="empty">No borrowing records found.</td></tr>';
        return;
    }

    table.innerHTML = "";

    borrowings.forEach(record => {
        const row = document.createElement("tr");

        const values = [
            record.borrowingId,
            record.memberName,
            record.bookId,
            record.bookTitle,
            record.librarianId,
            record.issueDate,
            record.dueDate,
            record.returnDate,
            record.status,
            record.fineAmount
        ];

        values.forEach(value => {
            const cell = document.createElement("td");
            cell.textContent = value ?? "—";
            row.appendChild(cell);
        });

        table.appendChild(row);
    });
} catch (error) {
    table.innerHTML = '<tr><td colspan="10" class="empty">Could not load borrowing records. Check the server terminal.</td></tr>';
    console.error("Borrowing error:", error);
}


}

document.getElementById("refresh-borrowings").addEventListener("click", loadBorrowings);
async function loadRatings() {
const table = document.getElementById("ratings-table");


table.innerHTML = '<tr><td colspan="7" class="empty">Loading ratings...</td></tr>';

try {
    const response = await fetch("/api/ratings");
    const ratings = await response.json();

    if (!response.ok) {
        throw new Error(ratings.error || "Could not load ratings.");
    }

    if (ratings.length === 0) {
        table.innerHTML = '<tr><td colspan="7" class="empty">No ratings found.</td></tr>';
        return;
    }

    table.innerHTML = "";

    ratings.forEach(record => {
        const row = document.createElement("tr");

        const values = [
            record.ratingId,
            record.memberId,
            record.memberName,
            record.librarianId,
            record.rating ? "★".repeat(Number(record.rating)) : "—",
            record.review,
            record.ratingDate
        ];

        values.forEach(value => {
            const cell = document.createElement("td");
            cell.textContent = value ?? "—";
            row.appendChild(cell);
        });

        table.appendChild(row);
    });
} catch (error) {
    table.innerHTML = '<tr><td colspan="7" class="empty">Could not load ratings. Check the server terminal.</td></tr>';
    console.error("Ratings error:", error);
}


}

document.getElementById("refresh-ratings").addEventListener("click", loadRatings);
