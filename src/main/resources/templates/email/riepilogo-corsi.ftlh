<!DOCTYPE html>
<html lang="it">
<head><meta charset="UTF-8"/></head>
<body style="font-family: Helvetica, Arial, sans-serif; font-size: 14px; color: #222;">
<p>Gentile ${saluto},</p>
<p>in allegato il riepilogo dei corsi attivi al ${data}, con i pagamenti, i gruppi e gli iscritti di ogni corso.</p>
<table style="border-collapse: collapse; margin: 12px 0;">
  <tr><td style="padding: 4px 16px 4px 0;">Corsi attivi</td><td style="padding: 4px 0; text-align: right;">${corsi}</td></tr>
  <tr><td style="padding: 4px 16px 4px 0;">Iscritti ai corsi attivi</td><td style="padding: 4px 0; text-align: right;">${iscritti}</td></tr>
  <tr><td style="padding: 4px 16px 4px 0;">Incassato</td><td style="padding: 4px 0; text-align: right;">${incassato}</td></tr>
  <tr><td style="padding: 4px 16px 4px 0;">Da incassare</td><td style="padding: 4px 0; text-align: right;">${daIncassare}</td></tr>
</table>
<p>${associazione.nome}</p>
</body>
</html>
