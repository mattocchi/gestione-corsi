# Riepilogo famiglia: anno scolastico sotto il corso

## Richiesta

"nel riepilogo per famiglia, sotto il nome del corso anziche indicare gli insegnanti indica l'anno scolastico" —
[F-2026-10-27/RIEPILOGO.1]

## Analisi

- `templates/documenti/riepilogo-famiglia.ftlh`: sotto il nome del corso `corso.insegnanti` e la nota della quota;
  il modello della riga è `DocumentiService.rigaRiepilogo`, usato anche dal riepilogo dell'allievo adulto.
- L'email del riepilogo non elenca i corsi.

### Decisioni

- [F-2026-10-27/RIEPILOGO.1] "Anno scolastico 2024/2025" al posto degli insegnanti, nulla per i corsi senza anno;
  la nota della quota resta dopo " · ". Stesso cambio nel riepilogo dell'allievo adulto (stesso modello).

## Passi

| Passo | Cosa | File | Verifica | Stato | Richiesta |
|---|---|---|---|---|---|
| 1 | `rigaRiepilogo` con l'anno al posto degli insegnanti; template; test; CLAUDE.md | `DocumentiService`, `riepilogo-famiglia.ftlh`, `DocumentiServiceTest` | `./mvnw test` | [x] | F-2026-10-27/RIEPILOGO.1 |

## Fuori ambito

- Nessuno.

## Registro

- Passo 1: `rigaRiepilogo` ha la chiave `anno` ("Anno scolastico …") al posto di `insegnanti`; il template la mostra con la nota della quota. Test del riepilogo esteso (anno sotto i due corsi, insegnanti assenti). `./mvnw test`: 331 test, tutti verdi. PDF non controllato a vista.
