package it.pagopa.pn.radd.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StoreLocatorCsvEntity {
    private String description;
    private String city;
    private String address;
    private String province;
    private String zipCode;
    private String phoneNumber;
    private String openingTime;
    private String monday;
    private String tuesday;
    private String wednesday;
    private String thursday;
    private String friday;
    private String saturday;
    private String sunday;
    private String latitude;
    private String longitude;

    public String retrieveFieldValue(String fieldName){
        return switch (fieldName) {
            case "description" -> description;
            case "city" -> city;
            case "address" -> address;
            case "province" -> province;
            case "zipCode" -> zipCode;
            case "phoneNumber" -> phoneNumber;
            case "openingTime" -> openingTime;
            case "monday" -> monday;
            case "tuesday" -> tuesday;
            case "wednesday" -> wednesday;
            case "thursday" -> thursday;
            case "friday" -> friday;
            case "saturday" -> saturday;
            case "sunday" -> sunday;
            case "latitude" -> latitude;
            case "longitude" -> longitude;
            default -> null;
        };
    }
}
