# Creazione di un Punto di Ritiro 
In questo scenario, vogliamo creare un punto di ritiro 

Ogni punto di ritiro avrà una proprietà aggiuntiva `operationCodes`, che rappresenta una lista di codici che il partner può utilizzare per identificare le operazioni compiute presso quello specifico punto di ritiro. Per ogni `operationCode` fornito in input, il sistema genera un corrispondente `operationId` che verrà restituito nella risposta e potrà essere utilizzato nelle operazioni future.

## API Utilizzate
- createPartnerOffice
- GET /partners/{partnerId}/partner-offices/operation-ids/{operationId}

## Sequence Diagram 

```mermaid
sequenceDiagram
    actor Client
    participant Service as PartnerOfficeService
    participant Geocoder as Geocoding Service
    participant DB as DynamoDB
    participant Audit as AuditLog

    Client->>Service: POST /partner-offices (payload con operationCodes)
    activate Service

    Service->>Service: validate input
    alt invalid input
        Service-->>Client: 400 Bad Request
        deactivate Service
    else valid input
        loop Retry geocoding (max 3)
            Service->>Geocoder: geocode(address)
            alt success
                Geocoder-->>Service: lat, lon, confidence
            else failure
                Geocoder-->>Service: error
            end
        end

        Service->>Service: generate ID from lat/lon (hash)
        Service->>Service: genera operationId per ogni operationCode

        par Inserimento atomico
            Service->>DB: putItem(officeId, full data, operationCodes, operationIds)
            Service->>DB: putItem(operationId, officeId) per ogni operationId
        end
        alt entrambi gli inserimenti vanno a buon fine
            DB-->>Service: OK
            Service->>Audit: log(success, officeId, partnerId, operationCodes, operationIds)
            Service-->>Client: 201 Created (officeId, operationIds)
        else almeno un inserimento fallisce
            DB-->>Service: ConditionalCheckFailed
            Service->>Audit: log(conflict, lat/lon, operationIds)
            Service-->>Client: 409 Conflict
        end
    end

```