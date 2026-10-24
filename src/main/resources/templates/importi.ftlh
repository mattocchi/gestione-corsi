<#-- Importi di conferma e ricevuta, uguali nell'email e nel PDF: con uno sconto (o maggiorazione) il prezzo di
     listino barrato, la variazione con il motivo e la quota dovuta in evidenza.
     XHTML ben formato (lo usa anche OpenHTMLtoPDF); con email=true gli stili sono in linea per i client di posta. -->
<#macro blocco email=false>
<#if email>
<#assign cella = "padding: 2px 16px 2px 0;">
<table style="border-collapse: collapse;">
  <#if importi.conVariazione>
  <tr><td style="${cella} color: #555;">Prezzo di listino del corso</td><td style="text-align: right; color: #555;"><s>${importi.listino}</s></td></tr>
  <tr><td style="${cella} color: #555;">${importi.etichettaVariazione}</td><td style="text-align: right; color: #555;">${importi.variazione}</td></tr>
  </#if>
  <tr><td style="${cella} border-top: 1px solid #999;"><strong>QUOTA DOVUTA</strong></td><td style="text-align: right; border-top: 1px solid #999;"><strong>${importi.quota}</strong></td></tr>
  <tr><td style="${cella}">Già pagato</td><td style="text-align: right;">${importi.pagato}</td></tr>
  <tr><td style="${cella}"><strong>DA PAGARE</strong></td><td style="text-align: right;"><strong>${importi.residuo}</strong></td></tr>
</table>
<#else>
<table class="totali importi">
  <#if importi.conVariazione>
  <tr class="listino"><td>Prezzo di listino del corso</td><td class="valore"><s>${importi.listino}</s></td></tr>
  <tr class="listino"><td>${importi.etichettaVariazione}</td><td class="valore">${importi.variazione}</td></tr>
  </#if>
  <tr class="evidenza<#if importi.conVariazione> separa</#if>"><td>QUOTA DOVUTA</td><td class="valore">${importi.quota}</td></tr>
  <tr><td class="normale">Già pagato</td><td class="valore normale">${importi.pagato}</td></tr>
  <tr class="evidenza"><td>DA PAGARE</td><td class="valore">${importi.residuo}</td></tr>
</table>
</#if>
</#macro>
