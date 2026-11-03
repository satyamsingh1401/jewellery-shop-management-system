# Gold Rate Management Module

| | |
|---|---|
| **Module** | 2 – Gold Rate Management |
| **Owner** | Ayush Kumar |
| **Branch** | `feature/gold-rate-management` |
| **Package** | `com.myhourly.jewellery.metalrate` |
| **Table** | `metal_rates` (created by Hibernate from the `MetalRate` entity) |

## 1. What this module does

The module stores gold, silver and platinum rates per gram for each purity level. It keeps the full rate history (who recorded each rate and when it takes effect) and gives every other module one place to look up the rate that applies at a given moment.

- Maintain rates for each purity (`K18`, `K22`, `K24`, `SILVER_925`, `PLATINUM_950`).
- Record **when** a rate becomes effective, **who** recorded it, and **when** it was recorded.
- Return the current rate, or the rate that applied at any past moment (needed for invoices and order snapshots).
- Calculate **metal value = net weight × applicable rate per gram**, as defined in section 7 of the requirements document.
- Schedule a rate for a future time and cancel it before it takes effect.

## 2. Package layout

```
metalrate/
├── controller/  MetalRateController                REST endpoints under /api/metal-rates
├── dto/         MetalRateRequest, MetalRateResponse,
│                MetalValueRequest, MetalValueResponse,
│                MetalRatePageResponse
├── entity/      MetalRate                          JPA entity -> metal_rates table
├── enums/       MetalType, Purity                  Purity knows which metal it belongs to
├── exception/   MetalRateRequestExceptionHandler   ApiError for bad query parameters (this controller only)
├── mapper/      MetalRateMapper
├── repository/  MetalRateRepository
└── service/     MetalRateService                   Business rules + public API for other modules
```

Errors use the shared `common.exception` classes and the `common.response.ApiError` shape that is already on `main`.

## 3. Data model – `metal_rates`

| Column | Type | Notes |
|---|---|---|
| `id` | BIGINT, PK | Auto-generated |
| `metal_type` | VARCHAR(20) | `GOLD`, `SILVER`, `PLATINUM` |
| `purity` | VARCHAR(20) | `K18`, `K22`, `K24`, `SILVER_925`, `PLATINUM_950` |
| `rate_per_gram` | DECIMAL(12,2) | Must be > 0 |
| `currency` | VARCHAR(3) | Always `INR` for now |
| `effective_from` | DATETIME | When the rate starts to apply |
| `remarks` | VARCHAR(255) | Optional |
| `created_by` | VARCHAR(100) | Username from the JWT/security context, or `system` |
| `created_at` | DATETIME | Server time when the rate was recorded |

- Unique constraint `uk_metal_rate_metal_purity_effective` on (`metal_type`, `purity`, `effective_from`).
- Index `idx_metal_rate_lookup` on the same columns, for fast "latest rate at time X" lookups.

## 4. Business rules

1. **Purity must match the metal.** `K18/K22/K24` are valid only for `GOLD`, `SILVER_925` only for `SILVER` and `PLATINUM_950` only for `PLATINUM`. Anything else returns 400.
2. **Rates are never edited.** To correct a rate that is already in effect, record a new rate. This keeps the audit history intact, and invoices that used an older rate can still be explained.
3. **No back-dating.** `effectiveFrom` may be omitted (it then defaults to *now*) or set to a future time. A time more than 5 minutes in the past is rejected, so rates that already appeared on bills can't be changed retroactively. The 5-minute allowance covers clock differences between client and server.
4. **One rate per metal/purity per instant.** A duplicate `effectiveFrom` for the same metal and purity returns 409.
5. **Applicable rate** = the entry with the latest `effectiveFrom` that is ≤ the requested time. Rates scheduled for the future are ignored until they take effect.
6. **Only future rates can be deleted.** Deleting a rate that is already in effect returns 400.
7. **Metal value** = `netWeight × ratePerGram`, rounded to 2 decimals using `HALF_UP`. All values are `BigDecimal`. Making charges, discounts and tax are handled by Billing.
8. **Seconds precision.** `effectiveFrom` is truncated to whole seconds.

## 5. REST API

Base path: `/api/metal-rates`

