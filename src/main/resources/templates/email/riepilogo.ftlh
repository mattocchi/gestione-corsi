<!DOCTYPE html>
<html lang="it">
<head><meta charset="UTF-8"/></head>
<body style="font-family: Helvetica, Arial, sans-serif; font-size: 14px; color: #222;">
<p>Gentile ${saluto},</p>
<p>in allegato il riepilogo dei pagamenti ${anno}<#if famiglia> per i corsi dei tuoi figli<#else> per i tuoi corsi</#if>
  (${numeroCorsi} <#if numeroCorsi == 1>corso<#else>corsi</#if>).</p>
<table style="border-collapse: collapse; margin: 12px 0;">
  <tr><td style="padding: 4px 16px 4px 0;">Quota dovuta</td><td style="padding: 4px 0; text-align: right;">${totali.quota}</td></tr>
  <tr><td style="padding: 4px 16px 4px 0;">Già pagato</td><td style="padding: 4px 0; text-align: right;">${totali.pagato}</td></tr>
  <tr><td style="padding: 4px 16px 4px 0; font-weight: bold;">Da pagare</td><td style="padding: 4px 0; text-align: right; font-weight: bold;">${totali.residuo}</td></tr>
</table>
<p>Cordiali saluti,<br/>${associazione.nome}</p>
</body>
</html>
