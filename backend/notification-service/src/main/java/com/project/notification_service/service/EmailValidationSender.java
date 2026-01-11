package com.project.notification_service.service;

import com.project.notification_service.dto.NotificationDto;
import com.project.notification_service.enums.EventTypeNotification;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailValidationSender {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailValidationSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendEmail(NotificationDto codeDto) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(codeDto.getEmail());
            helper.setFrom(fromEmail);

            if (codeDto.getType().equals(EventTypeNotification.EMAIL_VERIFICATION.toString())) {
                helper.setSubject("Email Verification Code");
                helper.setText(verificationHtml(codeDto), true);
            } else if (codeDto.getType().equals(EventTypeNotification.EMAIL_WELCOME.toString())) {
                helper.setSubject("Welcome to the Ultimate Anime Community!");
                helper.setText(welcomeHtml(codeDto), true);
            } else {
                throw new RuntimeException("Unsupported notification type");
            }

            mailSender.send(message);
            log.info("Verification Email sent to {} ({}) with Code : {}", codeDto.getUsername(), codeDto.getEmail() , codeDto.getInformation());

        } catch (MessagingException e) {
            log.error("Failed to send verification email to {}: {}", codeDto.getEmail(), e.getMessage());
            throw new RuntimeException("Email sending failed", e);
        }
    }

    private String verificationHtml(NotificationDto dto) {
        return "<!DOCTYPE html>"
                + "<html lang='en'>"
                + "<head>"
                + "<meta charset='utf-8'/>"
                + "<meta name='viewport' content='width=device-width,initial-scale=1'/>"
                + "<title>Verification code — ThatOtakuNetwork</title>"
                + "<style>@media only screen and (max-width:480px){.container{width:100% !important;padding:12px !important}.code{font-size:22px !important;padding:12px 14px !important}}</style>"
                + "</head>"
                + "<body style='margin:0;padding:0;background-color:#0b0a0f;font-family:Helvetica,Arial,sans-serif;color:#e6edf3;'>"
                + "<span style='display:none;font-size:1px;color:#0b0a0f;line-height:1px;max-height:0;max-width:0;opacity:0;overflow:hidden;'>Your 6-digit verification code for theOtakuNetwork</span>"
                + "<table width='100%' cellpadding='0' cellspacing='0' role='presentation' style='background-color:#0b0a0f;width:100%'>"
                + "<tr><td align='center' style='padding:28px 16px'>"
                + "<table class='container' width='600' cellpadding='0' cellspacing='0' role='presentation' style='width:600px;max-width:100%;background-color:#0f0b12;border-radius:12px;border:1px solid rgba(255,255,255,0.03);box-shadow:0 8px 24px rgba(2,6,23,0.6);'>"
                + "<tr><td style='padding:22px;text-align:center;'>"
                + "<div style='display:inline-block;padding:6px 14px;border-radius:999px;background:rgba(225,29,72,0.12);color:#e11d48;font-weight:700;font-size:13px;letter-spacing:0.06em;text-transform:uppercase;'>ThatOtakuNetwork</div>"
                + "<h2 style='margin:14px 0 8px;font-size:18px;color:#ffffff;font-weight:700;'>Hi " + dto.getUsername()
                + "</h2>"
                + "<p style='margin:0;color:#cbd5e1;font-size:14px;line-height:1.4;max-width:460px;margin-left:auto;margin-right:auto;'>Use the 6-digit code below to verify your account. It expires in <strong>5 minutes</strong>.</p>"
                + "</td></tr>"
                + "<tr><td align='center' style='padding:18px 22px 6px'>"
                + "<table cellpadding='0' cellspacing='0' role='presentation' style='background:#e11d48;border-radius:10px;padding:10px 18px;display:inline-block;'>"
                + "<tr><td align='center' style='font-family:Helvetica,Arial,sans-serif;color:#ffffff;font-weight:800;letter-spacing:0.12em;font-size:28px;'>"
                + "<span class='code' style='display:inline-block;font-family:Courier New,Courier,monospace;background:transparent;color:#fff;padding:14px 22px;border-radius:6px;min-width:200px;text-align:center;'>"
                + dto.getInformation() + "</span>"
                + "</td></tr></table>"
                + "</td></tr>"
                + "<tr><td align='center' style='padding:12px 22px 18px'>"
                + "</td></tr>"
                + "<tr><td style='padding:0 22px 20px;color:#c6d4e6;text-align:center;font-size:13px;line-height:1.4;'>"
                + "<p style='margin:0 0 8px'>If you didn't request this code, you can safely ignore this email.</p>"
                + "<p style='margin:0;color:#c6d4e6;font-size:12px'>Need help? Contact <a href='mailto:thatotakunetwork@gmail.com' style='color:#e11d48;text-decoration:none;'>thatotakunetwork@gmail.com</a></p>"
                + "</td></tr>"
                + "<tr><td style='padding:8px 22px 22px;color:#9aa6b2;text-align:center;font-size:12px;'>© ThatOtakuNetwork — Connect with your anime friends</td></tr>"
                + "</table></td></tr></table></body></html>";
    }

    private String welcomeHtml(NotificationDto dto) {

        return "<!DOCTYPE html>"
                + "<html lang='en'>"
                + "<head>"
                + "<meta charset='utf-8'/>"
                + "<meta name='viewport' content='width=device-width, initial-scale=1'/>"
                + "<title>Welcome to ThatOtakuNetwork</title>"
                + "</head>"
                + "<body style='margin:0;padding:0;background-color:#0b0a0f;"
                + "font-family:-apple-system,BlinkMacSystemFont,Segoe UI,Helvetica,Arial,sans-serif;"
                + "color:#e5e7eb;'>"
                + "<span style='display:none;font-size:1px;color:#0b0a0f;line-height:1px;"
                + "max-height:0;max-width:0;opacity:0;overflow:hidden;'>"
                + "Welcome to ThatOtakuNetwork."
                + "</span>"
                + "<table width='100%' cellpadding='0' cellspacing='0'>"
                + "<tr><td align='center' style='padding:36px 16px'>"
                + "<table width='600' cellpadding='0' cellspacing='0' "
                + "style='background-color:#120c12;border-radius:10px;"
                + "border:1px solid #2a1a1f;'>"
                + "<tr><td style='padding:28px 32px 18px'>"
                + "<h1 style='margin:0 0 8px;font-size:20px;font-weight:600;color:#f9fafb;'>"
                + "Welcome to <span style='color:#e11d48'>ThatOtakuNetwork</span>"
                + "</h1>"
                + "<p style='margin:0;font-size:15px;color:#f1c7cf;line-height:1.5;'>"
                + "Hi " + dto.getUsername() + ","
                + "</p>"
                + "</td></tr>"
                + "<tr><td style='padding:0 32px 24px'>"
                + "<p style='margin:0 0 12px;font-size:14px;color:#e5d1d6;line-height:1.6;'>"
                + "Your account has been created successfully."
                + "</p>"
                + "<p style='margin:0 0 12px;font-size:14px;color:#e5d1d6;line-height:1.6;'>"
                + "ThatOtakuNetwork is a place to discover anime communities, "
                + "participate in discussions, and follow topics that matter to you."
                + "</p>"
                + "<p style='margin:0;font-size:14px;color:#e5d1d6;line-height:1.6;'>"
                + "To begin, complete your profile and explore circles aligned with your interests."
                + "</p>"
                + "</td></tr>"
                + "<tr><td style='padding:0 32px 32px'>"
                + "<a href='http://localhost:5173/'  "
                + "style='display:inline-block;padding:10px 16px;"
                + "background-color:#e11d48;color:#ffffff;text-decoration:none;"
                + "border-radius:6px;font-size:14px;font-weight:500;'>"
                + "Go to dashboard"
                + "</a>"
                + "</td></tr>"
                + "<tr><td style='padding:16px 32px 24px;"
                + "border-top:1px solid #2a1a1f;font-size:12px;color:#c9a3ab;'>"
                + "If you have questions, contact us at "
                + "<a href='mailto:thatotakunetwork@gmail.com' "
                + "style='color:#f43f5e;text-decoration:none;'>"
                + "thatotakunetwork@gmail.com</a>."
                + "<br/><br/>— ThatOtakuNetwork team"
                + "</td></tr>"
                + "</table></td></tr></table>"
                + "</body></html>";
    }

}
