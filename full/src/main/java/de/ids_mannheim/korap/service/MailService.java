package de.ids_mannheim.korap.service;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Service;

import de.ids_mannheim.korap.interfaces.AuthenticationManagerIface;
import de.ids_mannheim.korap.user.User;

@Service
public class MailService {
    @Autowired
    private AuthenticationManagerIface authManager;
//    @Autowired
    private JavaMailSender mailSender;
//    @Autowired
    private VelocityEngine velocityEngine;

    public void sendMemberInvitationNotification (String inviteeName,
            String groupName, String inviter) {

        MimeMessagePreparator preparator = new MimeMessagePreparator() {

            public void prepare (MimeMessage mimeMessage) throws Exception {

                User invitee = authManager.getUser(inviteeName);

                MimeMessageHelper message = new MimeMessageHelper(mimeMessage);
                message.setTo(new InternetAddress(invitee.getEmail()));
                message.setFrom("noreply-korap-notification@ids-mannheim.de");
                message.setSubject("Invitation to join group");
                message.setText(prepareText(inviteeName, groupName, inviter),
                        true);
            }

        };
        mailSender.send(preparator);
    }

    private String prepareText (String username, String groupName,
            String inviter) {
        Context context = new VelocityContext();
        context.put("username", username);
        context.put("groupName", groupName);
        context.put("inviter", inviter);

        StringWriter stringWriter = new StringWriter();
        velocityEngine.mergeTemplate("invitationNotification.vm",
                StandardCharsets.UTF_16.name(), context, stringWriter);
        return stringWriter.toString();
    }
}
