<#import "comune.ftlh" as c>
<!DOCTYPE html>
<html lang="it">
<head>
<meta charset="UTF-8"/>
<title>Ricevuta di pagamento</title>
<@c.stile/>
</head>
<body>
<@c.intestazione titolo="Ricevuta di pagamento"/>
<@c.datiIscrizione/>
<h2>Pagamenti</h2>
<#if pagamenti?has_content>
<table class="elenco">
  <tr><th>Data</th><th class="importo">Importo</th><th>Metodo</th><th>Nota</th></tr>
  <#list pagamenti as p>
  <tr><td>${p.data}</td><td class="importo">${p.importo}</td><td>${p.metodo}</td><td>${p.nota}</td></tr>
  </#list>
</table>
<#else>
<p>Nessun pagamento registrato.</p>
</#if>
<table class="totali">
  <tr><td>Totale pagato</td><td class="valore">${totali.pagato}</td></tr>
  <tr><td>Residuo da pagare</td><td class="valore">${totali.residuo}</td></tr>
</table>
</body>
</html>
