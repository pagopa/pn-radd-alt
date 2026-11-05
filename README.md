# pn-radd-alt

Microservizio Spring Boot WebFlux reattivo dell'ecosistema PagoPA Piattaforma Notifiche (`pn-*`) che espone le API per il canale RADD alt (Rete di Accesso ai Documenti Digitali — canale alternativo, FSU e rete postale di terze parti).

Fa da front-end reattivo verso gli sportelli RADD e verso i sistemi BO che ne gestiscono l'anagrafica, e orchestra chiamate sincrone verso `pn-delivery`, `pn-delivery-push`, `pn-safe-storage` e `pn-data-vault`. Persiste lo stato su tabelle DynamoDB e consuma/produce eventi SQS per le elaborazioni asincrone (normalizzazione indirizzi, import massivo del registry, notifiche da Safe Storage).

Gestisce tre aree: anagrafica degli sportelli RADD (registry self, BO e import massivo v1/v2), copertura territoriale per CAP, e operazioni sportello (ACT, AOR, download documenti).

Per le versioni delle dipendenze vedi `pom.xml`.

## Architettura

```
RegistrySelfController / RegistrySelfControllerV2   ──► RegistrySelfService / RegistrySelfServiceV2
RegistryController                                  ──► RegistryService           (import massivo, upload CSV)
CoverageController / CoveragePrivateController      ──► CoverageService           (copertura CAP)
StoreRegistryController                             ──► StoreRegistryService
ActPrivateRestV1Controller                          ──► ActService                (operazioni ACT)
AorPrivateRestV1Controller                          ──► AorService                (operazioni AOR)
OperationPrivateRestV1Controller                    ──► OperationService
DocumentOperationsRestV1Controller                  ──► DocumentOperationsService

Servizi di dominio (trasversali):
  ├─ CsvService           (parsing/validazione CSV registry import, OpenCSV)
  ├─ AwsGeoService        (normalizzazione indirizzi, AWS GeoPlaces eu-central-1)
  ├─ SecretService        (AWS Secrets Manager)
  └─ RegistryImportProgressService  (job schedulato, ShedLock su pn-RaddShedLock)

Client HTTP uscenti (middleware/msclient, WebClient reattivi generati da OpenAPI):
  PnDeliveryClient        → pn-delivery
  PnDeliveryPushClient    → pn-delivery-push
  PnSafeStorageClient     → pn-safe-storage
  PnDataVaultClient       → pn-data-vault
  DocumentDownloadClient  → download presigned URL

Persistenza (middleware/db, DynamoDB Enhanced async):
  pn-RaddRegistry, pn-RaddRegistryV2, pn-RaddRegistryImport, pn-RaddRegistryRequest,
  pn-radd-transaction-alt, pn-operations-iuns-alt, pn-RaddCoverage, pn-RaddShedLock

Eventi SQS (middleware/queue, Spring Cloud Stream):
  pn-radd_alt_input        ──► RaddAltInputEventHandler
                                ├─ RADD_NORMALIZE_REQUEST  → AwsGeoService
                                └─ IMPORT_COMPLETED        → RegistryService
  pn-safestore_to_raddalt  ──► SafeStorageEventHandler     → SafeStorageEventService

Eventi uscenti EventBridge: AttachmentsConfigEvent su bus default.
```

## API e documentazione

Il servizio espone più API OpenAPI. La tabella elenca gli endpoint principali per area. Per request/response body vedi le spec in `docs/openapi/`.

| Metodo | Path | Metodo controller | Sequence diagram |
|--------|------|-------------------|------------------|
| POST | `/radd-net/api/v1/registry` | `RegistrySelfController.addRegistry` | [create-registry](docs/sequences/create-registry.md) |
| GET | `/radd-net/api/v1/registry` | `RegistrySelfController.retrieveRegistries` | — |
| PATCH | `/radd-net/api/v1/registry/{registryId}` | `RegistrySelfController.updateRegistry` | [update-registry](docs/sequences/update-registry.md) |
| PATCH | `/radd-net/api/v1/registry/{registryId}/dismiss` | `RegistrySelfController.deleteRegistry` | — |
| POST | `/radd-net/api/v2/registry` | `RegistrySelfControllerV2.addRegistry` | [create-registry](docs/sequences/create-registry.md) |
| PATCH | `/radd-net/api/v2/registry/{registryId}` | `RegistrySelfControllerV2.updateRegistry` | [update-registry](docs/sequences/update-registry.md) |
| PATCH | `/radd-net/api/v2/registry/{registryId}` (selective) | `RegistrySelfControllerV2.selectiveUpdateRegistry` | — |
| POST | `/radd-net/api/v1/registry/import/upload` | `RegistryController.uploadRegistryRequests` | — |
| POST | `/radd-net/api/v1/registry/import/{requestId}/verify` | `RegistryController.verifyRequest` | — |
| GET | `/radd-net/api/v1/registry/import/{requestId}` | `RegistryController.retrieveRequestItems` | — |
| POST/GET/PATCH | `/radd-bo/api/v1/registry/**` | `RegistryController` / `RegistrySelfController` (`*Bo`) | — |
| * | `/radd/api/v1/coverage/**` | `CoverageController`, `CoveragePrivateController` | — |
| * | `/radd-private/api/v1/**` (act/aor/operation/document) | `*PrivateRestV1Controller` | — |

