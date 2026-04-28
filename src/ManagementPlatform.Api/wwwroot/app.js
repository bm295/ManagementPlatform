const orderStatus = ["Draft", "CheckoutProcessing", "Paid", "ProductionQueued"];
const checkoutStatus = ["PaymentPending", "PaymentFailed", "PaymentSucceeded"];
const paymentStatus = ["Failed", "Succeeded"];
const outboxStatus = ["Pending", "Processing", "Succeeded", "Failed"];
const outboxType = ["Checkout email", "Invoice", "Production push"];

const state = {
  orders: [],
  selectedOrderId: null,
  selectedCheckoutId: null
};

const ordersElement = document.querySelector("#orders");
const orderDetailsElement = document.querySelector("#orderDetails");
const checkoutResultElement = document.querySelector("#checkoutResult");
const deadLettersElement = document.querySelector("#deadLetters");
const selectedOrderLabel = document.querySelector("#selectedOrderLabel");
const checkoutButton = document.querySelector("#checkoutButton");
const reloadCheckoutButton = document.querySelector("#reloadCheckoutButton");
const reloadDeadLettersButton = document.querySelector("#reloadDeadLettersButton");
const idempotencyKeyInput = document.querySelector("#idempotencyKey");
const toast = document.querySelector("#toast");

document.querySelector("#searchForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  await loadOrders();
});

document.querySelector("#refreshButton").addEventListener("click", async () => {
  await loadOrders();
  if (state.selectedCheckoutId) {
    await loadCheckout(state.selectedCheckoutId);
  }
  await loadDeadLetters();
});

document.querySelector("#checkoutForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  await checkoutSelectedOrder();
});

reloadCheckoutButton.addEventListener("click", async () => {
  if (state.selectedCheckoutId) {
    await loadCheckout(state.selectedCheckoutId);
  }
});

reloadDeadLettersButton.addEventListener("click", loadDeadLetters);

loadOrders();
loadDeadLetters();

async function loadOrders() {
  const name = document.querySelector("#searchInput").value.trim();
  const query = new URLSearchParams({ page: "1", pageSize: "20" });
  if (name) {
    query.set("name", name);
  }

  const result = await apiGet(`/api/orders?${query}`);
  state.orders = result.items;
  renderOrders();
}

async function selectOrder(orderId) {
  state.selectedOrderId = orderId;
  state.selectedCheckoutId = null;
  reloadCheckoutButton.disabled = true;
  checkoutResultElement.className = "details empty";
  checkoutResultElement.textContent = "No checkout has been run yet.";

  const order = await apiGet(`/api/orders/${orderId}`);
  selectedOrderLabel.textContent = order.name;
  checkoutButton.disabled = false;
  idempotencyKeyInput.value = `demo-${crypto.randomUUID()}`;
  renderOrderDetails(order);
  renderOrders();
}

async function checkoutSelectedOrder() {
  if (!state.selectedOrderId) {
    return;
  }

  const payload = {
    idempotencyKey: idempotencyKeyInput.value.trim(),
    paymentMethodToken: document.querySelector("#paymentToken").value
  };

  try {
    const result = await apiPost(`/api/orders/${state.selectedOrderId}/checkout`, payload);
    state.selectedCheckoutId = result.checkoutId;
    reloadCheckoutButton.disabled = false;
    renderCheckout(result);
    await loadOrders();
    await loadDeadLetters();
    showToast("Checkout request completed.");
  } catch (error) {
    showToast(error.message);
  }
}

async function loadCheckout(checkoutId) {
  const result = await apiGet(`/api/checkouts/${checkoutId}`);
  renderCheckout(result);
}

function renderOrders() {
  if (!state.orders.length) {
    ordersElement.innerHTML = `<div class="details empty">No orders found.</div>`;
    return;
  }

  ordersElement.innerHTML = state.orders.map(order => {
    const active = order.id === state.selectedOrderId ? " active" : "";
    const statusText = enumName(orderStatus, order.status);
    return `
      <button class="order-row${active}" type="button" data-order-id="${order.id}">
        <span>
          <span class="order-title">${escapeHtml(order.name)}</span>
          <span class="order-meta">${escapeHtml(order.customerName)} | ${formatMoney(order.amount, order.currency)}</span>
        </span>
        <span class="status ${statusClass(statusText)}">${statusText}</span>
      </button>
    `;
  }).join("");

  ordersElement.querySelectorAll("[data-order-id]").forEach(button => {
    button.addEventListener("click", () => selectOrder(button.dataset.orderId));
  });
}

