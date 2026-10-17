package it.attocchi.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.time.MonthDay;

/**
 * Festività saltata dalla generazione delle lezioni: un giorno o un periodo che si ripete ogni anno (anche a cavallo
 * d'anno, es. 23/12–6/1), date precise (un giorno o un periodo) oppure Pasqua o Pasquetta, calcolate per ogni anno.
 * Gestite da admin e segreteria; valgono solo quelle attive.
 */
@Entity
@Table(name = "festivita")
public class Festivita {

    public enum Tipo {
        OGNI_ANNO("Ogni anno"),
        DATE("Date precise"),
        PASQUA("Pasqua"),
        PASQUETTA("Pasquetta");

        private final String etichetta;

        Tipo(String etichetta) {
            this.etichetta = etichetta;
        }

        public String getEtichetta() {
            return etichetta;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Tipo tipo = Tipo.OGNI_ANNO;

    /** {@link Tipo#OGNI_ANNO}: primo giorno; l'ultimo è facoltativo (null = un solo giorno). */
    private Integer giornoInizio;
    private Integer meseInizio;
    private Integer giornoFine;
    private Integer meseFine;

    /** {@link Tipo#DATE}: primo giorno; l'ultimo è facoltativo (null = un solo giorno). */
    private LocalDate dataInizio;
    private LocalDate dataFine;

    @Column(nullable = false)
    private boolean attivo = true;

    public Festivita() {
    }

    /** Festività di un solo giorno che si ripete ogni anno. */
    public static Festivita ogniAnno(String nome, int giorno, int mese) {
        Festivita festivita = new Festivita();
        festivita.setNome(nome);
        festivita.setTipo(Tipo.OGNI_ANNO);
        festivita.setGiornoInizio(giorno);
        festivita.setMeseInizio(mese);
        return festivita;
    }

    /** Pasqua o Pasquetta. */
    public static Festivita calcolata(String nome, Tipo tipo) {
        Festivita festivita = new Festivita();
        festivita.setNome(nome);
        festivita.setTipo(tipo);
        return festivita;
    }

    /** Domenica di Pasqua dell'anno (calendario gregoriano, algoritmo di Meeus/Jones/Butcher). */
    public static LocalDate pasqua(int anno) {
        int a = anno % 19;
        int b = anno / 100;
        int c = anno % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int mese = (h + l - 7 * m + 114) / 31;
        int giorno = (h + l - 7 * m + 114) % 31 + 1;
        return LocalDate.of(anno, mese, giorno);
    }

    /** {@code true} se la data cade in questa festività (attiva o no). */
    public boolean comprende(LocalDate data) {
        return switch (tipo) {
            case OGNI_ANNO -> {
                MonthDay inizio = MonthDay.of(meseInizio, giornoInizio);
                MonthDay fine = meseFine != null && giornoFine != null ? MonthDay.of(meseFine, giornoFine) : inizio;
                MonthDay giorno = MonthDay.from(data);
                yield fine.isBefore(inizio)
                    // A cavallo d'anno, es. 23/12–6/1
                    ? !giorno.isBefore(inizio) || !giorno.isAfter(fine)
                    : !giorno.isBefore(inizio) && !giorno.isAfter(fine);
            }
            case DATE -> !data.isBefore(dataInizio) && !data.isAfter(dataFine != null ? dataFine : dataInizio);
            case PASQUA -> data.equals(pasqua(data.getYear()));
            case PASQUETTA -> data.equals(pasqua(data.getYear()).plusDays(1));
        };
    }

    /** Primo giorno della festività da {@code da} in poi (entro due anni), {@code null} se non ce ne sono più. */
    public LocalDate prossima(LocalDate da) {
        for (LocalDate data = da; data.isBefore(da.plusYears(2)); data = data.plusDays(1)) {
            if (comprende(data)) {
                return data;
            }
        }
        return null;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Tipo getTipo() {
        return tipo;
    }

    public void setTipo(Tipo tipo) {
        this.tipo = tipo;
    }

    public Integer getGiornoInizio() {
        return giornoInizio;
    }

    public void setGiornoInizio(Integer giornoInizio) {
        this.giornoInizio = giornoInizio;
    }

    public Integer getMeseInizio() {
        return meseInizio;
    }

    public void setMeseInizio(Integer meseInizio) {
        this.meseInizio = meseInizio;
    }

    public Integer getGiornoFine() {
        return giornoFine;
    }

    public void setGiornoFine(Integer giornoFine) {
        this.giornoFine = giornoFine;
    }

    public Integer getMeseFine() {
        return meseFine;
    }

    public void setMeseFine(Integer meseFine) {
        this.meseFine = meseFine;
    }

    public LocalDate getDataInizio() {
        return dataInizio;
    }

    public void setDataInizio(LocalDate dataInizio) {
        this.dataInizio = dataInizio;
    }

    public LocalDate getDataFine() {
        return dataFine;
    }

    public void setDataFine(LocalDate dataFine) {
        this.dataFine = dataFine;
    }

    public boolean isAttivo() {
        return attivo;
    }

    public void setAttivo(boolean attivo) {
        this.attivo = attivo;
    }

    /**
     * Uguaglianza basata sull'id: due istanze transient (id null) sono diverse.
     * hashCode costante per classe: resta stabile quando l'id viene assegnato al salvataggio.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Festivita other)) {
            return false;
        }
        return getId() != null && getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Festivita.class.hashCode();
    }
}
