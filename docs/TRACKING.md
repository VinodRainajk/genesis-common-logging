# Genesis tracking logs

This document is the contract for **cross-service Splunk tracing** using a single `trackingId` column.

Java services use the Maven library `genesis.common:logging` (`genesis.common.logging`).  
Python services do **not** use that jar. They must print the **same log shape** and follow the **same rules** so Splunk can join hops.

If the orchestrator (or caller) does **not** send `runNodeID` / correlation on a hop, this service cannot invent a chain. Logging only tags what arrived.

---

## 1. Log line (all languages)

```text
2026-09-24 23:21:00.123 INFO  spring-logging TestApiController node-1 GET received
```

| Column | Source | Example |
|--------|--------|---------|
| Date + time | Logger | `2026-09-24 23:21:00.123` |
| Level | `INFO` / `ERROR` / `DEBUG` | `INFO` |
| Service name | App config (`spring.application.name` in Java, app name in Python) | `spring-logging` |
| Class / logger | Logger name (simple class name, **not** a stack walk) | `TestApiController` |
| Tracking id | MDC / context, or literal `null` | `node-1` or `null` |
| Message | Your statement | `GET received` |

Do **not** put ids inside the message (`log.info("GET received")` stays that short).

Do **not** add `%M` / Python `funcName` from a stack walk on every INFO line. Class name from logger init is enough and cheap.

### Java pattern (library default)

Uses `spring.application.name` and MDC key `trackingId`:

```text
%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level ${spring.application.name:-} %logger{0} %X{trackingId:-null} %msg%n
```

### Python pattern

```text
%(asctime)s %(levelname)s %(name)s %(trackingId)s %(message)s
```

`asctime` formatted as `yyyy-MM-dd HH:mm:ss.SSS`.  
`name` should be the **short** logger name (class/module), not a huge dotted path if you can avoid it.  
`trackingId` comes from a `logging.Filter` reading `contextvars` (see below). Missing → `null`.

---

## 2. What `trackingId` means

`trackingId` is **this service’s configured join key**, not “any id we found”.

Typical keys in this estate:

- **`runNodeID`** in JSON body (~70% of services)
- **Correlation** in a **header** (or sometimes body)
- Other tracking headers only if this service **lists** them

**Allow-list only.** If this app is configured for `X-Run-Node-Id` and a new caller sends `X-Correlation-Id`, **ignore** correlation. Do not set `trackingId` from it.

An **orchestrator** usually sends the same run node / correlation on every hop. Each service `init`s from **its inbound** message. Services do not need to copy ids outbound unless they call another app **without** the orchestrator (direct HTTP or Kafka produce).

---

## 3. Core API

| Action | Java | Python |
|--------|------|--------|
| Clear then set | `Tracking.init(id)` | `tracking.init(id)` |
| Read | `Tracking.get()` | `tracking.get()` |
| Clear (adapters) | `Tracking.clear()` | `tracking.clear()` |

`init(null)` or blank → column prints `null`.

**Developers never `try/finally`.** HTTP/Kafka **doors** clear at entry (and Java HTTP filter also clears on the way out).  
**You** call `init` when the id lives in the **body** or at the **start of each Kafka record** in a batch loop.

Scope is **this request** or **this Kafka record** on **this thread / context**. Not a field on a Spring `@Service` or a Python singleton. Next request on a pooled thread must `init`/`clear` again.

Threads are **per JVM / per process**. Sharing the Java library or a Python helper does **not** share threads across microservices.

---

## 4. Java (`genesis.common.logging`)

### 4.1 Dependency

```xml
<dependency>
    <groupId>genesis.common</groupId>
    <artifactId>logging</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

Auto-config registers:

- HTTP `OncePerRequestFilter` (servlet apps)
- Kafka `RecordInterceptor` + `BatchInterceptor` (only if `spring-kafka` is on the classpath)

`spring-kafka` is **optional**. A pure web app does not need it. A non-batch consumer does not fail because `BatchInterceptor` exists; Spring only runs **one** interceptor type per listener.

### 4.2 Configuration

```properties
# Service column in every log line
spring.application.name=order-service

