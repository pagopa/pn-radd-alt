## Istruzioni per la compilazione
```
    ./mvnw clean install && ./mvnw -DCI_PROFILE clean install
```

## Istruzioni per la gestione delle configurazioni per il csv dello store locator

I campi che è possibile inserire all'interno del csv prelevandoli dall'entità Pn-RaddRegistry (tabella contenente i punti di ritiro SEND presenti sul territorio) 
sono i seguenti:
```
    - description
    - city
    - address
    - province
    - zipCode
    - phoneNumber
    - openingTime :  orari di apertura con struttura Mon=09:00-12:00_16:00-19:00#Tue=09:00-19:00.
    - monday : es.09:00-12:00_16:00-19:00
    - tuesday : es.09:00-12:00_16:00-19:00
    - wednesday : es.09:00-12:00_16:00-19:00 
    - thursday : es.09:00-12:00_16:00-19:00
    - friday : es.09:00-12:00_16:00-19:00
    - saturday : es.09:00-12:00_16:00-19:00
    - sunday : es.09:00-12:00_16:00-19:00
    - latitude
    - longitude
```

La configurazione da inserire sul parameter store nel parametro `/pn-radd-alt/csv-configuration` deve avere la seguente struttura:
```
{
    "version":"1", //ogni nuova configurazione deve incrementare la version
    "configs":[
        {
            "header": "descrizione", //nome della colonna del csv
            "field": "description" //nome del campo su radd-alt (uno dei valori al punto 1)
        },
        {
            "header": "città",
            "field": "city"
        },
        {
            "header": "cap",
            "field": "zipCode"
        },
        {
            "header": "via",
            "field": "address"
        },
        {
            "header": "provincia",
            "field": "province"
        },
        {
            "header": "URL"
        },
        {
            "header": "Email"
        },
        {
            "header": "Type"
        },
        {
            "header": "telefono",
            "field": "phoneNumber"
        }
    ]
}
```

**N.B**

1. Il campo Field può essere popolato solo con uno dei valori del punto 1.
Qualora non sia presente, nella lista al punto 1, il campo corrispondente alla colonna (header) del csv richiesta, il campo field non dovrà essere inserito.

2. Ogni nuova configurazione deve incrementare il campo version. 
Questo parametro è fondamentale per avviare una nuova generazione quando la struttura del csv viene modificata, 
anche nel caso in cui non sia trascorso l’intervallo di tempo configurato.