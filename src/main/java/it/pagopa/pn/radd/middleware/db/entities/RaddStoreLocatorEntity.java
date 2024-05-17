package it.pagopa.pn.radd.middleware.db.entities;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.time.Instant;

@DynamoDbBean
@Setter
@ToString
@EqualsAndHashCode
public class RaddStoreLocatorEntity {

    public static final String COL_PK = "pk";
    public static final String COL_CREATEDAT = "createdAt";
    public static final String COL_DIGEST = "digest";
    public static final String COL_STATUS = "status";
    public static final String COL_TTL = "ttl";
    public static final String COL_CSVTYPE = "csvType";

    public static final String STATUS_CREATEDAT_INDEX = "status-createdAt-index";
    public static final String CSVTYPE_CREATEDAT_INDEX = "csvType-createdAt-index";


    @Getter(onMethod = @__({@DynamoDbPartitionKey, @DynamoDbAttribute(COL_PK)}))
    private String pk;
    @Getter(onMethod = @__({@DynamoDbSortKey, @DynamoDbSecondarySortKey(indexNames = {STATUS_CREATEDAT_INDEX, CSVTYPE_CREATEDAT_INDEX}), @DynamoDbAttribute(COL_CREATEDAT)}))
    private Instant createdAt;
    @Getter(onMethod = @__({@DynamoDbAttribute(COL_DIGEST)}))
    private String digest;
    @Getter(onMethod = @__({@DynamoDbSecondaryPartitionKey(indexNames = STATUS_CREATEDAT_INDEX), @DynamoDbAttribute(COL_STATUS)}))
    private String status;
    @Getter(onMethod = @__({@DynamoDbSecondaryPartitionKey(indexNames = CSVTYPE_CREATEDAT_INDEX), @DynamoDbAttribute(COL_CSVTYPE)}))
    private String csvType;
    @Getter(onMethod = @__({@DynamoDbAttribute(COL_TTL)}))
    private Long ttl;


}
