# pn-radd-alt — Copilot instructions

RADD alt microservice for PagoPA's Piattaforma Notifiche (PN). Spring Boot 3 / WebFlux (reactive) service on Java 21, part of the `pn-*` ecosystem. Parent POM is `it.pagopa.pn:pn-parent` and shared libs `pn-commons` / `pn-model` are used extensively.

## Build, test, lint

Use the Maven wrapper — do not invoke a system `mvn`.

- Full build (runs code generation, compile, tests, jacoco): `./mvnw clean install`
- CI-equivalent build as documented in the README: `./mvnw clean install && ./mvnw -DCI_PROFILE clean install`
- Compile only (skip tests): `./mvnw clean compile -DskipTests`
- Run all tests: `./mvnw test`
- Run a single test class: `./mvnw test -Dtest=RegistryServiceTest`
- Run a single test method: `./mvnw test -Dtest=RegistryServiceTest#myMethod`
- Run the app locally: `./mvnw spring-boot:run` (reads `config/application.properties`; `pn.env.runtime=DEVELOPMENT`, expects LocalStack at `http://localhost:4566` and a mock upstream at `http://localhost:1080`).
- Regenerate OpenAPI/WS clients via the pagopa codegen container: `./scripts/generate-code.sh` (uses `pagopa.codegen.version` from `pom.xml`).

Coverage thresholds are enforced by Jacoco (`jacoco.min.line.cover.ratio` in `pom.xml`); generated sources under `it/pagopa/pn/radd/microservice/msclient/generated/**` and `it/pagopa/pn/radd/rest/radd/v1/**` are excluded. SonarQube is configured via `sonar-project.properties`.

Tests use JUnit 5 + Mockito + Reactor `StepVerifier`. DynamoDB-backed DAO tests use Testcontainers (see `src/test/resources/testcontainers/`). Tests that need an external HTTP stub use MockServer on `mockserver.bean.port=1050` (see `application-test.properties`). `logback-test.xml` is used in tests; `config/logback-local.xml` for local runs.

## Architecture — big picture

Reactive Spring Boot service that exposes several OpenAPI-described HTTP APIs and consumes/produces AWS SQS events. It fronts DynamoDB tables and integrates with sibling PN microservices (`pn-delivery`, `pn-delivery-push`, `pn-safe-storage`, `pn-data-vault`).

Top-level Java package: `it.pagopa.pn.radd` under `src/main/java/`.

Layering (request flow, top → bottom):

1. `rest/radd/fsu/**` — thin `@RestController`s that **implement generated `*Api` interfaces**. They only adapt HTTP ↔ service, they do not contain business logic. Controllers are grouped by OpenAPI spec: `RegistryController` (import), `RegistrySelfController` / `RegistrySelfControllerV2` (registry v1/v2), `CoverageController` / `CoveragePrivateController`, `StoreRegistryController`, plus `*PrivateRestV1Controller` for the internal `pn-radd-alt-private.yaml` API.
2. `services/radd/fsu/v1/**` — business logic (`RegistryService`, `RegistrySelfService(V2)`, `CoverageService`, `OperationService`, `ActService`, `AorService`, `DocumentOperationsService`, `StoreRegistryService`, `SafeStorageEventService`, `CsvService`, `AwsGeoService`, `SecretService`, …). Many extend `BaseService`. Sub-packages `dto/` and `validation/` hold service-level DTOs and validators (do not confuse with generated OpenAPI DTOs).
3. `middleware/msclient/**` — outbound HTTP clients to other PN services: `PnDeliveryClient`, `PnDeliveryPushClient`, `PnSafeStorageClient`, `PnDataVaultClient`, `DocumentDownloadClient`. They wrap generated OpenAPI clients; `common/` and `config/` hold shared WebClient config.
4. `middleware/db/**` — DynamoDB DAOs. `entities/` contains `@DynamoDbBean` POJOs (`RaddRegistryEntity`, `RaddRegistryEntityV2`, `RaddRegistryRequestEntity`, `RaddRegistryImportEntity`, `OperationsIunsEntity`, `CoverageEntity`, `NormalizedAddressEntity(V2)`, `AddressEntity`, `BiasPointEntity`). `impl/` contains the DAO implementations using the AWS SDK v2 DynamoDB Enhanced async client.
5. `middleware/queue/**` — SQS integration.
   - `producer/` sends messages.
   - `consumer/handler/` has the two Spring Cloud Stream consumers: `RaddAltInputEventHandler` (internal RADD events) and `SafeStorageEventHandler` (notifications from pn-safe-storage).
   - `event/` holds event payload POJOs.
6. `middleware/eventbus/**` — EventBridge integration for outbound events (`impl/`).
7. `mapper/` — MapStruct-style hand-written mappers between generated OpenAPI DTOs, DB entities, and service DTOs. Every cross-layer conversion goes through here; do not map inline inside services or controllers.
8. `config/`, `springbootcfg/` — Spring configuration beans (AWS clients, WebClient, scheduling, ShedLock, CORS, properties binding).
9. `exception/` — custom exceptions + `@ControllerAdvice` error mapping. The shared `pn-errors.yaml` (under `docs/openapi/`) defines error codes.
10. `utils/`, `pojo/` — cross-cutting helpers; `utils/log/` contains logging helpers used throughout the codebase — prefer them over adding ad-hoc logging.

Entry point: `it.pagopa.pn.radd.RaddFsuApplication` (a single `@SpringBootApplication` plus a trivial root `GET /` controller).