Nota: gli endpoint `radd-net` sono esposti agli sportelli RADD autenticati, quelli `radd-bo` al back-office operativo. Le versioni v1 e v2 di registry-self coesistono (tabelle `pn-RaddRegistry` vs `pn-RaddRegistryV2`): per nuove feature allineare la v2 se non specificato diversamente. L'inserimento massivo dello sportello avviene tramite lo script `scripts/registryMassiveInsert/` (Node.js, vedi il relativo `README.md`), non tramite un flusso documentato in `docs/sequences/`.

## Configurazione

Le property vanno definite in `config/application.properties` (formato Spring Boot, dotted-lowercase).
Spring Boot supporta relaxed binding: per passarle come env var, converti punti e trattini in underscore e metti tutto in maiuscolo. Esempio: `pn.radd.sanitize-mode` → `PN_RADD_SANITIZEMODE`.

### Feature toggle e runtime

| Property | Tipo | Default | Esempio |
|----------|------|---------|---------|
| `pn.env.runtime` | String | `DEVELOPMENT` | `PROD` |
| `pn.radd.document-page-count-enabled` | boolean | `false` | `true` |
| `pn.radd.sanitize-mode` | enum | `ESCAPING` | `ESCAPING` |
| `server.port` | int | `8086` | `8086` |

### Client verso altri microservizi PN

| Property | Tipo | Default | Esempio |
|----------|------|---------|---------|
| `pn.radd.client_delivery_basepath` | String (URL) | `http://localhost:1080` | `https://api.<env>.pn.pagopa.it` |
| `pn.radd.client_delivery_push_basepath` | String (URL) | `http://localhost:1080` | `https://api.<env>.pn.pagopa.it` |
| `pn.radd.client_delivery_push_internal_basepath` | String (URL) | `http://localhost:1080` | `https://api.<env>.pn.pagopa.it` |
| `pn.radd.client_safe_storage_basepath` | String (URL) | `http://localhost:1080` | `https://api.<env>.pn.pagopa.it` |
| `pn.radd.client_datavault_basepath` | String (URL) | `http://localhost:1080` | `https://api.<env>.pn.pagopa.it` |
| `pn.radd.safe-storage-cx-id` | String | `pn-radd-alt` | `pn-radd-alt` |
| `pn.radd.safe-storage-doc-type` | String | `PN_RADD_ALT_ATTACHMENT` | `PN_RADD_ALT_ATTACHMENT` |
| `pn.radd.registrysafestoragedoctype` | String | `PN_RADD_REGISTRY` | `PN_RADD_REGISTRY` |
| `pn.radd.application_basepath` | String (URL) | `http://localhost:8086` | `https://api.<env>.pn.pagopa.it` |

### AWS, DynamoDB e SQS

