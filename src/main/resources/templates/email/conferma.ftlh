<!DOCTYPE html>
<html lang="it">
<head><meta charset="UTF-8"/></head>
<body style="font-family: Helvetica, Arial, sans-serif; font-size: 14px; color: #222;">
<p>Gentile ${saluto},</p>
<p>confermiamo l'iscrizione di <strong>${allievo.nome}</strong> al corso <strong>${corso.nome}</strong>
  (anno scolastico ${corso.anno}<#if corso.insegnante?has_content>, ${corso.etichettaInsegnante?lower_case} ${corso.insegnante}</#if>).</p>
<table style="border-collapse: collapse;">
  <tr><td style="padding: 2px 16px 2px 0;">Quota del corso</td><td style="text-align: right;"><strong>${totali.quota}</strong></td></tr>
  <tr><td style="padding: 2px 16px 2px 0;">Già pagato</td><td style="text-align: right;">${totali.pagato}</td></tr>
  <tr><td style="padding: 2px 16px 2px 0;">Da pagare</td><td style="text-align: right;"><strong>${totali.residuo}</strong></td></tr>
</table>
<#if associazione.iban?has_content>
<p>Il pagamento può essere effettuato con bonifico intestato a ${associazione.nome}, IBAN <strong>${associazione.iban}</strong>,
  indicando nella causale &quot;${corso.nome} ${corso.anno} – ${allievo.nome}&quot;.</p>
</#if>
<p>In allegato la conferma di iscrizione in PDF.</p>
<p>Cordiali saluti,<br/>${associazione.nome}</p>
</body>
</html>
