<#import "comune.ftlh" as c>
<#-- Riepilogo dei corsi attivi (admin e segreteria): le sezioni della dashboard, poi gli iscritti di ogni corso -->
<#macro riquadri elenco>
<table class="riquadri">
  <#list elenco?chunk(4) as fila>
  <tr>
    <#list fila as r>
    <td class="riquadro-dato">
      <span class="etichetta">${r.etichetta}</span><br/>
      <span class="valore">${r.valore}</span>
      <#if r.dettaglio?has_content><br/><span class="dettaglio">${r.dettaglio}</span></#if>
    </td>
    </#list>
    <#list fila?size..<4 as vuoto><td class="vuoto"></td></#list>
  </tr>
  </#list>
</table>
</#macro>
<!DOCTYPE html>
<html lang="it">
<head>
<meta charset="UTF-8"/>
<title>${titolo}</title>
<@c.stile/>
<style>
  @page {
    size: A4 landscape; margin: 10mm 12mm 14mm;
    @bottom-right { content: "Pagina " counter(page) " di " counter(pages); font-family: Helvetica, Arial, sans-serif; font-size: 8.5pt; color: #555; }
  }
  table.riquadri { border-collapse: separate; border-spacing: 2mm; width: 100%; table-layout: fixed; margin: 0 -2mm; }
  td.riquadro-dato { border: 1px solid #ccc; padding: 2mm 3mm; vertical-align: top; }
  td.riquadro-dato .etichetta { color: #555; font-size: 9pt; }
  td.riquadro-dato .valore { font-size: 15pt; }
  td.riquadro-dato .dettaglio { color: #555; font-size: 8.5pt; }
  table.elenco { margin-top: 2mm; -fs-table-paginate: paginate; }
  table.elenco th { color: #555; font-weight: normal; font-size: 9pt; }
  table.elenco tr.totale td { background: #eeeeee; border-bottom: none; }
  table.elenco td.numero, table.elenco th.numero { text-align: right; width: 18mm; }
  table.elenco tr { page-break-inside: avoid; }
  .esito { font-size: 8.5pt; padding: 0.3mm 1.5mm; }
  .attivo { background: #dff0d8; }
  .perdita { background: #f8d7da; }
  /* Un corso che sta in una pagina non si spezza (i più lunghi sì, con l'intestazione ripetuta) */
  .corso { margin-top: 5mm; page-break-inside: avoid; }
  h3 { font-size: 11.5pt; margin: 0; padding: 1.5mm 2mm; background: #eeeeee; }
  .corso .dettaglio { color: #555; font-size: 9pt; margin: 1mm 0 0; }
  td.gruppo { background: #f6f6f6; color: #333; font-size: 9.5pt; }
  td.gruppo small { color: #555; }
  .nessuno { color: #555; }
  .insieme { page-break-inside: avoid; }
  /* Stesse colonne per tutti i corsi */
  table.iscritti { table-layout: fixed; }
  table.iscritti th.numero, table.iscritti td.numero { width: 10mm; }
  table.iscritti th.orario { width: 18mm; }
  table.iscritti th.importo { width: 28mm; }
  table.iscritti th.stato { width: 26mm; padding-left: 5mm; }
  table.iscritti td.stato { padding-left: 5mm; }
</style>
</head>
<body>
<@c.intestazione titolo=titolo/>

<h2>Pagamenti dei corsi attivi</h2>
<@riquadri elenco=pagamenti/>
<#if pagamentiCorsi?has_content>
<table class="elenco">
  <thead>
  <tr><th>Corso</th><th class="numero">Saldati</th><th class="numero">Parziali</th><th class="numero">Senza pagamenti</th>
    <th class="importo">Incassato</th><th class="importo">Da incassare</th><th class="importo">Costo insegnanti</th><th class="importo">Margine</th></tr>
  </thead>
  <tbody>
  <#list pagamentiCorsi as r>
  <tr>
    <td>${r.corso}</td><td class="numero">${r.saldati}</td><td class="numero">${r.parziali}</td><td class="numero">${r.senzaPagamenti}</td>
    <td class="importo">${r.incassato}</td><td class="importo">${r.daIncassare}</td><td class="importo">${r.costo}</td>
    <td class="importo">${r.margine}<#if r.esito?has_content> <span class="esito ${(r.esito == "Attivo")?then("attivo", "perdita")}">${r.esito}</span></#if></td>
  </tr>
  </#list>
  <tr class="totale">
    <td>Totale</td><td class="numero">${pagamentiTotale.saldati}</td><td class="numero">${pagamentiTotale.parziali}</td><td class="numero">${pagamentiTotale.senzaPagamenti}</td>
    <td class="importo">${pagamentiTotale.incassato}</td><td class="importo">${pagamentiTotale.daIncassare}</td>
    <td class="importo">${pagamentiTotale.costo}</td><td class="importo">${pagamentiTotale.margine}</td>
  </tr>
  </tbody>
</table>
<#else>
<p class="nessuno">Nessun corso attivo.</p>
</#if>

<div class="insieme">
<h2>Gruppi dei corsi attivi</h2>
<#if gruppi?has_content>
<@riquadri elenco=gruppiRiquadri/>
<table class="elenco">
  <thead><tr><th>Corso</th><th>Gruppo</th><th class="numero">Iscritti</th></tr></thead>
  <tbody>
  <#list gruppi as g>
  <tr><td>${g.corso}</td><td>${g.gruppo}</td><td class="numero">${g.iscritti}</td></tr>
  </#list>
  </tbody>
</table>
<#else>
<p class="nessuno">Nessun corso attivo con gruppi.</p>
</#if>
</div>

<#if corsi?has_content>
<h2>Iscritti dei corsi attivi</h2>
<#list corsi as corso>
<div class="corso">
  <h3>${corso.nome} · ${corso.numeroIscritti}</h3>
  <p class="dettaglio">${corso.dettaglio}</p>
  <table class="elenco iscritti">
    <thead>
    <tr><th class="numero">N.</th><th>Allievo</th><th>Genitore</th><th class="orario">Orario</th><th class="importo">Quota</th>
      <th class="importo">Pagato</th><th class="importo">Residuo</th><th class="stato">Stato</th></tr>
    </thead>
    <tbody>
    <#list corso.sezioni as sezione>
    <#if sezione.gruppo?has_content>
    <tr><td class="gruppo" colspan="8">${sezione.gruppo} <small>· ${sezione.numeroIscritti}</small></td></tr>
    </#if>
    <#list sezione.iscritti as i>
    <tr>
      <td class="numero">${i.numero}</td><td>${i.allievo}</td><td>${i.genitore}</td><td>${i.orario}</td>
      <td class="importo">${i.quota}</td><td class="importo">${i.pagato}</td><td class="importo">${i.residuo}</td><td class="stato">${i.stato}</td>
    </tr>
    </#list>
    </#list>
    <#if corso.sezioni?has_content>
    <tr class="totale">
      <td></td><td colspan="3">Totale del corso · saldati ${corso.totali.saldati}</td>
      <td class="importo">${corso.totali.quota}</td><td class="importo">${corso.totali.pagato}</td><td class="importo">${corso.totali.residuo}</td><td></td>
    </tr>
    <#else>
    <tr><td></td><td class="nessuno" colspan="7">Nessun iscritto.</td></tr>
    </#if>
    </tbody>
  </table>
</div>
</#list>
</#if>
</body>
</html>