| Property | Tipo | Default | Esempio |
|----------|------|---------|---------|
| `aws.region-code` | String | `us-east-1` | `us-east-1` |
| `aws.geo.region-code` | String | `eu-central-1` | `eu-central-1` |
| `aws.endpoint-url` | String (URL) | `http://localhost:4566` | `` (vuoto in cloud) |
| `aws.profile-name` | String | `default` | `default` |
| `spring.cloud.aws.region.static` | String | `us-east-1` | `us-east-1` |
| `pn.radd.dao.raddregistrytable` | String | `pn-RaddRegistry` | `pn-RaddRegistry` |
| `pn.radd.dao.raddregistrytablev2` | String | `pn-RaddRegistryV2` | `pn-RaddRegistryV2` |
| `pn.radd.dao.raddregistryimporttable` | String | `pn-RaddRegistryImport` | `pn-RaddRegistryImport` |
| `pn.radd.dao.raddregistryrequesttable` | String | `pn-RaddRegistryRequest` | `pn-RaddRegistryRequest` |
| `pn.radd.dao.raddtransactiontable` | String | `pn-radd-transaction-alt` | `pn-radd-transaction-alt` |
| `pn.radd.dao.iunsoperationsTable` | String | `pn-operations-iuns-alt` | `pn-operations-iuns-alt` |
| `pn.radd.dao.raddcoveragetable` | String | `pn-RaddCoverage` | `pn-RaddCoverage` |
| `pn.radd.dao.shedLockTableName` | String | `pn-RaddShedLock` | `pn-RaddShedLock` |
| `pn.radd.sqs.inputQueueName` | String | `pn-radd_alt_input` | `pn-radd_alt_input` |
| `pn.radd.sqs.safeStorageQueueName` | String | `pn-safestore_to_raddalt` | `pn-safestore_to_raddalt` |
| `pn.radd-alt.event.handler.RADD_NORMALIZE_REQUEST` | String | `pnRaddAltInputNormalizeRequestConsumer` | idem |
| `pn.radd-alt.event.handler.IMPORT_COMPLETED` | String | `pnRaddAltImportCompletedRequestConsumer` | idem |
| `pn.radd-alt.event.handler.SAFE_STORAGE_EVENTS` | String | `pnSafeStorageEventInboundConsumer` | idem |
| `pn.radd.eventbus.name` | String | `default` | `default` |
| `pn.radd.eventbus.detail-type` | String | `AttachmentsConfigEvent` | `AttachmentsConfigEvent` |
| `pn.radd.eventbus.source` | String | `pn-radd-alt` | `pn-radd-alt` |

### Registry import e scheduling

| Property | Tipo | Default | Esempio |
|----------|------|---------|---------|
| `pn.radd.registryImportReplacedTtl` | long (h) | `8766` | `8766` |
| `pn.radd.registrydefaultendvalidity` | int | `0` | `0` |
| `pn.radd.registryDefaultEndValidity` | int (anni) | `3` | `3` |
| `pn.radd.registrydefaultdeleterule` | enum | `DUPLICATE` | `DUPLICATE` |
| `pn.radd.registryImportProgress.delay` | long (ms) | `30000` | `30000` |
| `pn.radd.registryImportProgress.lock-at-most` | long (ms) | `120000` | `120000` |
| `pn.radd.registryImportProgress.lock-at-least` | long (ms) | `1000` | `1000` |
| `pn.radd.attempt-batch-writer` | int | `3` | `3` |
| `pn.radd.maxQuerySize` | int | `10` | `10` |
| `pn.radd.maxPageNumber` | int | `10` | `10` |
| `pn.radd.evaluated.zip.code.config.type` | enum | `ZIPCODE` | `ZIPCODE` |
| `pn.radd.evaluated.zip.code.config.number` | int | `1` | `1` |

### Altro

| Property | Tipo | Default | Esempio |
|----------|------|---------|---------|
| `cors.allowed.domains` | String (CSV) | `http://localhost:8090,http://localhost:8091` | `https://portal.<env>.pn.pagopa.it` |
| `spring.freemarker.template-loader-path` | String | `documents_composition_templates` | idem |
| `spring.freemarker.suffix` | String | `.html` | `.html` |
| `logging.config` | String | `config/logback-local.xml` | `/config/logback.xml` |

## Esecuzione

### Prerequisiti

Java 21, Maven wrapper (`./mvnw`) e, per il runtime locale, LocalStack su `http://localhost:4566` (DynamoDB, SQS, Secrets Manager, EventBridge) più un mock HTTP per gli upstream PN su `http://localhost:1080`.

```bash
# LocalStack e mock upstream (esempio con docker)
docker run --rm -d -p 4566:4566 localstack/localstack
docker run --rm -d -p 1080:1080 mockserver/mockserver
```

### Compilazione

```bash
./mvnw clean install
```

Per la build equivalente alla CI:

```bash
./mvnw clean install && ./mvnw -DCI_PROFILE clean install
```

Per rigenerare solo i client OpenAPI via container pagopa-codegen:

```bash
./scripts/generate-code.sh
```

### Avvio locale

```bash
./mvnw spring-boot:run
```

Override delle property locali: `config/application.properties`.

L'applicazione sarà disponibile su `http://localhost:8086`.
Output atteso all'avvio: `Started RaddFsuApplication in ... seconds`.

### Test

```bash
./mvnw test
```

Test unitari con JUnit 5 + Mockito + Reactor `StepVerifier`. I test delle DAO DynamoDB usano Testcontainers (vedi `src/test/resources/testcontainers/`). I test degli msclient usano MockServer su `mockserver.bean.port=1050` con le expectation in `src/test/resources/*Test-webhook.json`. Per un singolo test: `./mvnw test -Dtest=RegistryServiceTest#myMethod`.
