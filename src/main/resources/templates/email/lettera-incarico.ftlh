<!DOCTYPE html>
<html lang="it">
<head><meta charset="UTF-8"/></head>
<body style="font-family: Helvetica, Arial, sans-serif; font-size: 14px; color: #222;">
<p>Gentile ${saluto},</p>
<p>in allegato la lettera di incarico per il ${tipo} <strong>${corso}</strong><#if anno?has_content> dell'anno scolastico ${anno}</#if>.</p>
<table style="border-collapse: collapse; margin: 12px 0;">
  <#if giorniEOrari?has_content><tr><td style="padding: 4px 16px 4px 0;">Giorni e orari</td><td style="padding: 4px 0;">${giorniEOrari}</td></tr></#if>
  <#if tariffaOraria?has_content><tr><td style="padding: 4px 16px 4px 0;">Tariffa oraria</td><td style="padding: 4px 0;">${tariffaOraria}</td></tr></#if>
  <#if unaTantum?has_content><tr><td style="padding: 4px 16px 4px 0;">Una tantum</td><td style="padding: 4px 0;">${unaTantum}</td></tr></#if>
</table>
<p>Ti chiediamo di restituirla firmata per accettazione.</p>
<p>Cordiali saluti,<br/>${associazione.nome}</p>
</body>
</html>
