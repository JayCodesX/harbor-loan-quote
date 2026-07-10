# Deploy Harbor to Oracle Cloud (Always Free ARM) via Cloudflare Tunnel

Full-stack deployment on one Oracle Cloud Ampere A1 instance, exposed with a
Cloudflare Tunnel (outbound-only — no inbound ports, the OCI security list stays
closed). See ADR-0055 for why this topology.

```
Internet ──TLS──► Cloudflare edge ──tunnel (outbound from the VM)──► cloudflared
   oraroute.com ─────────────────────────────────► edge-tunnel (nginx :80) ─► web / api / admin
   pricing.oraroute.com ──────────────────────────► pricing-service:8084 (MCP)
```

**What you need:** an Oracle Cloud account, a domain on Cloudflare (`oraroute.com`),
and an SSH keypair. Steps marked **[you]** need your console/browser; the rest is
copy-paste on the instance.

---

## 1. Provision the instance **[you]**

Oracle Console → Compute → Instances → Create:
- **Image:** Canonical Ubuntu 22.04 (or 24.04).
- **Shape:** `VM.Standard.A1.Flex` (Ampere ARM). Set **2 OCPU / 12 GB** — the current
  Always Free ceiling (it was 4/24 before June 2026). This fits the full stack (~5 GB
  of containers).
- **SSH key:** upload your public key.
- **Boot volume:** 50 GB is plenty.

> **ARM capacity lottery:** "Out of host capacity" is common in busy regions. If it
> fails, try a different **Availability Domain**, retry over a few hours, or pick a
> less-busy home region (Frankfurt / Singapore tend to have ARM). Converting to
> **Pay-As-You-Go** (step 2) also improves capacity access and, importantly, stops
> the idle-reclamation that deletes Always-Free VMs.

Note the instance's **public IP**.

## 2. (Recommended) Convert to Pay-As-You-Go **[you]**

Console → Billing → Upgrade to Paid. You stay within the Always-Free resource limits
(no charge for the A1 free allocation), but you avoid: (a) idle VMs being reclaimed,
and (b) capacity denials. This is the single biggest reliability win for a demo you
want to stay up.

## 3. Base setup on the instance

SSH in (`ssh ubuntu@<public-ip>`), then:

```bash
sudo apt-get update && sudo apt-get install -y docker.io docker-compose-plugin git
sudo usermod -aG docker ubuntu && newgrp docker      # run docker without sudo
git clone https://github.com/JayCodesX/harbor-loan-quote.git
cd harbor-loan-quote
```

## 4. Secrets

```bash
cp .env.prod.example .env.prod
# Generate the RS256 keypair + HMAC secrets per the comments at the top of the file,
# and set HARBOR_MCP_API_KEYS to a fresh key:  openssl rand -hex 24
nano .env.prod            # fill in every CHANGE_ME
```

Keep `.env.prod` on the box only — it is gitignored and must never be committed.

## 5. Cloudflare Tunnel

Add `oraroute.com` to Cloudflare (Console → add site; update your registrar's
nameservers) if you haven't. Then, on the instance:

```bash
# install cloudflared
curl -L https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-arm64 \
  -o cloudflared && chmod +x cloudflared && sudo mv cloudflared /usr/local/bin/

cloudflared tunnel login                     # opens a URL; authorize in your browser
cloudflared tunnel create harbor             # prints a TUNNEL_ID and writes ~/.cloudflared/<id>.json
cloudflared tunnel route dns harbor oraroute.com
cloudflared tunnel route dns harbor www.oraroute.com
cloudflared tunnel route dns harbor pricing.oraroute.com

# wire the config the compose expects
cp cloudflared/config.example.yml cloudflared/config.yml
sed -i "s/REPLACE_WITH_TUNNEL_ID/<TUNNEL_ID>/" cloudflared/config.yml
cp ~/.cloudflared/<TUNNEL_ID>.json cloudflared/credentials.json
```

## 6. Deploy

```bash
docker compose --env-file .env.prod \
  -f docker-compose.yml -f docker-compose.prod.yml \
  up -d --build \
  mysql redis rabbitmq redpanda otel-lgtm \
  pricing-service notification-service api web admin-web edge-tunnel cloudflared
```

The explicit service list omits the dev `edge` (self-signed) and `keycloak` on
purpose. First build takes a few minutes (Maven builds three services). Watch it:

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml ps
docker compose -f docker-compose.yml -f docker-compose.prod.yml logs -f cloudflared
```

## 7. Verify

```bash
# App, through the tunnel:
curl -sI https://oraroute.com | head -1                      # 200/301 from the web app
curl -s https://oraroute.com/actuator/health                 # {"status":"UP"}

# MCP server, its own subdomain:
curl -s -o /dev/null -w "%{http_code}\n" https://pricing.oraroute.com/sse          # 401 (no key) = gateway working
curl -s -o /dev/null -w "%{http_code}\n" -H "X-API-Key: <your key>" https://pricing.oraroute.com/sse   # 200
```

Then point the **agent demo** at the deployed MCP server — a real LLM calling your
engine over the public internet:

```bash
cd mcp-client-demo
MCP_BASE_URL=https://pricing.oraroute.com MCP_API_KEY=<your key> \
  python3 harbor_agent_demo.py
```

Grafana (traces) is intentionally **not** exposed on a public hostname. Reach it over
an SSH tunnel: `ssh -L 3000:localhost:3000 ubuntu@<public-ip>` then open
`http://localhost:3000`. (To expose it publicly, uncomment the `grafana.oraroute.com`
ingress and put it behind Cloudflare Access first.)

---

## Operations

| Task | Command |
|---|---|
| Logs | `docker compose -f docker-compose.yml -f docker-compose.prod.yml logs -f <svc>` |
| Restart a service | `... restart pricing-service` |
| Update to latest | `git pull` then re-run the step-6 `up -d --build` |
| Stop everything | `... down` (keeps volumes) |
| DB backup | `docker exec mortgage-loan-mysql mysqldump -uroot -p... --all-databases > backup.sql` |

## Gotchas (verified)

- **No inbound ports needed.** The tunnel is outbound, so the OCI security list can
  stay at its default (only SSH/22). Don't open 80/443 — you don't need to.
- **Rotate the MCP key** by adding a second value to `HARBOR_MCP_API_KEYS`
  (comma-separated), redeploying, moving clients over, then removing the old one.
- **ARM shapes are ARM64** — every image here is multi-arch (mysql, redis,
  redpanda, nginx, cloudflared, otel-lgtm, and the Temurin JRE base), so builds work
  natively on Ampere.
- **Free-tier changes silently.** Oracle has trimmed Always-Free before; PAYG (step 2)
  insulates you from reclamation and most capacity denials.