| Method | Endpoint | Role | Purpose |
|---|---|---|---|
| POST | `/api/metal-rates` | ADMIN | Record a new rate (now or scheduled) |
| GET | `/api/metal-rates/current?metalType=GOLD&purity=K22` | Authenticated | Current rate for one metal/purity |
| GET | `/api/metal-rates/current/all` | Authenticated | Current rate for every purity that has one |
| GET | `/api/metal-rates/applicable?metalType=GOLD&purity=K22&at=2026-11-04T10:00:00` | Authenticated | Rate that applied at a given time |
| GET | `/api/metal-rates/history?metalType=&purity=&from=&to=&page=0&size=20` | Authenticated | Paged history, newest first (all filters optional, max size 100) |
| GET | `/api/metal-rates/{id}` | Authenticated | One rate entry |
| POST | `/api/metal-rates/metal-value` | Authenticated | Metal value for a given weight |
| DELETE | `/api/metal-rates/{id}` | ADMIN | Cancel a rate scheduled for the future |

Role checks use `@PreAuthorize("hasRole('ADMIN')")`. These take effect once the Authentication module (which has `@EnableMethodSecurity`) is merged; no changes to this module are needed then.

### Examples

**Create a rate**

```http
POST /api/metal-rates
Authorization: Bearer <admin-jwt>
Content-Type: application/json

{
  "metalType": "GOLD",
  "purity": "K22",
  "ratePerGram": 7000.00,
  "effectiveFrom": "2026-11-05T09:00:00",
  "remarks": "Opening rate"
}
```

`201 Created`, `Location: /api/metal-rates/1`

```json
{
  "id": 1,
  "metalType": "GOLD",
  "purity": "K22",
  "ratePerGram": 7000.00,
  "currency": "INR",
  "effectiveFrom": "2026-11-05T09:00:00",
  "remarks": "Opening rate",
  "createdBy": "admin@shop.com",
  "createdAt": "2026-11-04T18:30:00"
}
```

**Calculate metal value**

```http
POST /api/metal-rates/metal-value
Content-Type: application/json

{ "metalType": "GOLD", "purity": "K22", "netWeight": 10.000 }
```

```json
{
  "metalType": "GOLD",
  "purity": "K22",
  "netWeight": 10.000,
  "rateId": 1,
  "ratePerGram": 7000.00,
  "currency": "INR",
  "rateEffectiveFrom": "2026-11-05T09:00:00",
  "metalValue": 70000.00
}
```

Add `"at": "2026-11-05T12:00:00"` to price against the rate that applied at that time.

**Error shape** (shared `ApiError`)

```json
{
  "timestamp": "2026-11-04T18:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Request validation failed",
  "path": "/api/metal-rates",
  "fieldErrors": { "ratePerGram": "Rate per gram must be greater than zero" }
}
```

| Status | When |
|---|---|
| 400 | Validation failure, purity/metal mismatch, back-dated rate, bad query parameter, deleting an active rate |
| 401 / 403 | Not logged in / not an ADMIN (once the Authentication module is merged) |
| 404 | Rate id not found, or no rate effective at the requested time |
| 409 | Duplicate rate for the same metal, purity and `effectiveFrom` |

## 6. Using this module from other modules (Billing, Order, Product)

Inject `MetalRateService` and call it directly; no HTTP call is needed inside the monolith.

```java
MetalRateResponse rate = metalRateService.getApplicableRate(MetalType.GOLD, Purity.K22, orderTime);

MetalValueResponse value = metalRateService.calculateMetalValue(
        MetalType.GOLD, Purity.K22, netWeight, orderTime);
```

- **Order / Billing:** store `rate.id()` and `rate.ratePerGram()` on the order item or invoice as a pricing snapshot. Later rate changes then don't affect history.
- **Product:** the Product module has its own `MetalType` / `Purity` enums with the **same constant names**. Convert by name:
  ```java
  Purity purity = Purity.valueOf(product.getPurity().name());
  MetalType metal = MetalType.valueOf(product.getMetalType().name());
  ```
  Once Product is merged to `main`, the team can agree on a single shared enum (for example under `common/`) in a follow-up PR.

## 7. Running and testing

Requirements: JDK 21, MySQL (only to run the application, not for these tests).

```bash
# Unit + MockMvc tests for this module only (no database needed)
./mvnw test -Dtest="com.myhourly.jewellery.metalrate.**.*Test"

# Windows PowerShell
.\mvnw.cmd test "-Dtest=com.myhourly.jewellery.metalrate.**.*Test"
```

