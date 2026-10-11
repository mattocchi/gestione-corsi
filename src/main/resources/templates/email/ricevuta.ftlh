<!DOCTYPE html>
<html lang="it">
<head><meta charset="UTF-8"/></head>
<body style="font-family: Helvetica, Arial, sans-serif; font-size: 14px; color: #222;">
<p>Gentile ${saluto},</p>
<p>in allegato la ricevuta dei pagamenti per il corso <strong>${corso.nome}</strong> (anno scolastico ${corso.anno})
  di <strong>${allievo.nome}</strong>.</p>
<table style="border-collapse: collapse;">
  <tr><td style="padding: 2px 16px 2px 0;">Totale pagato</td><td style="text-align: right;"><strong>${totali.pagato}</strong></td></tr>
  <tr><td style="padding: 2px 16px 2px 0;">Residuo da pagare</td><td style="text-align: right;">${totali.residuo}</td></tr>
</table>
<p>Cordiali saluti,<br/>${associazione.nome}</p>
</body>
</html>
