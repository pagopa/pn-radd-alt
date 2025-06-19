```mermaid
sequenceDiagram
    participant OPS as Operatore OPS
    participant PN as pn-radd-alt

    OPS->>PN: PATCH /registry/{locationId}
    Note right of OPS: Payload: <br>{"operatingHours": "9-18",<br>"contactPhone": "+39 123456789"}
    activate PN

    alt Campi validi
        PN->>PN: Validazione regole business
        Note left of PN: Verifica:<br>- Formato orario<br>- Numero telefonico valido
        PN->>PN: Aggiorna dati modificabili
        PN-->>OPS: 200 OK
        Note left of PN: {<br>"status": "updated",<br>"updatedFields": ["operatingHours","contactPhone"]<br>}
    else Campi non validi
        PN-->>OPS: 400 Bad Request
        Note left of PN: {<br>"error": "VALIDATION_ERROR",<br>"details": ["Formato orario non valido"]<br>}
    end
    deactivate PN
    