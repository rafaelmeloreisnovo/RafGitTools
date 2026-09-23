# Manifold Route Binding V1 — ContextBroker bridge

State: `IMPLEMENTED_SOURCE / CI_PENDING`  
Parent: `native/rafcode_route_v1`  
Authority: `Mapa:data/manifold/routes_omega_v1.jsonl`

## Boundary

```text
text/user intent
→ higher-layer trigger classification
→ rafcode_route_v1 receipt
→ ManifoldRouteBinding
→ ContextBundle V2 annotations
→ WorkspaceSession / provider action
```

The Kotlin bridge **does not execute or emulate** the freestanding resolver. It carries an already resolved V1 result into the bundle with explicit authority and resolver identity.

Reserved annotations:

- `manifold_route_id`
- `manifold_trigger_code`
- `manifold_route_code`
- `manifold_route_tag`
- `manifold_route_authority`
- `manifold_route_resolver`
- `manifold_route_state`

Callers cannot override those keys through generic annotations. That avoids a higher layer spoofing a low-level route receipt.

## V1 invariant

`routeCode == triggerCode` is enforced because ROUTES_OMEGA_V1 currently binds trigger classes 1..10 to R0001..R0010 one-to-one. A changed mapping requires a new contract version.

## Non-goals

- no automatic source ingestion;
- no filesystem/network access;
- no route selection from free text;
- no write authorization;
- no claim promotion;
- no JNI binding yet.

`ROUTE_RESOLVED != SOURCE_READ != EXECUTION != EVIDENCE != CLAIM`.