function renderOrderDetails(order) {
  orderDetailsElement.className = "details";
  orderDetailsElement.innerHTML = `
    <dl>
      <dt>Order</dt><dd>${escapeHtml(order.name)}</dd>
      <dt>Customer</dt><dd>${escapeHtml(order.customerName)}</dd>
      <dt>Email</dt><dd>${escapeHtml(order.customerEmail)}</dd>
      <dt>Amount</dt><dd>${formatMoney(order.amount, order.currency)}</dd>
      <dt>Status</dt><dd>${enumName(orderStatus, order.status)}</dd>
      <dt>Created</dt><dd>${formatDate(order.createdAt)}</dd>
      <dt>Paid</dt><dd>${order.paidAt ? formatDate(order.paidAt) : "Not paid yet"}</dd>
    </dl>
  `;
}

function renderCheckout(result) {
  const checkoutText = enumName(checkoutStatus, result.status);
  const paymentText = result.paymentStatus === null
    ? "Not charged"
    : enumName(paymentStatus, result.paymentStatus);

  checkoutResultElement.className = "details";
  checkoutResultElement.innerHTML = `
    <dl>
      <dt>Checkout id</dt><dd>${escapeHtml(result.checkoutId)}</dd>
      <dt>Order id</dt><dd>${escapeHtml(result.orderId)}</dd>
      <dt>Checkout status</dt><dd><span class="status ${statusClass(checkoutText)}">${checkoutText}</span></dd>
      <dt>Payment status</dt><dd>${paymentText}</dd>
      <dt>Failure reason</dt><dd>${result.failureReason ? escapeHtml(result.failureReason) : "None"}</dd>
    </dl>
    ${renderIntegrations(result.integrations)}
  `;
}

function renderIntegrations(integrations) {
  if (!integrations.length) {
    return `<div class="details empty" style="margin-top:14px">No follow-up work was created.</div>`;
  }

  return `
    <div class="integrations">
      ${integrations.map(item => {
        const typeText = enumName(outboxType, item.type);
        const statusText = enumName(outboxStatus, item.status);
        return `
          <div class="integration">
            <h3>${typeText}</h3>
            <div class="status ${statusClass(statusText)}">${statusText}</div>
            <p class="muted">Attempts: ${item.attempts}</p>
            ${item.lastError ? `<p>${escapeHtml(item.lastError)}</p>` : ""}
          </div>
        `;
      }).join("")}
    </div>
  `;
}

async function loadDeadLetters() {
  const result = await apiGet("/api/dead-letters");
  renderDeadLetters(result);
}

function renderDeadLetters(items) {
  if (!items.length) {
    deadLettersElement.className = "details empty";
    deadLettersElement.textContent = "No dead letter messages.";
    return;
  }

  deadLettersElement.className = "integrations";
  deadLettersElement.innerHTML = items.map(item => `
    <div class="integration">
      <h3>${enumName(outboxType, item.type)}</h3>
      <p class="muted">Failed at: ${formatDate(item.failedAt)}</p>
      <p class="muted">Attempts: ${item.attemptCount}</p>
      <p>${escapeHtml(item.failureReason)}</p>
      <p class="muted">Checkout: ${escapeHtml(item.checkoutAttemptId)}</p>
    </div>
  `).join("");
}

async function apiGet(url) {
  const response = await fetch(url);
  return readResponse(response);
}

async function apiPost(url, payload) {
  const response = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload)
  });

  return readResponse(response);
}

async function readResponse(response) {
  const body = await response.json().catch(() => null);

  if (!response.ok) {
    throw new Error(body?.detail || body?.title || `Request failed with ${response.status}`);
  }

  return body;
}

function enumName(values, value) {
  if (typeof value === "string") {
    return value;
  }

  return values[value] ?? String(value);
}

function statusClass(status) {
  const lower = status.toLowerCase();
  if (lower.includes("failed")) {
    return "failed";
  }
  if (lower.includes("succeeded") || lower.includes("paid") || lower.includes("queued")) {
    return "succeeded";
  }
  return "pending";
}

function formatMoney(amount, currency) {
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency
  }).format(amount);
}

function formatDate(value) {
  return new Intl.DateTimeFormat("en-US", {
    dateStyle: "medium",
    timeStyle: "short"
  }).format(new Date(value));
}

function escapeHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function showToast(message) {
  toast.textContent = message;
  toast.hidden = false;
  window.clearTimeout(showToast.timeout);
  showToast.timeout = window.setTimeout(() => {
    toast.hidden = true;
  }, 3500);
}
