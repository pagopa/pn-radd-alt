```mermaid
sequenceDiagram
    participant OPS as Operatore OPS
    participant PN as pn-radd-alt
    participant ALS as Amazon Location Service

    OPS->>PN: POST /registry
    Note right of OPS: Payload: {addressRow, city, pr, cap, country}
    activate PN
    
    PN->>ALS: ValidateAddressRequest
    Note left of PN: {Text: "VIA SALVO D'ACQUISTO 24, TARANTO",<br>Country: "ITA"}
    activate ALS
    
    alt Address found
        ALS-->>PN: ValidateAddressResponse
        Note right of ALS: {Valid: true,<br>Latitude: 40.4762,<br>Longitude: 17.2297,<br>MatchType: "EXACT"}
    else Address not found
        ALS-->>PN: {Valid: false,<br>Error: "NO_MATCH"}
    end
    deactivate ALS
    
    alt Valid address
        PN-->>OPS: 200 OK
        Note left of PN: {coordinates: [40.4762,17.2297],<br>quality: "EXACT"}
    else Invalid address
        PN-->>OPS: 400 Bad Request
        Note left of PN: {error: "INDIRIZZO_NON_VALIDO",<br>details: "Non trovato in ALS"}
    end
    deactivate PN