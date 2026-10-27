<#import "comune.ftlh" as c>
<#-- Dichiarazione delle spese per la detrazione (art. 15, c. 1, lett. e-quater TUIR), secondo il fac-simile 1.1 del
     vademecum del Tavolo Permanente delle Federazioni Bandistiche Italiane: per un anno solare, solo versamenti
     tracciabili. Fuori dalla fascia 5-18 anni (minore=false) il testo non cita la detrazione per i minori. -->
<!DOCTYPE html>
<html lang="it">
<head>
<meta charset="UTF-8"/>
<title>Ricevuta spese per corsi musicali ${anno}</title>
<@c.stile/>
<style>
  p.sottotitolo { margin: 0 0 3mm; color: #555; }
  table.spett { margin: 2mm 0 4mm; }
  p.testo { margin: 2.5mm 0; line-height: 1.45; }
  .riquadro .totali { margin-top: 0; }
  table.elenco { table-layout: fixed; margin-top: 1mm; font-size: 9pt; }
  table.elenco th.data, table.elenco td.data { width: 24mm; }
  table.elenco th.importo, table.elenco td.importo { width: 28mm; }
  table.firma { width: 100%; margin-top: 8mm; border-collapse: collapse; }
  table.firma td { vertical-align: top; width: 50%; }
  .linea { border-top: 1px solid #999; margin-top: 14mm; padding-top: 1mm; color: #555; font-size: 9pt; text-align: center; }
</style>
</head>
<body>
<@c.intestazione titolo="Ricevuta spese per corsi musicali"/>
<#if minore><p class="sottotitolo">per ragazzi di età compresa tra 5 e 18 anni (non ancora maggiorenni)</p></#if>

<table class="dati spett">
  <tr><td class="etichetta">Spett.</td><td>${intestatario.nome}</td></tr>
  <tr><td class="etichetta">Indirizzo</td><td>${intestatario.indirizzo}</td></tr>
  <tr><td class="etichetta">Codice fiscale</td><td>${intestatario.codiceFiscale}</td></tr>
</table>

<p class="testo">Con la presente si dichiara che <#if minore>il minore<#else>l'allievo</#if> ${allievo.nome},
  nato a ${allievo.luogoNascita} il ${allievo.nascita}, codice fiscale ${allievo.codiceFiscale},
  residente in ${allievo.comune}<#if allievo.provincia?has_content> (prov. ${allievo.provincia})</#if>, ${allievo.via}<#if allievo.cap?has_content>,
  CAP ${allievo.cap}</#if>, è iscritto e ha frequentato i Corsi di Musica organizzati dalla nostra associazione
  (${corsi}) nel periodo ${anno}.</p>

<p class="testo">I versamenti sono stati effettuati con modalità tracciabile per un importo complessivo di
  ${totale} (${totaleInLettere} euro).</p>

<#if minore>
<p class="testo">Si dichiara che l'Associazione è in possesso dei requisiti previsti dalla normativa vigente affinché
  l'intestatario della presente ricevuta possa usufruire delle detrazioni d'imposta del 19% per la frequenza ai corsi
  musicali di ragazzi minori di 18 anni di età.</p>
</#if>
<p class="testo">Si rilascia la presente per gli usi consentiti dalla legge.</p>

<table class="firma">
  <tr>
    <td>
      <#if associazione.luogo?has_content>${associazione.luogo}, </#if>${data}<br/><br/>
      Associazione ${associazione.nome}<br/>
      <#if associazione.indirizzo?has_content>${associazione.indirizzo}<br/></#if>
      <#if associazione.codiceFiscale?has_content>Codice fiscale/Partita IVA ${associazione.codiceFiscale}</#if>
    </td>
    <td><div class="linea">p. l'Associazione – timbro e firma</div></td>
  </tr>
</table>

<h2>Dettaglio dei versamenti tracciabili del ${anno}</h2>
<table class="elenco">
  <tr><th class="data">Data</th><th>Corso</th><th class="importo">Importo</th></tr>
  <#list pagamenti as pagamento>
  <tr><td class="data">${pagamento.data}</td><td>${pagamento.corso} – ${pagamento.metodo}</td><td class="importo">${pagamento.importo}</td></tr>
  </#list>
</table>
</body>
</html>
