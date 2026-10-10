package it.attocchi.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import it.attocchi.entity.User;
import it.attocchi.service.UserService;

/**
 * Campi dell'utente collegato a una persona (username, password, nome, cognome, email, telefono) per i dialog
 * di creazione e modifica. Per un utente nuovo username e password sono obbligatori; per uno esistente lo
 * username è in sola lettura e la password non si modifica qui.
 */
public class UtenteFields {

    private final Binder<User> binder = new Binder<>(User.class);
    private final boolean nuovo;
    private final TextField username = new TextField("Username");
    private final PasswordField password = new PasswordField("Password");
    private final TextField nome = new TextField("Nome");
    private final TextField cognome = new TextField("Cognome");
    private final TextField email = new TextField("Email");
    private final TextField telefono = new TextField("Telefono");

    public UtenteFields(User user, UserService userService) {
        this.nuovo = user.getId() == null;

        binder.forField(username).asRequired("Inserisci lo username")
            .withValidator(u -> !nuovo || !userService.existsByUsername(u), "Username già in uso")
            .bind(User::getUsername, User::setUsername);
        binder.forField(nome).asRequired("Inserisci il nome").bind(User::getNome, User::setNome);
        binder.forField(cognome).asRequired("Inserisci il cognome").bind(User::getCognome, User::setCognome);
        binder.forField(email).withNullRepresentation("").bind(User::getEmail, User::setEmail);
        binder.forField(telefono).withNullRepresentation("").bind(User::getTelefono, User::setTelefono);
        binder.readBean(user);

        if (nuovo) {
            password.setRequiredIndicatorVisible(true);
        } else {
            username.setReadOnly(true);
            password.setVisible(false);
        }
    }

    /** Valida i campi mostrando gli errori; per un utente nuovo anche la password. */
    public boolean validate() {
        boolean passwordOk = !nuovo || !password.isEmpty();
        password.setInvalid(!passwordOk);
        password.setErrorMessage(passwordOk ? null : "Inserisci la password");
        return binder.validate().isOk() & passwordOk;
    }

    /** Scrive i campi nell'utente se validi. */
    public boolean writeBeanIfValid(User user) {
        return binder.writeBeanIfValid(user);
    }

    /** Password inserita (solo per un utente nuovo; vuota altrimenti). */
    public String getPassword() {
        return password.getValue();
    }

    public Component[] getComponents() {
        return new Component[] {username, password, nome, cognome, email, telefono};
    }
}
