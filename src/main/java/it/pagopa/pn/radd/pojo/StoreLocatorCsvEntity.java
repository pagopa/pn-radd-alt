package it.pagopa.pn.radd.pojo;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvBindByPosition;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StoreLocatorCsvEntity {

    @CsvBindByName(column = "descrizione")
    @CsvBindByPosition(position = 0)
    private String denomination;

    @ToString.Exclude
    @CsvBindByName(column = "citta")
    @CsvBindByPosition(position = 1)
    private String city;

    @ToString.Exclude
    @CsvBindByName(column = "via")
    @CsvBindByPosition(position = 2)
    private String address;

    @ToString.Exclude
    @CsvBindByName(column = "provincia")
    @CsvBindByPosition(position = 3)
    private String province;

    @ToString.Exclude
    @CsvBindByName(column = "cap")
    @CsvBindByPosition(position = 4)
    private String zipCode;

    @ToString.Exclude
    @CsvBindByName(column = "telefono")
    @CsvBindByPosition(position = 5)
    private String contacts;

    private String monday;

    private String tuesday;

    private String wednesday;

    private String thursday;

    private String friday;

    private String saturday;

    private String sunday;

    private String latitude;

    private String longitude;
}
