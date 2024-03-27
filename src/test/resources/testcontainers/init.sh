echo "### CREATE RADD ALT TRANSACTION TABLE ###"

aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb create-table \
    --table-name pn-radd-transaction-alt \
    --attribute-definitions \
        AttributeName=transactionId,AttributeType=S \
        AttributeName=operationType,AttributeType=S \
        AttributeName=iun,AttributeType=S \
        AttributeName=recipientId,AttributeType=S \
        AttributeName=delegateId,AttributeType=S \
    --key-schema \
        AttributeName=transactionId,KeyType=HASH \
        AttributeName=operationType,KeyType=RANGE \
    --provisioned-throughput ReadCapacityUnits=10,WriteCapacityUnits=5 \
    --global-secondary-indexes \
        '[
            {
                "IndexName": "iun-transaction-index",
                "KeySchema": [{"AttributeName":"iun","KeyType":"HASH"}],
                "Projection": {"ProjectionType":"ALL"},
                "ProvisionedThroughput": {"ReadCapacityUnits": 10, "WriteCapacityUnits": 5}
            },
            {
                "IndexName": "recipient-transaction-index",
                "KeySchema": [{"AttributeName":"recipientId","KeyType":"HASH"}],
                "Projection": {"ProjectionType":"ALL"},
                "ProvisionedThroughput": {"ReadCapacityUnits": 10, "WriteCapacityUnits": 5}
            },
            {
                "IndexName": "delegate-transaction-index",
                "KeySchema": [{"AttributeName":"delegateId","KeyType":"HASH"}],
                "Projection": {"ProjectionType":"ALL"},
                "ProvisionedThroughput": {"ReadCapacityUnits": 10, "WriteCapacityUnits": 5}
            }
        ]'

echo "### CREATE OPERATIONS AND IUNS TABLE ###"

aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb create-table \
    --table-name pn-operations-iuns-alt \
    --attribute-definitions \
        AttributeName=transactionId,AttributeType=S \
        AttributeName=iun,AttributeType=S \
    --key-schema \
        AttributeName=transactionId,KeyType=HASH \
        AttributeName=iun,KeyType=RANGE \
    --provisioned-throughput \
        ReadCapacityUnits=10,WriteCapacityUnits=5 \
    --global-secondary-indexes \
    "[
        {
            \"IndexName\": \"iun-transaction-index\",
            \"KeySchema\": [
                {\"AttributeName\":\"iun\",\"KeyType\":\"HASH\"},
                {\"AttributeName\":\"transactionId\",\"KeyType\":\"RANGE\"}
            ],
            \"Projection\":{
                \"ProjectionType\":\"ALL\"
            },
            \"ProvisionedThroughput\": {
                \"ReadCapacityUnits\": 10,
                \"WriteCapacityUnits\": 5
            }
        }
    ]"

echo "### CREATE PN ANAGRAFICHE RADD TABLE ###"

#zipCode-index: pk su "zipCode", projection ALL
#cxId-requestId-index: pk su “cxId" e sk su "requestId" , projection ALL

aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb create-table \
    --table-name Pn-RaddRegistry \
    --attribute-definitions \
    AttributeName=registryId,AttributeType=S \
    AttributeName=cxId,AttributeType=S \
    AttributeName=zipCode,AttributeType=S \
    AttributeName=requestId,AttributeType=S \
    --key-schema \
    AttributeName=registryId,KeyType=HASH \
    AttributeName=cxId,KeyType=RANGE \
    --provisioned-throughput \
    ReadCapacityUnits=10,WriteCapacityUnits=5 \
    --global-secondary-indexes \
    "[
        {
            \"IndexName\":\"zipCode-index\",
            \"KeySchema\":[
                {\"AttributeName\":\"zipCode\",\"KeyType\":\"HASH\"},
            ],
            \"Projection\":{
                \"ProjectionType\":\"ALL\"
            },
            \"ProvisionedThroughput\":{
                \"ReadCapacityUnits\":10,
                \"WriteCapacityUnits\":5
            }
        },{
            \"IndexName\":\"cxId-requestId-index\",
            \"KeySchema\":[
                {\"AttributeName\":\"cxId\",\"KeyType\":\"HASH\"},
                {\"AttributeName\":\"requestId\",\"KeyType\":\"RANGE\"}
            ],
            \"Projection\":{
                \"ProjectionType\":\"ALL\"
            },
            \"ProvisionedThroughput\":{
                \"ReadCapacityUnits\":10,
                \"WriteCapacityUnits\":5
            }
        }
    ]"

echo "Initialization terminated"