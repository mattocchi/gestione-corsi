<#import "comune.ftlh" as c>
<!DOCTYPE html>
<html lang="it">
<head>
<meta charset="UTF-8"/>
<title>Riepilogo famiglia</title>
<@c.stile/>
<style>
  table.dati.famiglia { margin-top: 4mm; }
  table.elenco { table-layout: fixed; }
  table.elenco th.importo, table.elenco td.importo { width: 28mm; }
  table.elenco td.corso small { color: #555; }
  table.elenco tr.subtotale td { font-weight: bold; border-bottom: none; }
  .riquadro .totali { margin-top: 0; }
</style>
</head>
<body>
<@c.intestazione titolo="Riepilogo famiglia"/>
<table class="dati famiglia">
  <tr><td class="etichetta">Genitore</td><td>${genitore}</td></tr>
  <tr><td class="etichetta">Periodo</td><td>${anno}</td></tr>
</table>
<#if figli?has_content>
<#list figli as figlio>
<h2>${figlio.nome}</h2>
<table class="elenco">
  <tr><th>Corso</th><th class="importo">Quota</th><th class="importo">Pagato</th><th class="importo">Residuo</th></tr>
  <#list figlio.corsi as corso>
  <tr>
    <td class="corso">${corso.nome}<br/><small>${corso.insegnanti}<#if corso.nota?has_content> · ${corso.nota}</#if></small></td>
    <td class="importo">${corso.quota}</td><td class="importo">${corso.pagato}</td><td class="importo">${corso.residuo}</td>
  </tr>
  </#list>
  <#if (figlio.corsi?size > 1)>
  <tr class="subtotale"><td>Totale ${figlio.nome}</td><td class="importo">${figlio.quota}</td><td class="importo">${figlio.pagato}</td><td class="importo">${figlio.residuo}</td></tr>
  </#if>
</table>
</#list>
<div class="riquadro">
<table class="totali">
  <tr><td>Totale spesa della famiglia (${numeroCorsi} <#if numeroCorsi == 1>corso<#else>corsi</#if>)</td><td class="valore">${totali.quota}</td></tr>
  <tr><td>Totale pagato</td><td class="valore">${totali.pagato}</td></tr>
  <tr><td>Residuo da pagare</td><td class="valore">${totali.residuo}</td></tr>
</table>
</div>
<#else>
<p>Nessun corso dei figli in questo periodo.</p>
</#if>
</body>
</html>
