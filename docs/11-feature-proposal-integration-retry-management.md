# User Story: Recover Failed Checkout Follow-Up Actions

## Story

As a commerce operations manager, I want the business to quickly identify and recover checkout follow-up actions that did not complete, so that customers receive the expected order confirmations and services without needing to place duplicate orders or contact support.

## Background

When checkout follow-up actions fail, customers may experience missing confirmations, delayed fulfillment, or incomplete post-purchase services even though payment and order capture were successful. Operations teams need a controlled recovery workflow that lets the business resolve temporary partner or service disruptions while protecting customers from duplicate communications, duplicate fulfillment, or repeated charges.

## Acceptance Criteria

### Scenario: Review checkout follow-up actions that need attention

Given one or more checkout follow-up actions did not complete and can still be recovered,
when an operations user reviews items needing attention,
then the user can see each affected checkout,
and the user can understand what follow-up action failed, its current business status, how many recovery attempts have been made, the most recent failure reason, and when the issue last occurred.

### Scenario: Recover one failed checkout follow-up action

Given a checkout follow-up action is eligible for recovery,
when an operations user starts recovery for that specific action,
then only the selected follow-up action is attempted again,
and the user receives the latest recovery status, attempt count, and any remaining failure reason.

### Scenario: Successful recovery removes the item from the work queue

Given a failed checkout follow-up action is recovered successfully,
when the operations user reviews items needing attention again,
then the recovered item no longer appears in the work queue,
and the related checkout shows that the follow-up action is complete.

### Scenario: Duplicate recovery request does not duplicate customer impact

Given an operations user already requested recovery for a checkout follow-up action,
when the same recovery request is submitted again,
then the user receives the original recovery result,
and the customer or downstream partner does not receive the same follow-up action twice.

### Scenario: Recovery request for a different item is treated separately

Given a recovery request has already been recorded for one checkout follow-up action,
when an operations user attempts to reuse that same recovery request for a different follow-up action,
then the platform rejects the request as a conflict,
and no customer or downstream partner action is performed.

### Scenario: Completed follow-up actions cannot be recovered again

Given a checkout follow-up action is already complete,
when an operations user attempts to recover it again,
then the platform rejects the request as invalid,
and the customer or downstream partner does not receive a duplicate follow-up action.

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

- The recovery work queue is covered by automated checks.
- The retry endpoint is covered for success, missing integration, invalid completed integration, idempotent replay, and idempotency conflict.
- Existing checkout and order checks continue to pass.
- API documentation includes the recovery workflow and example responses.
