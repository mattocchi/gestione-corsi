<#import "comune.ftlh" as c>
<!DOCTYPE html>
<html lang="it">
<head>
<meta charset="UTF-8"/>
<title>Lettera di incarico</title>
<@c.stile/>
<style>
  p.testo { margin: 3mm 0; line-height: 1.45; }
  table.firme { width: 100%; margin-top: 16mm; border-collapse: collapse; }
  table.firme td { width: 50%; vertical-align: top; padding: 0 4mm 0 0; }
  .linea { border-top: 1px solid #999; margin-top: 12mm; padding-top: 1mm; color: #555; font-size: 9pt; }
</style>
</head>
<body>
<@c.intestazione titolo="Lettera di incarico"/>
<h2>Incaricato</h2>
<table class="dati">
  <tr><td class="etichetta">Nome</td><td>${insegnante}</td></tr>
  <#if nascita?has_content><tr><td class="etichetta">Nato il</td><td>${nascita}</td></tr></#if>
  <#if codiceFiscale?has_content><tr><td class="etichetta">Codice fiscale</td><td>${codiceFiscale}</td></tr></#if>
  <#if indirizzo?has_content><tr><td class="etichetta">Residenza</td><td>${indirizzo}</td></tr></#if>
</table>
<p class="testo">${associazione.nome} incarica ${insegnante} di svolgere il ${tipo} ${corso}<#if anno?has_content>
  per l'anno scolastico ${anno}</#if>, alle condizioni seguenti.</p>
<h2>Incarico</h2>
<table class="dati">
  <tr><td class="etichetta"><#if servizio>Servizio<#else>Corso</#if></td><td>${corso}</td></tr>
  <#if anno?has_content><tr><td class="etichetta">Anno scolastico</td><td>${anno}</td></tr></#if>
  <#if dataInizio?has_content><tr><td class="etichetta">Inizio</td><td>${dataInizio}</td></tr></#if>
  <#if giorniEOrari?has_content><tr><td class="etichetta">Giorni e orari</td><td>${giorniEOrari}</td></tr></#if>
  <#if luogo?has_content><tr><td class="etichetta">Luogo</td><td>${luogo}</td></tr></#if>
</table>
<h2>Compenso concordato</h2>
<table class="dati">
  <#if tariffaOraria?has_content><tr><td class="etichetta">Tariffa oraria</td><td>${tariffaOraria} all'ora</td></tr></#if>
  <#if unaTantum?has_content><tr><td class="etichetta">Una tantum</td><td>${unaTantum} per l'anno</td></tr></#if>
</table>
<#if nota?has_content><p class="testo">${nota}</p></#if>
<p class="testo">L'incarico vale per l'anno scolastico indicato; il compenso è riconosciuto come rimborso spese.</p>
<table class="firme">
  <tr>
    <td><div class="linea">Per ${associazione.nome}</div></td>
    <td><div class="linea">Per accettazione, ${insegnante}</div></td>
  </tr>
</table>
</body>
</html>