# HTTP headers (default off — body/run-node majority)
genesis.logging.header.enabled=false
genesis.logging.header.fields=X-Run-Node-Id
genesis.logging.header.path-patterns=/api/orders/**

# Kafka record headers (record listeners only)
genesis.logging.kafka.header.enabled=false
genesis.logging.kafka.header.fields=X-Run-Node-Id

# Set false to keep the app's own logging.pattern.console
genesis.logging.pattern.enabled=true
```

`fields` is an **ordered allow-list**. First non-blank wins. Unlisted headers are noise.

`path-patterns` empty + `header.enabled=true` → all URLs. The HTTP **filter cannot see the Java controller class**; paths are the filter equivalent of “this API”.

---

## 5. Python (same contract, no jar)

Use **`contextvars`**, not a global `tracking_id = ...`. Contextvars follow asyncio tasks; a plain global will leak across requests on the same worker.

### 5.1 Minimal helper

```python
import contextvars
import logging

tracking_id_var = contextvars.ContextVar("trackingId", default=None)

def init(tracking_id: str | None) -> None:
    clear()
    if tracking_id and str(tracking_id).strip():
        tracking_id_var.set(str(tracking_id).strip())

def get() -> str | None:
    return tracking_id_var.get()

def clear() -> None:
    tracking_id_var.set(None)


class TrackingFilter(logging.Filter):
    def filter(self, record: logging.LogRecord) -> bool:
        value = tracking_id_var.get()
        record.trackingId = value if value else "null"
        return True
```

Attach `TrackingFilter` to the handler. Format:

```python
logging.basicConfig(
    format="%(asctime)s %(levelname)s %(app_name)s %(name)s %(trackingId)s %(message)s",
)
```

Set `app_name` from config (equivalent of `spring.application.name`).

FastAPI/Starlette middleware ≈ Java filter. Flask `before_request` / `teardown_request` ≈ filter try/finally.  
Kafka: clear at start of poll/batch; `init` at the start of each record.

---

## 6. Cases and scenarios

Legend: **Door** = filter / middleware / Kafka interceptor. **App** = controller / listener / your loop.

---

### Case A — Body `runNodeID` (default, ~70%)

**Setup:** header tracking **off**. Orchestrator JSON includes `runNodeID`.

**Java**

```properties
genesis.logging.header.enabled=false
```

```java
@PostMapping
public ResponseEntity<Void> create(@RequestBody NodeRequest request) {
    Tracking.init(request.getRunNodeID());
    log.info("CREATE received");
    return ResponseEntity.status(HttpStatus.CREATED).build();
}
```

**Python (FastAPI)**

```python
@app.post("/api/test")
def create(payload: dict):
    tracking.init(payload.get("runNodeID"))
    logging.getLogger("TestApi").info("CREATE received")
    return Response(status_code=201)
```

**Log**

```text
2026-09-24 23:21:00.123 INFO  order-service TestApiController node-1 CREATE received
```

HTTP response is empty (this sample). The sentence is **only** in logs.

GET/PUT/DELETE: same `init` + `GET received` / `UPDATE received` / `DELETE received`.

---

### Case B — Header allow-list (`X-Run-Node-Id` only)

**Setup:** this child is keyed by run node in a **header**. Correlation must **not** become `trackingId`.

**Java**

```properties
genesis.logging.header.enabled=true
genesis.logging.header.fields=X-Run-Node-Id
```

No `Tracking.init` in the controller if the header is enough.

**Python middleware**

```python
ALLOWED = ["X-Run-Node-Id"]  # not X-Correlation-Id

async def tracking_middleware(request, call_next):
    tracking.clear()
    for name in ALLOWED:
        value = request.headers.get(name)
        if value:
            tracking.init(value)
            break
    try:
        return await call_next(request)
    finally:
        tracking.clear()
```

**Incoming**

| Headers | `trackingId` |
|---------|----------------|
| `X-Run-Node-Id: node-1` | `node-1` |
| `X-Correlation-Id: corr-999` only | `null` (unless body `init` later) |
| Both | `node-1` (listed field only) |

A new service sending only correlation does **not** require a code change on this child. Tracking from correlation is **invalid** here until you **add** that name to the list on purpose.

---

### Case C — Header on + body `init` (override / fill)

Filter/middleware may set from header, then controller `init(runNodeID)` **replaces** it (`init` always clears then sets).

Use when this hop’s **configured** key is the body field, even if some callers also send headers.

```java
Tracking.init(request.getRunNodeID()); // wins over whatever the filter set
log.info("UPDATE received");
```

---

### Case D — Path-limited HTTP headers

**Java**

```properties
genesis.logging.header.enabled=true
genesis.logging.header.fields=X-Run-Node-Id
genesis.logging.header.path-patterns=/api/orders/**
```

`POST /api/orders/1` → header init.  
`POST /api/test` → filter does **not** read headers; use `Tracking.init` from body if needed.

Python: same with path prefix checks in middleware.

---

### Case E — Hybrid app (HTTP API + Kafka → same service)

Do **not** store tracking on the `@Service` / Python service object.

```text
HTTP request  → HTTP door (clear / header init) → Controller init(body) → OrderService.doWork()
Kafka record  → Kafka door (clear)              → Listener init(record) → OrderService.doWork()
```

Tomcat/uvicorn threads ≠ Kafka listener threads. Concurrent HTTP and Kafka do not overwrite each other if you use MDC / contextvars.

Each Kafka **record** gets its **own** `init`. Each HTTP **request** gets its **own**.

```java
// HTTP
Tracking.init(request.getRunNodeID());
orderService.doWork(request);

// Kafka record listener
@KafkaListener(topics = "orders")
public void onMessage(OrderEvent event) {
    Tracking.init(event.getRunNodeID());
    orderService.doWork(event);
}
```

```python
# HTTP
tracking.init(payload.get("runNodeID"))
order_service.do_work(payload)

# Kafka
tracking.init(event.get("runNodeID"))
order_service.do_work(event)
```

`OrderService` only logs. It does not know HTTP vs Kafka.

---

### Case F — Kafka **record** listener (no batching)

Spring: **`RecordInterceptor` only**. `BatchInterceptor` does **not** run. Not an error.

**Door:** clear; if `genesis.logging.kafka.header.enabled=true`, init from allow-listed **record** headers.

**App:** `Tracking.init` from payload if the id is only in JSON.

```properties
genesis.logging.kafka.header.enabled=true
genesis.logging.kafka.header.fields=X-Run-Node-Id
```

Python one-message callback: `clear()` at start (or `init` which clears), then `init(id)`, process. Next message calls `init` again.

---

### Case G — Kafka **batch** + process one-by-one + commit per record

Spring: **`BatchInterceptor` only**. `RecordInterceptor` does **not** run. You **cannot** put all clears only in the record interceptor.

```text
poll / batch     BatchInterceptor / Python poll start  → clear
  record 1       Tracking.init(id1)  → work → commit
  record 2       Tracking.init(id2)  → work → commit
next poll        clear again
```

**Java**

```java
@KafkaListener(topics = "orders", batch = "true")
public void onBatch(List<ConsumerRecord<String, OrderEvent>> records, Acknowledgment ack) {
    for (ConsumerRecord<String, OrderEvent> rec : records) {
        Tracking.init(rec.value().getRunNodeID()); // or listed header on rec
        orderService.doWork(rec.value());
        // commit this record
    }
}
```

Do **not** wrap work in `Tracking.run(() -> ...)`. One `init` at the top of each iteration.

**Python**

```python
records = consumer.poll(...)
tracking.clear()  # start of this poll
for rec in records:
    tracking.init(extract_id(rec))  # header allow-list or body runNodeID
    order_service.do_work(rec)
    consumer.commit()  # this record
```

If you skip `init` on record 2, logs for record 2 show **record 1’s** id (wrong Splunk hop).

---

### Case H — Exception / last record leftover

| Situation | What logs show | Impact |
|-----------|----------------|--------|
| Exception **during** this request/record after `init` | This id (good for the stacktrace) | Useful |
| Last Kafka record finished; next poll not yet | Last id until door `clear` / next `init` | Usually no extra lines |
| Next record **without** `init` | **Previous** id | **Bad** — wrong journey in Splunk |
| HTTP `/error` or logs **after** Java filter `finally` | `null` | Honest miss |
| Other microservice | Cannot see this MDC | No leak across apps |

Java HTTP filter **clears in `finally`** so the Tomcat thread is clean in the pool.  
Java Kafka interceptors clear at intercept start and after success/failure.  
Python should `clear()` in middleware `finally` and at the start of the next poll.

---

### Case I — Orchestrator sends the id every hop (main topology)

```text
orchestrator → order-service     (runNodeID=node-1) → init + log
orchestrator → inventory-service (runNodeID=node-1) → init + log
orchestrator → python-worker     (runNodeID=node-1) → init + log
```

Each service only reads **inbound**. Splunk: `trackingId=node-1`.

If the orchestrator **omits** the id on inventory, inventory logs `null`. Logging **cannot** fix that.

---

### Case J — Side path (no orchestrator)

Service A HTTP-calls B or publishes Kafka that B consumes. A must **put the same field** on that outbound call (header or body). B `init`s from inbound. If A does not send it, B’s logs are a broken chain.

This library **does not** auto-copy outbound (v1). Fetch/build is the caller’s (or orchestrator’s) job.

---

### Case K — Forgot `init` (header off)

Door cleared MDC/context. No header init. No `init` in handler.

```text
... INFO order-service TestApiController null GET received
```

Better than printing the **previous** customer’s id.

---

### Case L — Blank / missing id

`Tracking.init(null)` or `init("")` → `null` in the log. Same for Python.

---

### Case M — Method name in the pattern

Possible (`%M` / `funcName`). **Not recommended** for default INFO: stack walk on every log. Use logger/class column instead.

---

## 7. Decision cheat sheet

| Entry | Who clears | Who sets `trackingId` |
|-------|------------|------------------------|
| HTTP, header off, id in body | Filter / middleware | You: `init(runNodeID)` |
| HTTP, header on, listed header present | Filter inits | Optional later `init` if body is the real key |
| HTTP, header on, only unlisted header | Filter ignores it | `null` unless you `init` from body |
| Kafka record listener | `RecordInterceptor` | Header config and/or `init` in listener |
| Kafka batch listener | `BatchInterceptor` once per poll | **You:** `init` **every** record |
| Shared domain service | Nobody | Nobody — already on the thread/context |

---

## 8. Sample GET (this repo)

Request:

```bash
curl -X GET http://localhost:8080/api/test ^
  -H "Content-Type: application/json" ^
  -d "{\"runNodeID\":\"node-1\",\"correlationId\":\"corr-123\",\"address\":\"10.0.0.1\"}"
```

- **Response:** `200`, empty body  
- **Log:** `... INFO  spring-logging TestApiController node-1 GET received`

`spring-logging` comes from `spring.application.name` in `application.properties`.

---

## 9. Splunk

Search one hop family:

```text
trackingId=node-1
```

or the raw line substring. Sort by time, table service + class + message.

Until every hop uses the **same** kind of id (all run node, or all correlation), you may need **two** searches. Do not collapse both into one column unless this service was **configured** to use that key.

---

## 10. What not to do

- Sniff every tracking-like header  
- Parse JSON in the HTTP filter (slow, fragile)  
- `try/finally` in every controller  
- `private String trackingId` on a singleton service  
- Assume `RecordInterceptor` runs for batch listeners  
- Assume the HTTP filter runs for Kafka  
- Assume one Python `global` is request-safe  
- Expect the logging library to repair a missing orchestrator field
