<#import "comune.ftlh" as c>
<!DOCTYPE html>
<html lang="it">
<head>
<meta charset="UTF-8"/>
<title>Conferma di iscrizione</title>
<@c.stile/>
</head>
<body>
<@c.intestazione titolo="Conferma di iscrizione"/>
<p>Confermiamo l'iscrizione di <strong>${allievo.nome}</strong> al corso <strong>${corso.nome}</strong>
  per l'anno scolastico ${corso.anno}.</p>
<@c.datiIscrizione/>
<h2>Importo</h2>
<table class="totali">
  <tr><td>Quota del corso</td><td class="valore">${totali.quota}</td></tr>
  <tr><td>Già pagato</td><td class="valore">${totali.pagato}</td></tr>
  <tr><td>Da pagare</td><td class="valore">${totali.residuo}</td></tr>
</table>
<#if associazione.iban?has_content>
<div class="riquadro">
  Il pagamento può essere effettuato con bonifico intestato a <strong>${associazione.nome}</strong>,
  IBAN <strong>${associazione.iban}</strong>, indicando nella causale
  &quot;${corso.nome} ${corso.anno} – ${allievo.nome}&quot;.
</div>
</#if>
</body>
</html>
