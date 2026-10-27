# Piano: data di release nel menu laterale

## Richiesta

[F-2026-10-27/VERSIONE.1] In fondo al menu laterale, sopra o sotto il logo, la data di release del software, presa se possibile dalla data del JAR.

## Analisi

- Il fondo del drawer è costruito in `MainLayout` (menu, "Aggiungi alla Home", logo).
- La data del file JAR cambia con copie e deploy (rsync); il goal `build-info` di `spring-boot-maven-plugin` scrive nel JAR `META-INF/build-info.properties` (versione, data e ora della build), letto da Spring Boot come bean `BuildProperties`.

### Decisioni

- [F-2026-10-27/VERSIONE.1] Data della build scritta nel JAR (`build-info`), non la data del file.
- [F-2026-10-27/VERSIONE.1] Sotto il logo, piccolo e grigio: "Versione 1.1.0 · 27/10/2026".
- [F-2026-10-27/VERSIONE.1] Visibile solo ad admin e segreteria.
- Scelta tecnica: senza `build-info` (es. avvio dall'IDE) la riga mostra "Versione di sviluppo".

## Passi

| Passo | Cosa | File | Richiesta | Verifica | Stato |
|---|---|---|---|---|---|
| 1 | Goal `build-info` nel pom; riga sotto il logo per admin e segreteria; test; CLAUDE.md; registro | `pom.xml`, `MainLayout`, test, `CLAUDE.md`, questo piano | VERSIONE.1 | `./mvnw test`; JAR | [x] |

## Fuori ambito

- Numero di versione automatico (resta quello del pom).

## Registro

- `./mvnw test`: 328 test superati. Nuovi: `ViewAccessTest.admin_vedeVersioneEDataDellaBuildSottoIlLogo` (riga "Versione x · gg/mm/aaaa" nel drawer, "Versione di sviluppo" senza build-info) e `genitore_nonVedeLaVersione`.
- `target/classes/META-INF/build-info.properties` generato dalla compilazione (versione 1.1.0, data della build): entra nel JAR di `./build.sh`. JAR di produzione non costruito in questa sessione.
- Non verificato nel browser.
- Lasciato aperto: nessuno.

