#!/bin/sh
set -e
awslocal sqs create-queue --queue-name quote-pricing-requests >/dev/null
awslocal sqs create-queue --queue-name quote-pricing-results >/dev/null

awslocal sqs create-queue --queue-name quote-notification-events >/dev/null
awslocal sqs create-queue --queue-name quote-pricing-results-dlq >/dev/null
awslocal sqs create-queue --queue-name quote-notification-events-dlq >/dev/null
