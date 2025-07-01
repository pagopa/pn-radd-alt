package it.pagopa.pn.radd.middleware.db.entities;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

@DynamoDbBean
@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = false)
public class NormalizedAddressEntity extends AddressEntity {
    public static final String COL_LATITUDE = "latitude";
    public static final String COL_LONGITUDE = "longitude";
    public static final String COL_BIAS_POINT = "biasPoint";

    private String latitude;
    private String longitude;
    private Integer biasPoint;
}
