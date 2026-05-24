# YAS Service Mesh — Istio Setup Guide

Istio + Kiali on Minikube · namespace: `dev` · 12 active services

---

## Table of Contents

1. [Prerequisites](#1-prerequisites)
2. [Install Istio](#2-install-istio)
3. [Install Observability Add-ons](#3-install-observability-add-ons)
4. [Enable Sidecar Injection](#4-enable-sidecar-injection)
5. [Apply mTLS Configuration](#5-apply-mtls-configuration)
6. [Apply Authorization Policies](#6-apply-authorization-policies)
7. [Apply Retry & Timeout Policies](#7-apply-retry--timeout-policies)
8. [Verify the Setup](#8-verify-the-setup)
9. [Test Plan](#9-test-plan)
10. [Cleanup & Troubleshooting](#10-cleanup--troubleshooting)

---

## 1. Prerequisites

| Requirement | Version |
|---|---|
| Minikube | ≥ 1.32 |
| kubectl | ≥ 1.28 |
| istioctl | 1.21.x |
| Helm | ≥ 3.12 |
| Minikube resources | **≥ 6 vCPU, ≥ 12 GB RAM** |

Start Minikube with sufficient resources:

```bash
minikube start \
  --cpus=8 \
  --memory=14336 \
  --disk-size=60g \
  --driver=docker
```

---

## 2. Install Istio

Use the **demo** profile to include Prometheus, Jaeger, Kiali, and Grafana add-ons with minimal configuration.

```bash
# Download istioctl
curl -L https://istio.io/downloadIstio | ISTIO_VERSION=1.21.0 sh -
cd istio-1.21.0
export PATH=$PWD/bin:$PATH

# Install Istio with demo profile (disables egress gateway to save CPU)
istioctl install \
  --set profile=demo \
  --set components.egressGateways[0].enabled=false \
  -y

# Verify control plane is healthy
kubectl get pods -n istio-system
# Expected: istiod, istio-ingressgateway all Running
```

---

## 3. Install Observability Add-ons

Prometheus is required by Kiali for metrics. Manage both add-ons with Argo CD so they stay in sync with Git:

```bash
kubectl apply -f k8s/argocd/istio-addons.yaml

# Watch Argo CD create the add-on resources
kubectl get applications -n argocd istio-prometheus istio-kiali

# Wait for add-ons to be ready
kubectl rollout status deployment/prometheus -n istio-system
kubectl rollout status deployment/kiali -n istio-system
```

Open Kiali:

```bash
istioctl dashboard kiali
# or: kubectl port-forward svc/kiali -n istio-system 20001:20001
```

---

## 4. Enable Sidecar Injection

Label the `dev` namespace so Istio automatically injects Envoy sidecars into every new pod.

```bash
kubectl label namespace dev istio-injection=enabled --overwrite

# Restart all deployments to inject sidecars into existing pods
kubectl rollout restart deployment -n dev

# Verify sidecars are injected (each pod should show 2/2 READY)
kubectl get pods -n dev
```

### Infrastructure Namespaces

PostgreSQL, Kafka, and Keycloak do **not** have Istio sidecars. Do **not** label those namespaces:

```bash
# These namespaces should NOT be labeled:
#   postgres, kafka, keycloak, observability
```

---

## 5. Apply mTLS Configuration

Apply the files in order: infra destination rules first (so connections to postgres/kafka/keycloak keep working), then the mesh-wide mTLS rules, then PeerAuthentication STRICT.

```bash
# Step 1: Disable mTLS for infrastructure services (no sidecar)
kubectl apply -f k8s/istio/destination-rule-infra.yaml

# Step 2: Enforce ISTIO_MUTUAL for all intra-mesh calls + outlier detection
kubectl apply -f k8s/istio/destination-rule-mtls.yaml

# Step 3: Set PeerAuthentication to STRICT for dev namespace
#         (PERMISSIVE for infra namespaces and BFF ingress ports)
kubectl apply -f k8s/istio/peer-authentication.yaml

# Verify mTLS is active for intra-dev calls
# Pick any pod in dev and check TLS status
POD=$(kubectl get pod -n dev -l app.kubernetes.io/name=product -o jsonpath='{.items[0].metadata.name}')
istioctl authn tls-check ${POD}.dev
# Expected: MODE=STRICT for dev↔dev calls
# Expected: MODE=DISABLE (or not listed) for postgres/kafka/keycloak
```

---

## 6. Apply Authorization Policies

**Important**: Apply deny-all and allow-policies in the same `kubectl apply` invocation (or back-to-back) to minimise the outage window.

```bash
# Apply deny-all first, then allow policies immediately
kubectl apply -f k8s/istio/authz-deny-all.yaml
kubectl apply -f k8s/istio/authz-allow-policies.yaml

# Confirm policies are created
kubectl get authorizationpolicy -n dev
```

Expected output:

```
NAME                      AGE
deny-all                  5s
allow-to-storefront-bff   4s
allow-to-backoffice-bff   4s
allow-to-product          4s
allow-to-cart             4s
allow-to-customer         4s
allow-to-order            4s
allow-to-inventory        4s
allow-to-tax              4s
allow-to-search           4s
allow-to-media            4s
```

### Authorization Matrix

| Source | Destination | Methods | Paths |
|---|---|---|---|
| storefront-bff | product | GET | `/api/products*` |
| storefront-bff | cart | GET, POST | `/api/carts*` |
| storefront-bff | customer | GET | `/api/customers*` |
| storefront-bff | order | GET, POST | `/api/orders*` |
| storefront-bff | tax | GET | `/api/taxes*` |
| storefront-bff | search | GET | `/api/search*` |
| storefront-bff | media | GET | `*` |
| backoffice-bff | product | GET, PUT, POST, DELETE, PATCH | `*` |
| backoffice-bff | inventory | GET, PUT, POST, PATCH | `*` |
| backoffice-bff | order | GET, PUT, POST, PATCH | `*` |
| backoffice-bff | customer | GET, PUT, POST, DELETE, PATCH | `*` |
| backoffice-bff | media | GET, PUT, POST, DELETE, PATCH | `*` |
| backoffice-bff | tax | GET | `*` |
| order | cart | GET, DELETE | `*` |
| order | inventory | GET, PUT | `*` |
| order | tax | GET | `*` |
| order | product | GET | `*` |
| order | customer | GET | `*` |
| product | media | GET | `*` |
| product | tax | GET | `*` |
| product | search | GET | `*` |
| cart | product | GET | `*` |
| cart | tax | GET | `*` |
| inventory | product | GET | `*` |
| storefront-ui | storefront-bff | ALL | `*` |
| backoffice-ui | backoffice-bff | ALL | `*` |
| \* | payment | \* | **DENY** (skip-reconcile) |
| any other | any other | \* | **DENY** (default) |

---

## 7. Apply Retry & Timeout Policies

```bash
kubectl apply -f k8s/istio/virtual-services.yaml

# Verify VirtualServices are created
kubectl get virtualservice -n dev
```

Expected output:

```
NAME                 GATEWAYS   HOSTS              AGE
storefront-bff-vs               [storefront-bff]   5s
backoffice-bff-vs               [backoffice-bff]   5s
product-vs                      [product]          5s
cart-vs                         [cart]             5s
customer-vs                     [customer]         5s
order-vs                        [order]            5s
inventory-vs                    [inventory]        5s
tax-vs                          [tax]              5s
search-vs                       [search]           5s
media-vs                        [media]            5s
```

### Timeout Summary

| Service | Timeout | perTryTimeout | Reason |
|---|---|---|---|
| storefront-bff | 30s | 5s | Fan-out to 6+ backends |
| backoffice-bff | 30s | 5s | Fan-out to 6+ backends |
| order | 15s | 3s | Multi-step saga |
| product | 10s | 2s | DB + optional media/search calls |
| search | 10s | 2s | Elasticsearch query |
| media | 10s | 2s | File I/O |
| cart | 5s | 2s | Single-step |
| customer | 5s | 2s | Single-step |
| inventory | 5s | 2s | Single-step |
| tax | 5s | 2s | Single-step |

---

## 8. Verify the Setup

### Check sidecar injection

```bash
# All dev pods should show 2/2 READY (app container + Envoy sidecar)
kubectl get pods -n dev
```

### Check proxy status

```bash
# All pods should appear in SYNCED state
istioctl proxy-status
```

### Check mTLS status

```bash
POD=$(kubectl get pod -n dev -l app.kubernetes.io/name=product -o jsonpath='{.items[0].metadata.name}')
istioctl authn tls-check ${POD}.dev
```

---

## 9. Test Plan

### T1 — Verify mTLS (all dev↔dev calls must be STRICT)

**Prerequisites:** `istioctl` 1.21.x on your PATH (match the Istio control plane version).
If `istioctl` is missing or reports `unknown command "authn"`, use the full binary path
from the Istio bundle or skip to **Option B** below.

```bash
# Install istioctl once (Linux/macOS)
curl -L https://istio.io/downloadIstio | ISTIO_VERSION=1.21.0 sh -
export PATH=$HOME/istio-1.21.0/bin:$PATH
istioctl version
```

**Option A — `istioctl authn tls-check` (preferred when available)**

```bash
POD=$(kubectl get pod -n dev -l app.kubernetes.io/name=storefront-bff -o jsonpath='{.items[0].metadata.name}')
# Use the bundled binary if another istioctl is on PATH:
# ~/istio-1.21.0/bin/istioctl authn tls-check ${POD}.dev
istioctl authn tls-check ${POD}.dev
# Expected: STATUS=OK, MODE=STRICT for product.dev, cart.dev, etc.
```

**Option B — Without `authn tls-check` (policy + sidecar + proxy-config)**

```bash
# 1) Confirm mTLS policies are applied
kubectl get peerauthentication default -n dev -o jsonpath='{.spec.mtls.mode}{"\n"}'
# Expected: STRICT

kubectl get destinationrule mesh-wide-mtls -n dev -o jsonpath='{.spec.trafficPolicy.tls.mode}{"\n"}'
# Expected: ISTIO_MUTUAL

# 2) Confirm mesh pods have Envoy sidecars (2/2 READY)
kubectl get pods -n dev -l 'app.kubernetes.io/name in (storefront-bff,product,cart)'

# 3) Confirm workload certificates exist on the sidecar
POD=$(kubectl get pod -n dev -l app.kubernetes.io/name=storefront-bff -o jsonpath='{.items[0].metadata.name}')
istioctl proxy-config secret ${POD}.dev -n dev
# Expected: entries for the workload identity and ROOTCA

# 4) Optional: list outbound clusters to product
istioctl proxy-config cluster ${POD}.dev -n dev | grep product
```

If `istioctl` is not installed at all, Options B steps 1–2 plus a successful **T2** curl
through Envoy (`server: envoy`, no TLS handshake error) are sufficient evidence that
intra-mesh mTLS is working.

### T2 — Allowed path (expect HTTP 200)

```bash
STOREFRONT_POD=$(kubectl get pod -n dev -l app.kubernetes.io/name=storefront-bff -o jsonpath='{.items[0].metadata.name}')
kubectl exec -n dev ${STOREFRONT_POD} -- \
  curl -sv http://product/api/products
# Expected: HTTP/1.1 200
```

### T3 — Denied path (expect HTTP 403)

```bash
# media service is NOT allowed to call order
MEDIA_POD=$(kubectl get pod -n dev -l app.kubernetes.io/name=media -o jsonpath='{.items[0].metadata.name}')
kubectl exec -n dev ${MEDIA_POD} -- \
  curl -sv http://order/api/orders
# Expected: RBAC: access denied (HTTP 403)
```

### T4 — Retry policy with fault injection

```bash
# Inject 50% 500 errors into product
kubectl apply -f k8s/istio/fault-injection.yaml

# Call storefront-bff → product 20 times
STOREFRONT_POD=$(kubectl get pod -n dev -l app.kubernetes.io/name=storefront-bff -o jsonpath='{.items[0].metadata.name}')
for i in $(seq 1 20); do
  kubectl exec -n dev ${STOREFRONT_POD} -- \
    curl -s -o /dev/null -w "%{http_code}\n" http://product/api/products
done
# Expected: mostly 200 (retries rescue ~87% of requests at 50% fault rate)
# Watch retries in Kiali: Graph → Namespace: dev → Display: "Request Rate"

# Cleanup fault injection
kubectl delete -f k8s/istio/fault-injection.yaml
```

### T5 — Kiali topology graph

```bash
istioctl dashboard kiali
```

Navigate to: **Graph → Namespace: dev → Display: "Request Rate"**

Confirm arrows match the communication map. Take a screenshot for the project report.

---

## 10. Cleanup & Troubleshooting

### Remove all Istio policies

```bash
kubectl delete -f k8s/istio/authz-allow-policies.yaml
kubectl delete -f k8s/istio/authz-deny-all.yaml
kubectl delete -f k8s/istio/virtual-services.yaml
kubectl delete -f k8s/istio/destination-rule-mtls.yaml
kubectl delete -f k8s/istio/destination-rule-infra.yaml
kubectl delete -f k8s/istio/peer-authentication.yaml
```

### Common Issues

| Symptom | Root Cause | Fix |
|---|---|---|
| Pod shows `1/1` instead of `2/2` | Namespace not labeled or pods not restarted | `kubectl label ns dev istio-injection=enabled --overwrite && kubectl rollout restart deploy -n dev` |
| `upstream connect error` or `RBAC denied` after applying deny-all | Allow policies not applied yet | `kubectl apply -f k8s/istio/authz-allow-policies.yaml` |
| Services return 401 toward `identity.yas.local.com` | Keycloak has no sidecar; STRICT rejects plaintext outbound | `kubectl apply -f k8s/istio/destination-rule-infra.yaml` |
| Kiali shows blank graph | Prometheus not running or no traffic flowed yet | `kubectl get pods -n istio-system` — confirm prometheus Running; send test traffic first |
| Health checks fail with 403 after deny-all | Istio rewrites probes automatically in 1.9+; if probes still fail, enable commented policy in `authz-allow-policies.yaml` | Uncomment `allow-health-checks` policy |
| Cascading failures / too many retries | `retryOn` conditions do not match error type | Use `retryOn: "5xx,gateway-error,connect-failure,retriable-4xx"` (already set) |

### Nginx Ingress + Istio Compatibility

The project uses `nginx` as the ingress class. Because nginx-ingress pods are in the `ingress-nginx` namespace and do **not** have Istio sidecars, they send plain HTTP to services.

`peer-authentication.yaml` already handles this with `portLevelMtls: 80: mode: PERMISSIVE` on the BFF and UI services. If you encounter issues:

```bash
# Option A: inject sidecars into ingress-nginx namespace
kubectl label namespace ingress-nginx istio-injection=enabled
kubectl rollout restart deployment -n ingress-nginx

# Option B: apply per-service PERMISSIVE mode (already done for BFFs via peer-authentication.yaml)
```

---

## File Reference

| File | Purpose |
|---|---|
| `peer-authentication.yaml` | mTLS STRICT for `dev`; PERMISSIVE for infra namespaces & BFF ingress ports |
| `destination-rule-infra.yaml` | Disable mTLS outbound to postgres, kafka, keycloak, otel-collector |
| `destination-rule-mtls.yaml` | Force ISTIO_MUTUAL for all `*.dev.svc.cluster.local` + outlier detection |
| `authz-deny-all.yaml` | Default deny-all AuthorizationPolicy for `dev` namespace |
| `authz-allow-policies.yaml` | Per-service allow rules based on communication map |
| `virtual-services.yaml` | Retry (3x, 2s) + timeout config for all active services |
| `fault-injection.yaml` | **Test-only** — 50% 500 faults into product; delete after T4 test |
| `../argocd/istio-addons.yaml` | Argo CD Applications for Istio Prometheus and Kiali add-ons |
