package it.attocchi.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/** {@link JavaMailSender} solo se è configurato l'host SMTP ({@code MAIL_HOST}); altrimenti l'invio è simulato. */
@Configuration
public class MailConfig {

    @Bean
    @ConditionalOnExpression("!'${app.mail.host:}'.isBlank()")
    public JavaMailSender mailSender(MailProperties mail) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(mail.host());
        sender.setPort(mail.port());
        sender.setUsername(mail.username() != null && !mail.username().isBlank() ? mail.username() : null);
        sender.setPassword(mail.password() != null && !mail.password().isBlank() ? mail.password() : null);
        sender.setDefaultEncoding("UTF-8");
        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", String.valueOf(sender.getUsername() != null));
        props.put("mail.smtp.starttls.enable", String.valueOf(mail.starttls()));
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "20000");
        props.put("mail.smtp.writetimeout", "20000");
        return sender;
    }
}
