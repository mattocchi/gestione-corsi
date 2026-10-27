<#import "piede.ftlh" as p>
<!DOCTYPE html>
<html lang="it">
<head><meta charset="UTF-8"/></head>
<body style="font-family: Helvetica, Arial, sans-serif; font-size: 14px; color: #222;">
<p>Gentile ${saluto},</p>
<p>in allegato l'attestazione delle spese sostenute nell'anno ${anno}<#if famiglia> per ${allievo}</#if>
  (${numeroPagamenti} <#if numeroPagamenti == 1>pagamento<#else>pagamenti</#if>).</p>
<table style="border-collapse: collapse; margin: 12px 0;">
  <tr><td style="padding: 4px 16px 4px 0; font-weight: bold;">Totale pagato nel ${anno}</td><td style="padding: 4px 0; text-align: right; font-weight: bold;">${totale}</td></tr>
</table>
<p>Cordiali saluti,<br/>${associazione.nome}</p>
<@p.areaCorsi/>
</body>
</html>
