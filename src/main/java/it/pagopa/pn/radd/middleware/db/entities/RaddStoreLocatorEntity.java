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
    public static final String COL_CSVCONFIGURATIONVERSION = "csvConfigurationVersion";
    public static final String COL_VERSIONID = "versionId";

    public static final String STATUS_CREATEDAT_INDEX = "status-createdAt-index";
    public static final String CSVCONFIGURATIONVERSION_CREATEDAT_INDEX = "csvConfigurationVersion-createdAt-index";


    @Getter(onMethod = @__({@DynamoDbPartitionKey, @DynamoDbAttribute(COL_PK)}))
    private String pk;
    @Getter(onMethod = @__({@DynamoDbAttribute(COL_VERSIONID)}))
    private String versionId;
    @Getter(onMethod = @__({@DynamoDbSecondarySortKey(indexNames = {STATUS_CREATEDAT_INDEX, CSVCONFIGURATIONVERSION_CREATEDAT_INDEX}), @DynamoDbAttribute(COL_CREATEDAT)}))
    private Instant createdAt;
    @Getter(onMethod = @__({@DynamoDbAttribute(COL_DIGEST)}))
    private String digest;
    @Getter(onMethod = @__({@DynamoDbSecondaryPartitionKey(indexNames = STATUS_CREATEDAT_INDEX), @DynamoDbAttribute(COL_STATUS)}))
    private String status;
    @Getter(onMethod = @__({@DynamoDbSecondaryPartitionKey(indexNames = CSVCONFIGURATIONVERSION_CREATEDAT_INDEX), @DynamoDbAttribute(COL_CSVCONFIGURATIONVERSION)}))
    private String csvConfigurationVersion;
    @Getter(onMethod = @__({@DynamoDbAttribute(COL_TTL)}))
    private Long ttl;


}
