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

The API creates the tables on startup. Edit `server/src/main/resources/db.properties` to change the JDBC URL, user, or password, then restart the server.

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
