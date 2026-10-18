# New Life Hospital — Indore - Ujjain Rd

Public website + hospital management SPA.

| Tier | Stack |
|---|---|
| Frontend | Angular 22 (standalone components, signals, zoneless, lazy-loaded feature routes, reactive forms, functional interceptors/guards) |
| Backend | Java 21 · Spring Boot 3.5 (virtual threads, records) · Spring Security · Spring Data JPA · Flyway |
| Database | PostgreSQL (H2 in PostgreSQL mode for the `dev` profile and tests) |

```
backend/   Spring Boot API         (src/main/resources/db/migration = schema + master data)
frontend/  Angular SPA             (src/app/core, shared, layout, features/{public,auth,staff,admin,shared-pages})
```

## Run locally

Prerequisites: JDK 21, Maven 3.9+, Node 20.19+/22+.

```bash
cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

```bash
cd frontend && npm install && npm start
```

Open http://localhost:4200. The Angular dev server proxies `/api` to `:8080` (`frontend/proxy.conf.json`), so the
browser sees a single origin and the session cookie stays `SameSite=Strict`.

The `dev` profile uses an in-memory database (reset on restart), throwaway keys, and creates demo accounts whose
credentials are in [`backend/src/main/resources/application-dev.yml`](backend/src/main/resources/application-dev.yml).
Never use that profile outside a developer machine.

### Local PostgreSQL instead of the in-memory database

1. Create the app role and an empty database (you'll be prompted for the `postgres` password):
   `psql -h localhost -p <port> -U postgres -d postgres -v app_password=<choose one> -f backend/scripts/create-local-db.sql`
2. Copy `backend/local-db.properties.example` to `backend/local-db.properties` (git-ignored) and fill in port and password.
3. Start the backend with both profiles: `mvn spring-boot:run -Dspring-boot.run.profiles=dev,local`.
   Flyway creates the tables and sample data on first start; data then survives restarts.

Tests: `mvn test` (backend: crypto unit tests + MockMvc end-to-end flows) and `npm test -- --watch=false` (frontend).

## Production

1. Create a PostgreSQL database and user; Flyway creates the schema on first start (`ddl-auto=validate` afterwards).
2. Provide every variable in [`.env.example`](.env.example) from a secret manager. The app refuses to start if a key is missing.
3. `mvn -f backend/pom.xml package` → `java -jar backend/target/hospital-backend-1.0.0.jar`
4. `npm --prefix frontend run build` → serve `frontend/dist/frontend/browser` from the same origin as `/api`
   (e.g. Nginx: static files + `location /api { proxy_pass http://backend:8080; }`, with SPA fallback to `index.html`),
   over HTTPS.
5. Sign in to the Admin portal with the bootstrap admin, change its password (Account), create staff accounts, and
   replace the placeholder doctors, prices and contact details (seeded as examples in `V2__master_data.sql`).

## Architecture notes

### Database-driven validation
`field_validations` holds the rules for each form/field (type, required, regex, min/max length, label, message).
- `GET /api/validations?forms=patient` → Angular `ValidationService` builds validators at runtime and shows the DB labels and messages.
- The server applies the **same rows** through `DynamicValidator` before every write, so bypassing the UI changes nothing.
- Rules such as `idNumber_AADHAAR` / `idNumber_PASSPORT` override the generic `idNumber` rule per ID type.
- Editing a rule in the table takes effect on the next request (server) / next app load (client).

### Sessions & authorization
- Login issues an HS256 JWT in an **HttpOnly, Secure, SameSite=Strict** cookie scoped to `/api`; JavaScript never sees it.
- CSRF: double-submit cookie (`XSRF-TOKEN` → `X-XSRF-TOKEN`), handled automatically by Angular's HttpClient.
- Each request re-checks the user row: disabling/deleting an account, changing a username or password, or signing out
  bumps `token_version` and invalidates every outstanding token immediately.
- Roles are enforced three times: URL rules in `SecurityConfig`, `@PreAuthorize` on `StaffController`/`AdminController`,
  and Angular `canMatch` guards (UX only). An account can only sign in through the portal matching its role.
- Brute-force lockout after 5 failures for 15 minutes (in-memory; use Redis or similar when running several instances), timing-equalized responses for unknown usernames.

### Cryptography
- Passwords: `BCryptPasswordEncoder(12)`; admins can reset, never read, a password.
- `EncryptionUtil` — AES-256-**GCM** with a random 96-bit IV per value and optional associated data. The original
  template used `Cipher.getInstance("AES")` (= ECB, deterministic, no integrity) and an 18-byte key that AES rejects;
  both are fixed and documented in the class. Developer CLI:
  ```bash
  java -cp backend/target/classes com.newlife.hospital.security.EncryptionUtil keygen
  ```
  (`encrypt [aad]` reads plaintext from stdin; `decrypt <ciphertext> [aad]`; the key comes from `APP_CRYPTO_KEY`.)
- Patient government ID numbers are stored encrypted with `EncryptionUtil` (associated data `patients.id_number`);
  an HMAC-SHA256 blind index (`id_number_hash`, unique) enables duplicate detection without plaintext. The API only
  ever returns the masked form `XXXX-1234`, and ID lookups use POST bodies so numbers never appear in URLs or logs.

### Business rules
- Patients: name / ID / DOB / gender are immutable after registration (no setters, `updatable=false`, and
  unknown JSON properties are rejected); only contact number and address can be edited.
- Invoices: prices come from the charges master (never from the client) and are snapshotted on each line item;
  `invoice_number` = `NLH-INV-<yyyyMMdd>-<sequence>`; paid/balance/status are server-computed, enforced by DB check
  constraints, and further payments are row-locked. Revenue reports use the hospital-local (IST) `invoice_date`.
