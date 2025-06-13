# Punti di Ritiro
Il micro-servizio pn-radd-alt, gestisce i punti di ritiro di SEND (partner-offices) messi a disposizione dai partner. I punti di ritiro sono postazioni fisiche di soggetti esterni a PagoPA ( partner), integrati a SEND, che offrono la possibilità di accedere alle notifiche ai cittadini sul territorio. 

E' possibile interagire puntualmente con i punti di ritiro attraverso gli scenari 

- Creazione di un punto di ritiro  (PUT)
- Modifica di un punto di ritiro  (PATCH)
- Cancellazione di un punto di ritiro  (DELETE)
- Lettura di un punto di ritiro  (GET)


GET    /partners/{partner_id}/partner-offices
GET    /partners/{partner_id}/partner-offices/{office_id}
POST   /partners/{partner_id}/partner-offices
PUT    /partners/{partner_id}/partner-offices/{office_id}
DELETE /partners/{partner_id}/partner-offices/{office_id}

Oppure in modalità massiva per tutti i punti di ritiro della medesima società 


## Punto di Ritiro (partner-office)
Un Punto di ritiro è rappresentato da: 
- PartnerId : identificativo (vatCode) della società partner 
- partnerUrl : Url del partner 
- name : Nome 
- description : Descrizione 
- street : Indirizzo 
- opening_hours : orario di apertura
- phone : contatto telefonico 
- email : contatto e-mail 
- appointament : identifica se il punto di ritiro è solo su appuntamento



POST /partners/{partner_id}/partner-offices/bulk   # append
PUT  /partners/{partner_id}/partner-offices/bulk   # replace all


GET /aziende/{azienda_id}/punti-ritiro/{id}
→ Recupera un singolo punto

POST /aziende/{azienda_id}/punti-ritiro
→ Crea un nuovo punto di ritiro

PUT /aziende/{azienda_id}/punti-ritiro/{id}
→ Aggiorna un punto di ritiro esistente (sostituzione)

PATCH /aziende/{azienda_id}/punti-ritiro/{id}
→ Aggiorna parzialmente

DELETE /aziende/{azienda_id}/punti-ritiro/{id}
→ Cancella il punto


Oppure in modo massivo
- Creazione di una sigla ( replace-all PUT) 
- Aggiunta di punti di ritiro  (append POST)


POST /aziende/{azienda_id}/punti-ritiro/massive
PUT /aziende/{azienda_id}/punti-ritiro/massive
DELETE /aziende/{azienda_id}/punti-ritiro/massive