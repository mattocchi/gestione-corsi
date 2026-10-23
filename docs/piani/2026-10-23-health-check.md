# Piano: endpoint di health check per il monitoraggio

## Richiesta

Un endpoint di health check della web application, per poterla monitorare.

## Analisi

- Il progetto non ha Spring Boot Actuator. Con `spring-boot-starter-actuator` l'endpoint `/actuator/health` risponde 200 `{"status":"UP"}` o 503 `{"status":"DOWN"}`, aggregando i controlli automatici (database, spazio su disco, ping).
- `SecurityConfig` usa `VaadinSecurityConfigurer`: gli indirizzi non di Vaadin vanno permessi esplicitamente (come `/styles.css`), altrimenti il monitor viene rimandato al login.
- `MailConfig` crea il `JavaMailSender` quando l'SMTP è configurato: Actuator aggiungerebbe un controllo del server email a ogni chiamata, e un SMTP lento o irraggiungibile renderebbe DOWN tutta l'applicazione → va disattivato (`management.health.mail.enabled=false`).
- Il server di produzione è gestito da pm2 (`deploy/ecosystem.config.js`, `deploy/README.md`).

### Decisioni

- Indirizzo `/actuator/health` (standard di Spring Boot), solo GET, senza login.
- Senza login solo lo stato (UP/DOWN); i dettagli (database, disco) solo agli utenti ADMIN collegati (`show-details=when-authorized`, `roles=ADMIN`).
- Nessun indirizzo separato di liveness/readiness; esposto solo l'endpoint `health`.
- Scelte tecniche: controllo dell'SMTP escluso dall'health check (sopra).

## Passi

| Passo | Cosa | File | Verifica | Stato |
|---|---|---|---|---|
| 1 | Actuator, configurazione dell'endpoint (solo `health`, dettagli agli admin, niente controllo SMTP), accesso libero in `SecurityConfig`; test con MockMvc (anonimo: 200 e solo lo stato; admin: dettagli; altri endpoint non esposti) | `pom.xml`, `application.properties`, `SecurityConfig`, `security/HealthCheckTest` | `./mvnw test`; `curl` sull'app avviata | [x] Spring Boot 4 attiva di default i gruppi liveness/readiness: disattivati (`probes.enabled=false`) come deciso |
| 2 | Documentazione: monitoraggio in `deploy/README.md` e `CLAUDE.md` | `deploy/README.md`, `CLAUDE.md` | rilettura | [x] |
| 3 | Registro finale | questo piano | build e test | [x] |

## Fuori ambito

- Liveness/readiness, metriche (`/actuator/metrics`, Prometheus) e configurazione dello strumento di monitoraggio esterno.

## Registro

- `./mvnw test`: tutti i test superati, compreso il nuovo `HealthCheckTest` (anonimo e segreteria solo `{"status":"UP"}`, admin con `db` e senza `mail`, `/actuator/env`, `/beans`, `/info` non esposti).
- `curl` sull'app in sviluppo (con la servlet di Vaadin): `/actuator/health` 200 `{"status":"UP"}` senza login; `/actuator`, `/actuator/env`, `/actuator/info` rimandano al login.
- Da fare sul server: build e deploy, poi configurare il monitor esterno sull'indirizzo pubblico (`https://<indirizzo-app>/actuator/health`). Nessuno script SQL.
- Lasciato aperto: nessuno.