### OpenAPI-first code generation (very important)

APIs and msclients are **code-generated at build time** by `openapi-generator-maven-plugin` (`pom.xml`, `generate-resources` phase). Sources are written into `target/generated-sources/**` and compiled together with the rest. Do not hand-edit generated code and do not commit it.

Server-side specs (live in `docs/openapi/`) generate `*Api` interfaces that controllers implement:

- `pn-radd-alt-registry-internal.yaml` → `it.pagopa.pn.radd.alt.generated.openapi.server.v1`
- `pn-radd-alt-coverage-internal.yaml` → `...server.coverage.v1`
- `pn-radd-alt-store-registry-private.yaml` → store-registry server package
- `pn-radd-alt-internal.yaml` → legacy `rest/radd/v1` server package (jacoco-excluded)
- `pn-radd-alt-registry-v2-internal.yaml` → v2 registry server package
- `pn-radd-alt-import-registry-internal.yaml` → import-registry server package
- `pn-radd-alt-private.yaml` → private API server package

Client-side specs (pulled by URL in `pom.xml`, pinned to specific commits) generate WebClient-based reactive clients with `modelNameSuffix=Dto`:

- pn-delivery → `...msclient.pndelivery.v1`
- pn-delivery-push → `...msclient.pndeliverypush.v1`
- pn-ss (safe-storage) → `...msclient.pnsafestorage.v1`
- pn-data-vault → `...msclient.pndatavault.v1`

When an upstream API changes: bump the pinned commit SHA in the `<inputSpec>` URL inside `pom.xml`, run `./mvnw clean compile`, then update the msclient wrapper in `middleware/msclient/` and any mapper that references the changed DTOs.

When a local API changes: edit the YAML under `docs/openapi/`, rebuild, then update the controller / service / mapper. Sequence diagrams and example Postman collections live under `docs/sequences/` and `docs/postman/`.

## Conventions specific to this codebase

- **Reactive everywhere.** Public APIs return `Mono`/`Flux`. Do not introduce blocking calls; use the existing reactive DynamoDB and WebClient abstractions. In tests, assert with `StepVerifier`.
- **Lombok is pervasive.** `@RequiredArgsConstructor` + `private final` fields is the standard DI pattern in controllers/services (no `@Autowired`). Config in `lombok.config`.
- **Generated DTOs vs internal DTOs.** DTOs produced by the OpenAPI generator have the `Dto` suffix (`modelNameSuffix=Dto`) for msclients; server-side DTOs live under the `...generated.openapi.server.*.dto` packages. Hand-written service DTOs live under `services/radd/fsu/v1/dto/`. Keep generated types out of controllers' public signatures only when the spec demands it; otherwise controllers pass the generated server DTOs straight to services and rely on `mapper/` to cross layers.
- **Property naming.** All app-specific properties are namespaced under `pn.radd.*`. DynamoDB table names are injected via `pn.radd.dao.*`. See `config/application.properties` and `src/test/resources/application-test.properties` for the full set, including feature flags such as `pn.radd.document-page-count-enabled` and `pn.radd.sanitize-mode`.
- **Scheduled jobs use ShedLock** (`net.javacrumbs.shedlock`) with the `pn-RaddShedLock` table — do not add bare `@Scheduled` without the lock.
- **CSV handling** goes through `CsvService` (OpenCSV). Registry import test fixtures live in `src/test/resources/radd-registry*.csv`.
- **PDF / HTML rendering** uses openhtmltopdf + pdfbox + OWASP HTML sanitizer + FreeMarker templates under `documents_composition_templates/` (template path configured via `spring.freemarker.template-loader-path`).
- **AWS.** Region is `us-east-1` (DynamoDB/SQS), `aws.geo.region-code=eu-central-1` for GeoPlaces. Locally everything points at LocalStack (`aws.endpoint-url=http://localhost:4566`). Secrets are loaded via `SecretService` from AWS Secrets Manager.
- **Mockserver webhooks.** `src/test/resources/*Test-webhook.json` files are expectation files loaded into MockServer by client tests — update them when changing those clients.
- **Versioned APIs coexist.** v1 and v2 of registry-self live side by side (`RegistrySelfService` / `RegistrySelfServiceV2`, `RaddRegistryEntity` / `RaddRegistryEntityV2`, tables `pn-RaddRegistry` / `pn-RaddRegistryV2`). When adding features, mirror the v2 path unless explicitly targeting legacy v1.
- **Error codes** are centralized in `docs/openapi/pn-errors.yaml`; add new codes there and surface them via the existing exception hierarchy in `exception/`.

## Useful entry points when extending

- New inbound HTTP endpoint → edit the relevant `docs/openapi/*.yaml`, rebuild, implement the new method on the matching controller in `rest/radd/fsu/`, delegate to (or create) a service in `services/radd/fsu/v1/`, add a mapper in `mapper/` if crossing layers, write a `StepVerifier`-based test.
- New outbound call → add a method on the matching `middleware/msclient/Pn*Client.java`, bumping the pinned spec SHA in `pom.xml` if the upstream contract changed.
- New SQS event → add payload to `middleware/queue/event/`, wire handler in `middleware/queue/consumer/handler/` (or producer in `middleware/queue/producer/`), and bind it via Spring Cloud Stream in `config/`.
- New DynamoDB access → add/extend an entity in `middleware/db/entities/` and a DAO under `middleware/db/impl/`; register the table name as a `pn.radd.dao.*` property.
