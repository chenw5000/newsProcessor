# newsProcessor

News processor. The server stores data in PostgreSQL database `newsprocessor`. The client is the UI.

## Layout

- `server` — Java 21 and Spring Boot API
- `client` — React and TypeScript, built with Vite

## Database

PostgreSQL must be running. Create the database once:

```sql
CREATE DATABASE newsprocessor;
```

Or, from `server`:

```powershell
psql -U postgres -h localhost -f scripts/create-database.sql
```

Copy the connection template, then edit the local file (it is gitignored):

```powershell
copy server\src\main\resources\db.properties.example server\src\main\resources\db.properties
```

Put the JDBC URL, user, and password in `db.properties`, then restart the server.

## Run

In one terminal:

```powershell
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "your-password"
mvn -f server/pom.xml spring-boot:run
```

In another:

```powershell
cd client
npm install
npm run dev
```

Open http://localhost:5173. The dev server proxies `/api` to http://localhost:6060.
