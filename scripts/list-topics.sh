#!/bin/bash
# List Kafka topics

docker exec codeops-kafka kafka-topics --list --bootstrap-server localhost:9092
