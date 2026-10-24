<#-- Regole su recuperi, assenze e rinuncia, uguali nell'email e nel PDF di conferma di iscrizione.
     XHTML ben formato (lo usa anche OpenHTMLtoPDF); con email=true gli stili sono in linea per i client di posta. -->
<#macro regole email=false>
<#if email>
<p style="margin: 16px 0 4px;"><strong>Lezioni, assenze e recuperi</strong></p>
<ul style="margin: 0; padding-left: 20px;">
<#else>
<h2>Lezioni, assenze e recuperi</h2>
<ul class="regole">
</#if>
  <li><strong>Se l'insegnante non può fare lezione</strong> – La lezione viene sempre recuperata, in un altro giorno o
    al termine del corso: il numero di lezioni previsto è garantito.</li>
  <li><strong>Se l'allievo non può venire</strong> – Avvisate la segreteria dei corsi almeno una settimana prima:
    l'assenza sarà giustificata e, quando possibile, concorderemo un recupero con l'insegnante. Con un preavviso più
    breve, o senza avviso, la lezione non potrà essere recuperata e resta dovuta.</li>
  <li><strong>Se decidete di ritirarvi</strong> – Le lezioni non frequentate non vengono rimborsate. Situazioni
    particolari saranno valutate caso per caso dalla segreteria dei corsi, a suo insindacabile giudizio.</li>
</ul>
</#macro>
