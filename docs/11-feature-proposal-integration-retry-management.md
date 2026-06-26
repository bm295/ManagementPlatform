# User Story: Retry Failed Checkout Integrations

## Story

As an operations user, I want to view failed checkout integrations and retry a selected integration message, so that I can recover from transient downstream outages without asking a customer to place the order again.

## Background

The platform currently exposes checkout status and dead-letter messages. Those views help an operator identify that an integration failed, but they do not describe an operator workflow for recovery. A retry story gives the product a clear path for handling failures such as a temporary email provider outage while preserving checkout state and idempotency.

## Acceptance Criteria

### Scenario: View retryable integration failures

Given at least one checkout integration has failed with a retryable error,
when the operations user requests the retryable integrations list,
then the response includes the failed integration message,
and the response includes the checkout attempt ID, integration type, current status, attempt count, last error, and failure timestamp.

### Scenario: Retry one failed integration

Given a retryable checkout integration exists,
when the operations user retries that integration with a new idempotency key,
then the platform executes only that integration message,
and the response includes the updated status, attempt count, and last error.

### Scenario: Successful retry clears the operational failure

Given a failed integration is retried successfully,
when the operations user views retryable integrations again,
then that integration no longer appears in the retryable list,
and the related checkout status shows the integration as completed.

### Scenario: Idempotent retry request is replayed

Given an operations user already retried an integration with an idempotency key,
when the same retry request is submitted again with the same key and target integration,
then the platform returns the original retry result,
and the integration side effect is not executed a second time.

### Scenario: Idempotency conflict is rejected

Given an idempotency key was used for one integration retry,
when the operations user submits the same key for a different integration retry,
then the platform rejects the request with a conflict response,
and no integration side effect is executed.

### Scenario: Completed integrations cannot be retried

Given an integration message is already completed,
when the operations user attempts to retry it,
then the platform rejects the request as invalid,
and the completed integration is not executed again.

## Notes for Implementation

- Add an application use case for retrying an integration message.
- Keep retry execution behind an application port so the use case does not depend on HTTP or infrastructure details.
- Add presentation routes only as adapters around the use case.
- Keep bootstrap limited to wiring the new use case and adapter dependencies.
- Preserve existing checkout idempotency behavior; retry idempotency should apply only to retry commands.

## Suggested API Shape

```http
GET /api/integrations/retryable?page=1&pageSize=20
```

```http
POST /api/integrations/{outboxMessageId}/retry
Content-Type: application/json

{"idempotencyKey":"retry-email-1001"}
```

## Definition of Done

- The retryable integrations list is covered by automated checks.
- The retry endpoint is covered for success, missing integration, invalid completed integration, idempotent replay, and idempotency conflict.
- Existing checkout and order checks continue to pass.
- API documentation includes the retry workflow and example responses.
