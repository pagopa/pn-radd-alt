package it.pagopa.pn.radd.middleware.db.entities;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.time.Instant;
import java.time.LocalDate;

@DynamoDbBean
@Setter
@ToString
@EqualsAndHashCode
public class CoverageEntity {

    public static final String COL_UID = "cap";
    public static final String COL_CREATION_TIMESTAMP = "cap";
    public static final String COL_UPDATE_TIMESTAMP = "cap";
    public static final String COL_CAP = "cap";
    public static final String COL_LOCALITY = "locality";
    public static final String COL_PROVINCE = "province";
    public static final String COL_CADASTRAL_CODE = "cadastralCode";
    public static final String COL_START_VALIDITY = "startValidity";
    public static final String COL_END_VALIDITY = "endValidity";

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_UID)}))
    private String uid;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_CREATION_TIMESTAMP)}))
    private Instant creationTimestamp;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_UPDATE_TIMESTAMP)}))
    private Instant updateTimestamp;

    @Getter(onMethod = @__({@DynamoDbPartitionKey, @DynamoDbAttribute(COL_CAP)}))
    private String cap;

    @Getter(onMethod = @__({@DynamoDbSortKey, @DynamoDbAttribute(COL_LOCALITY)}))
    private String locality;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_PROVINCE)}))
    private String province;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_CADASTRAL_CODE)}))
    private String cadastralCode;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_START_VALIDITY)}))
    private LocalDate startValidity;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_END_VALIDITY)}))
    private LocalDate endValidity;

}