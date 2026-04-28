# Extra Work for the Demo

This file lists small improvements that can be added to the demo without changing the main 4-day deadline.

These are optional items. Pick only a few if there is time left after the core flow is done.

## Good Demo Additions

- Add a clear status badge for `Draft`, `Paid`, and `ProductionQueued`.
- Add a small event history on the order page, such as payment success, invoice queued, and production queued.
- Add a filter for order status in the demo page.
- Add a quick button to create a new idempotency key in the UI.
- Add a small admin view for failed outbox messages.
- Add manual retry for failed outbox messages in the demo.
- Add more details in the dead letter view, such as payload and retry history.
- Add a page or section that shows mock integration logs.
- Add a clearer error message when checkout is rejected because the order is already paid or processing.
- Add pagination controls to the order list.
- Add a short health endpoint and show its status in the demo.

## Nice to Have if Time Allows

- Export a simple invoice preview for the demo.
- Add a separate screen that shows all checkout attempts for one order.
- Add a separate view for payment failures and manual retry with a new checkout request.
- Add a simple loading state for each action in the UI.
- Add a banner that explains whether the demo is using current data or fresh reset data.
- Add screenshots or a short demo script in the docs.

## What Not to Add in a 4-Day Demo

- Real payment integration
- Real email provider integration
- Real invoice system integration
- Full authentication and authorization
- Full monitoring stack
- Complex admin tools
- Large frontend redesign

## How to Use This List

- Finish the core scenario first.
- Use leftover time for 1-3 small improvements from the first list.
- Do not add stretch items if they risk breaking the main demo flow.
