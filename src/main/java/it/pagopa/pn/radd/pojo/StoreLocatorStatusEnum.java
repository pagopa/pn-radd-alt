package it.pagopa.pn.radd.pojo;

public enum StoreLocatorStatusEnum {
    TO_UPLOAD("Prima persistenza, l’upload del file non è ancora stato effettuato"),
    UPLOADED("File caricato correttamente"),
    REPLACED("File sostituito da una generazione successiva"),
    DUPLICATE("Il digest calcolato è uguale all’ultima versione caricata su S3, non è stato quindi effettuato il nuovo upload"),
    ERROR("Errore durante la generazione del file");

    private final String description;

    StoreLocatorStatusEnum(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return this.name() + " (" + description + ")";
    }
}
