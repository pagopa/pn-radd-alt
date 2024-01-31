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
            },
        ]'

echo "### CREATE OPERATIONS AND IUNS TABLE ###"

aws --profile default --region us-east-1 --endpoint-url=http://localstack:4566 \
    dynamodb create-table \
    --table-name pn-operations-iuns-alt \
    --attribute-definitions \
        AttributeName=id,AttributeType=S \
        AttributeName=operationId,AttributeType=S \
        AttributeName=iun,AttributeType=S \
    --key-schema \
        AttributeName=id,KeyType=HASH \
    --provisioned-throughput \
        ReadCapacityUnits=10,WriteCapacityUnits=5 \
    --global-secondary-indexes \
    "[
        {
            \"IndexName\": \"iun-and-operation-index\",
            \"KeySchema\": [
                {\"AttributeName\":\"iun\",\"KeyType\":\"HASH\"},
                {\"AttributeName\":\"operationId\",\"KeyType\":\"RANGE\"}
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

    echo "### CREATE QUEUES FOR RADD-ALT ###"
    queues="pn-safestore_to_radd_alt"
    for qn in $(echo $queues | tr " " "\n"); do
      echo creating queue $qn ...
      aws --profile default --region us-east-1 --endpoint-url http://localstack:4566 \
        sqs create-queue \
        --attributes '{"DelaySeconds":"2"}' \
        --queue-name $qn
    done
echo "Initialization terminated"