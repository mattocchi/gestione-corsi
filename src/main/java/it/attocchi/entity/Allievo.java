package it.attocchi.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "allievi")
public class Allievo implements Anagrafica, IntestazioneDocumenti {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    /** Facoltativo, ma necessario per le ricevute ({@link #getDatiMancantiPer(TipoDocumento)}). */
    private String codiceFiscale;

    /** Facoltativa, ma necessaria per conferme di iscrizione e ricevute. */
    private LocalDate dataNascita;

    private String luogoNascita;

    /** Intestatario dell'attestazione dell'allievo adulto, se diverso da lui ({@link IntestazioneDocumenti}). */
    private String intestazioneNome;

    private String intestazioneCognome;

    private String intestazioneCodiceFiscale;

    @Embedded
    private Indirizzo indirizzo = new Indirizzo();

    @OneToMany(mappedBy = "allievo")
    private Set<Iscrizione> iscrizioni = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "genitore_id")
    private Genitore genitore;

    // Costruttori
    public Allievo() {
    }

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getCodiceFiscale() {
        return codiceFiscale;
    }

    public void setCodiceFiscale(String codiceFiscale) {
        this.codiceFiscale = codiceFiscale;
    }

    /** Età in anni compiuti alla data indicata; {@code null} senza data di nascita. */
    public Integer getEta(LocalDate oggi) {
        return dataNascita != null ? java.time.Period.between(dataNascita, oggi).getYears() : null;
    }

    public LocalDate getDataNascita() {
        return dataNascita;
    }

    public void setDataNascita(LocalDate dataNascita) {
        this.dataNascita = dataNascita;
    }

    public String getIntestazioneNome() {
        return intestazioneNome;
    }

    public void setIntestazioneNome(String intestazioneNome) {
        this.intestazioneNome = intestazioneNome;
    }

    public String getIntestazioneCognome() {
        return intestazioneCognome;
    }

    public void setIntestazioneCognome(String intestazioneCognome) {
        this.intestazioneCognome = intestazioneCognome;
    }

    public String getIntestazioneCodiceFiscale() {
        return intestazioneCodiceFiscale;
    }

    public void setIntestazioneCodiceFiscale(String intestazioneCodiceFiscale) {
        this.intestazioneCodiceFiscale = intestazioneCodiceFiscale;
    }

    /** Quello indicato per l'intestatario, altrimenti il codice fiscale dell'allievo. */
    public String getCodiceFiscaleIntestatario() {
        return intestazioneCodiceFiscale != null && !intestazioneCodiceFiscale.isBlank() ? intestazioneCodiceFiscale : codiceFiscale;
    }

    public String getLuogoNascita() {
        return luogoNascita;
    }

    public void setLuogoNascita(String luogoNascita) {
        this.luogoNascita = luogoNascita;
    }

    /** Mai {@code null}: Hibernate lascia l'embedded a {@code null} quando tutte le colonne sono vuote. */
    public Indirizzo getIndirizzo() {
        if (indirizzo == null) {
            indirizzo = new Indirizzo();
        }
        return indirizzo;
    }

    public void setIndirizzo(Indirizzo indirizzo) {
        this.indirizzo = indirizzo;
    }

    /** {@code true} se l'allievo non ha nessun campo dell'indirizzo e si mostra quello del genitore. */
    public boolean isIndirizzoDelGenitore() {
        return getIndirizzo().isVuoto() && genitore != null && !genitore.getIndirizzo().isVuoto();
    }

    /** Indirizzo da mostrare: il proprio o, se non ne ha nessun campo, quello del genitore (non copiato). */
    public Indirizzo getIndirizzoEffettivo() {
        return isIndirizzoDelGenitore() ? genitore.getIndirizzo() : getIndirizzo();
    }

    /**
     * Dati necessari al documento che mancano: la data di nascita per conferma e ricevuta, il codice fiscale solo per
     * la ricevuta ("codice fiscale", "data di nascita"); vuota se completi.
     */
    public List<String> getDatiMancantiPer(TipoDocumento tipo) {
        List<String> mancanti = new ArrayList<>();
        if (tipo == TipoDocumento.RICEVUTA && (codiceFiscale == null || codiceFiscale.isBlank())) {
            mancanti.add("codice fiscale");
        }
        if (dataNascita == null) {
            mancanti.add("data di nascita");
        }
        return mancanti;
    }

    /** Dati mancanti per almeno un documento (quelli della ricevuta, che comprendono quelli della conferma). */
    public List<String> getDatiMancantiPerDocumenti() {
        return getDatiMancantiPer(TipoDocumento.RICEVUTA);
    }

    /** {@code true} se ci sono i dati per stampare il documento. */
    public boolean isDatiPer(TipoDocumento tipo) {
        return getDatiMancantiPer(tipo).isEmpty();
    }

    public Set<Iscrizione> getIscrizioni() {
        return iscrizioni;
    }

    /** Corsi a cui l'allievo è iscritto, ricavati dalle iscrizioni. */
    public Set<Corso> getCorsi() {
        return iscrizioni.stream().map(Iscrizione::getCorso).collect(Collectors.toSet());
    }

    public Genitore getGenitore() {
        return genitore;
    }

    public void setGenitore(Genitore genitore) {
        this.genitore = genitore;
    }

    public String getNomeCompleto() {
        return user != null ? user.getNomeCompleto() : "";
    }

    /**
     * Uguaglianza basata sull'id (entity non ancora salvate sono uguali solo a se stesse).
     * hashCode costante per classe: resta stabile quando l'id viene assegnato al salvataggio.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Allievo other)) {
            return false;
        }
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Allievo.class.hashCode();
    }
}
