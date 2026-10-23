<#import "comune.ftlh" as c>
<#import "/importi.ftlh" as i>
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
<h2>Importi</h2>
<@i.blocco/>
</body>
</html>