Coverage:

| Test class | What it checks |
|---|---|
| `MetalRateServiceTest` | Default effective time, audit user, purity rules, back-dating, duplicates, current/applicable lookups, metal value rounding, history paging, deleting scheduled vs active rates |
| `MetalRateControllerTest` | Status codes, `Location` header, validation `fieldErrors`, invalid enums and missing parameters, 404/409 mapping |
| `PurityTest` | Purity ↔ metal mapping, constant names match the Product module |

> `JewelleryShopManagementSystemApplicationTests.contextLoads` on `main` needs a configured MySQL datasource, so running `./mvnw test` without filters fails until the database properties are set up. This is unrelated to this module.

To run the app locally, set the datasource in an **untracked** local config or as environment variables (do not commit credentials), then run `./mvnw spring-boot:run`. Hibernate creates the `metal_rates` table when `spring.jpa.hibernate.ddl-auto=update` is configured.

## 8. Why this branch merges cleanly

This branch **only adds new files**:

- `src/main/java/com/myhourly/jewellery/metalrate/**`
- `src/test/java/com/myhourly/jewellery/metalrate/**`
- `docs/GOLD_RATE_MANAGEMENT.md`

It does **not** modify `pom.xml`, `application.properties`, the root `README.md`, any `SecurityConfig`, the shared `common` package or the existing test class. Those are the files other branches are changing. It also doesn't import any class from another unmerged module, so it compiles whatever order the PRs are merged in.

Keep it that way: if a later change really needs a shared file (for example a new dependency in `pom.xml`), make it a separate small PR, or coordinate with the team first.

## 9. Git workflow: committing on this branch and opening the PR

### 9.1 Daily work on the branch

```bash
git switch feature/gold-rate-management
git status
git add src/main/java/com/myhourly/jewellery/metalrate src/test/java/com/myhourly/jewellery/metalrate docs/GOLD_RATE_MANAGEMENT.md
git commit -m "Add metal rate history filter"
```

- Stage files by path. Avoid `git add .` so local files such as the project PDFs, IDE folders or local configs aren't committed.
- Keep commit subjects short and in the imperative ("Add …", "Fix …").

### 9.2 Stay in sync with `main` before pushing

```bash
git fetch origin
git merge origin/main
./mvnw test -Dtest="com.myhourly.jewellery.metalrate.**.*Test"
```

If `git merge` reports conflicts: open each conflicted file, keep both sides where appropriate, then `git add <file>` and `git commit`. Because this module only adds its own files, conflicts should be rare.

### 9.3 Push the branch

```bash
git push -u origin feature/gold-rate-management
```

`-u` is only needed the first time; after that, `git push` is enough.

### 9.4 Open the pull request

**GitHub website:** open the repository, click **Compare & pull request** for `feature/gold-rate-management`, set **base: `main`**, fill in the description and create the PR.

**GitHub CLI:**

```bash
gh pr create --base main --head feature/gold-rate-management \
  --title "Gold rate management module" \
  --body "Adds metal rate recording with audit history, current/applicable rate lookup and metal value calculation. See docs/GOLD_RATE_MANAGEMENT.md."
```

Suggested PR checklist:

- [ ] Branch is up to date with `origin/main`
- [ ] Module tests pass locally
- [ ] Only `metalrate/**` and `docs/GOLD_RATE_MANAGEMENT.md` are changed (`git diff --stat origin/main...HEAD`)
- [ ] No credentials or local config committed
- [ ] Reviewer assigned; QA (Shubhankari Dash) informed that the endpoints are ready to test

### 9.5 After review comments

Make the changes on the same branch, commit, and `git push`. The open PR updates automatically. Don't force-push once reviewers have started commenting.

### 9.6 After the PR is merged

```bash
git switch main
git pull origin main
git branch -d feature/gold-rate-management
```

## 10. Open items to confirm with the team

- Single shared `MetalType` / `Purity` enum once the Product module is on `main`.
- Whether Sales Staff should also be allowed to record rates (currently ADMIN only).
- Whether purity conversion (for example pricing 22K from a 24K base rate) is needed, or whether a separate rate is always recorded per purity (current behaviour).
- Multi-currency support (currently fixed to INR).
