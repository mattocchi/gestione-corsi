<#-- Regole su recuperi, assenze e rinuncia, uguali nell'email e nel PDF di conferma di iscrizione.
     XHTML ben formato (lo usa anche OpenHTMLtoPDF); con email=true gli stili sono in linea per i client di posta. -->
<#macro regole email=false>
<#if email>
<#assign sottoelenco = "margin: 4px 0 0; padding-left: 20px;">
<p style="margin: 16px 0 4px;"><strong>Lezioni, assenze e recuperi</strong></p>
<ul style="margin: 0; padding-left: 20px;">
<#else>
<h2>Lezioni, assenze e recuperi</h2>
<ul class="regole">
<#assign sottoelenco = "">
</#if>
  <li><strong>Se l'insegnante non può fare lezione</strong> – La lezione viene sempre recuperata, in un altro giorno o
    al termine del corso: il numero di lezioni previsto è garantito.</li>
  <li><strong>Assenze dell’allievo (solo lezioni individuali)</strong> – Le lezioni individuali sono organizzate su
    misura, riservando per ogni allievo il tempo dell’insegnante e gli spazi della scuola. Per questo, se l’allievo non
    può partecipare, è importante avvisare la segreteria il prima possibile e sempre tramite la community WhatsApp: più
    l’avviso è tempestivo, più è facile riorganizzare il calendario e trovare una data alternativa.
    <ul<#if sottoelenco?has_content> style="${sottoelenco}"</#if>>
      <li>Con un preavviso di almeno una settimana, il recupero della lezione è garantito e verrà concordato con
        l’insegnante.</li>
      <li>Con un preavviso inferiore, faremo il possibile per organizzare un recupero, ma solo se l’insegnante e gli
        spazi saranno disponibili. In caso di avviso a ridosso della lezione, il recupero sarà difficilmente
        realizzabile.</li>
      <li>Se il recupero non può essere organizzato, la lezione verrà comunque conteggiata e dovrà essere pagata.</li>
    </ul></li>
  <li><strong>Se decidete di ritirarvi</strong> – Le lezioni non frequentate non vengono rimborsate. Situazioni
    particolari saranno valutate caso per caso dalla segreteria dei corsi, a suo insindacabile giudizio.</li>
</ul>
</#macro>
