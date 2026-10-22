<#-- Parti comuni di ricevuta e conferma di iscrizione. XHTML ben formato: lo converte in PDF OpenHTMLtoPDF. -->
<#macro stile>
<style>
  @page { size: A4; margin: 18mm 16mm; }
  body { font-family: Helvetica, Arial, sans-serif; font-size: 10.5pt; color: #222; }
  .intestazione { width: 100%; border-bottom: 1px solid #999; padding-bottom: 6mm; margin-bottom: 6mm; }
  .intestazione td { vertical-align: top; }
  .logo { width: 45mm; }
  .associazione { text-align: right; font-size: 9pt; color: #555; }
  .associazione strong { font-size: 10.5pt; color: #222; }
  h1 { font-size: 16pt; margin: 0 0 1mm; }
  h2 { font-size: 11.5pt; margin: 6mm 0 2mm; border-bottom: 1px solid #ddd; padding-bottom: 1mm; }
  .emessa { color: #555; margin: 0; }
  table.dati { border-collapse: collapse; width: 100%; }
  table.dati td { padding: 1mm 0; vertical-align: top; }
  table.dati td.etichetta { width: 45mm; color: #555; }
  table.elenco { border-collapse: collapse; width: 100%; }
  table.elenco th, table.elenco td { border-bottom: 1px solid #ddd; padding: 1.5mm 1mm; text-align: left; }
  table.elenco td.importo, table.elenco th.importo { text-align: right; }
  .totali { margin-top: 5mm; width: 100%; }
  .totali td { padding: 1mm 0; }
  .totali td.valore { text-align: right; font-weight: bold; }
  .riquadro { border: 1px solid #999; padding: 3mm 4mm; margin-top: 6mm; }
  ul.regole { margin: 0; padding-left: 5mm; }
  ul.regole li { margin-bottom: 1.5mm; }
</style>
</#macro>

<#macro intestazione titolo>
<table class="intestazione">
  <tr>
    <td><#if logo??><img class="logo" src="${logo}" alt="${associazione.nome}"/></#if></td>
    <td class="associazione">
      <strong>${associazione.nome}</strong>
      <#if associazione.indirizzo?has_content><br/>${associazione.indirizzo}</#if>
      <#if associazione.codiceFiscale?has_content><br/>C.F. ${associazione.codiceFiscale}</#if>
      <#if associazione.email?has_content><br/>${associazione.email}</#if>
    </td>
  </tr>
</table>
<h1>${titolo}</h1>
<p class="emessa">Emessa il ${emessa}</p>
</#macro>

<#macro datiIscrizione>
<h2>Allievo</h2>
<table class="dati">
  <tr><td class="etichetta">Nome</td><td>${allievo.nome}</td></tr>
  <tr><td class="etichetta">Codice fiscale</td><td>${allievo.codiceFiscale}</td></tr>
  <tr><td class="etichetta">Indirizzo</td><td>${allievo.indirizzo}</td></tr>
</table>
<#if genitore??>
<h2>Genitore</h2>
<table class="dati">
  <tr><td class="etichetta">Nome</td><td>${genitore.nome}</td></tr>
  <tr><td class="etichetta">Codice fiscale</td><td>${genitore.codiceFiscale}</td></tr>
</table>
</#if>
<h2>Corso</h2>
<table class="dati">
  <tr><td class="etichetta">Corso</td><td>${corso.nome}</td></tr>
  <tr><td class="etichetta">Anno scolastico</td><td>${corso.anno}</td></tr>
  <tr><td class="etichetta">${corso.etichettaInsegnante}</td><td>${corso.insegnante}</td></tr>
  <tr><td class="etichetta">Quota</td><td>${corso.quota}</td></tr>
</table>
</#macro>
