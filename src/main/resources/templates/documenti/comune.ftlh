<#-- Parti comuni di ricevuta e conferma di iscrizione. XHTML ben formato: lo converte in PDF OpenHTMLtoPDF. -->
<#macro stile>
<style>
  /* Compatto: la conferma (anche con sconto, genitore, bonifico e regole) sta in una pagina A4 */
  @page { size: A4; margin: 12mm 15mm; }
  body { font-family: Helvetica, Arial, sans-serif; font-size: 10pt; color: #222; }
  .intestazione { width: 100%; border-bottom: 1px solid #999; padding-bottom: 3mm; margin-bottom: 3mm; }
  .intestazione td { vertical-align: top; }
  .logo { width: 40mm; }
  .associazione { text-align: right; font-size: 9pt; color: #555; }
  .associazione strong { font-size: 10.5pt; color: #222; }
  h1 { font-size: 15pt; margin: 0 0 1mm; }
  h2 { font-size: 11pt; margin: 4mm 0 1.5mm; border-bottom: 1px solid #ddd; padding-bottom: 1mm; }
  .emessa { color: #555; margin: 0; }
  table.dati { border-collapse: collapse; width: 100%; }
  table.dati td { padding: 0.6mm 0; vertical-align: top; }
  table.dati td.etichetta { width: 45mm; color: #555; }
  /* Allievo e genitore affiancati */
  table.colonne { border-collapse: collapse; width: 100%; }
  td.colonna { width: 50%; vertical-align: top; padding: 0; }
  td.colonna + td.colonna { padding-left: 6mm; }
  td.colonna table.dati td.etichetta { width: 28mm; }
  table.elenco { border-collapse: collapse; width: 100%; }
  table.elenco th, table.elenco td { border-bottom: 1px solid #ddd; padding: 1.5mm 1mm; text-align: left; }
  table.elenco td.importo, table.elenco th.importo { text-align: right; }
  .totali { margin-top: 2mm; width: 100%; }
  .totali td { padding: 1mm 0; }
  .totali td.valore { text-align: right; font-weight: bold; }
  /* Evidenza senza grassetto (il font del PDF non lo rende): righe più grandi su fondo grigio */
  table.importi { border-collapse: collapse; }
  .importi td { padding: 1mm 2mm; }
  .importi tr.listino td { color: #555; font-weight: normal; }
  .importi tr.evidenza td { font-size: 12.5pt; background: #eeeeee; padding: 1.5mm 2mm; }
  .importi tr.separa td { border-top: 1px solid #999; }
  .importi td.normale { font-weight: normal; padding: 1mm 2mm; }
  .riquadro { border: 1px solid #999; padding: 2mm 3mm; margin-top: 3mm; }
  ul.regole { margin: 0; padding-left: 5mm; }
  ul.regole li { margin-bottom: 1mm; }
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

<#-- Allievo e genitore affiancati, poi il corso (la quota è negli importi) -->
<#macro datiIscrizione>
<table class="colonne">
  <tr>
    <td class="colonna">
      <h2>Allievo</h2>
      <table class="dati">
        <tr><td class="etichetta">Nome</td><td>${allievo.nome}</td></tr>
        <tr><td class="etichetta">Codice fiscale</td><td>${allievo.codiceFiscale}</td></tr>
        <tr><td class="etichetta">Indirizzo</td><td>${allievo.indirizzo}</td></tr>
      </table>
    </td>
    <#if genitore??>
    <td class="colonna">
      <h2>Genitore</h2>
      <table class="dati">
        <tr><td class="etichetta">Nome</td><td>${genitore.nome}</td></tr>
        <tr><td class="etichetta">Codice fiscale</td><td>${genitore.codiceFiscale}</td></tr>
      </table>
    </td>
    </#if>
  </tr>
</table>
<h2>Corso</h2>
<table class="dati">
  <tr><td class="etichetta">Corso</td><td>${corso.nome}</td></tr>
  <tr><td class="etichetta">Anno scolastico</td><td>${corso.anno}</td></tr>
  <tr><td class="etichetta">${corso.etichettaInsegnante}</td><td>${corso.insegnante}</td></tr>
</table>
</#macro>
