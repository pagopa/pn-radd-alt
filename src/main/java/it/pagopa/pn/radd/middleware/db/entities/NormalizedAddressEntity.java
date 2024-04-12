package it.pagopa.pn.radd.middleware.db.entities;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@DynamoDbBean
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class NormalizedAddressEntity {
    public static final String COL_ADDRESS_ROW = "addressRow";
    public static final String COL_CAP = "cap";
    public static final String COL_CITY = "city";
    public static final String COL_PR = "pr";
    public static final String COL_COUNTRY = "country";

    private String addressRow;
    private String cap;
    private String city;
    private String pr;
    private String country;

}
