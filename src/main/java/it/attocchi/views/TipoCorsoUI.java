package it.attocchi.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import it.attocchi.entity.Corso;

/** Tipo del corso nelle view: badge "Servizio" (i corsi non ne hanno) e nome del corso con il badge accanto. */
public final class TipoCorsoUI {

    private TipoCorsoUI() {
    }

    /** Badge "Servizio", o {@code null} per un corso. */
    public static Badge badge(Corso corso) {
        return corso.isServizio() ? new Badge("Servizio", VaadinIcon.TOOLS.create()) : null;
    }

    /** Nome del corso seguito dal badge "Servizio" se è un servizio. */
    public static Component nome(Corso corso) {
        Span nome = new Span(corso.getNome());
        Badge badge = badge(corso);
        if (badge == null) {
            return nome;
        }
        Span riga = new Span(nome, badge);
        riga.addClassNames("inline-flex flex-wrap items-center gap-2");
        return riga;
    }
}
