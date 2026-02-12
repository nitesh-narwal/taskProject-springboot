package com.example.shopapi.util;

import com.mailjet.client.ClientOptions;
import com.mailjet.client.MailjetClient;
import com.mailjet.client.MailjetRequest;
import com.mailjet.client.MailjetResponse;
import com.mailjet.client.resource.Emailv31;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    private final MailjetClient mailjetClient;
    private final String senderEmail;
    private final String senderName;
    private final String frontendUrl;
    private final String baseUrl;

    public EmailService(
            @Value("${mailjet.api-key}") String apiKey,
            @Value("${mailjet.api-secret}") String apiSecret,
            @Value("${mailjet.sender-email}") String senderEmail,
            @Value("${mailjet.sender-name}") String senderName,
            @Value("${app.frontend-url}") String frontendUrl,
            @Value("${app.base-url}") String baseUrl) {
        this.mailjetClient = new MailjetClient(
                ClientOptions.builder()
                        .apiKey(apiKey)
                        .apiSecretKey(apiSecret)
                        .build()
        );
        this.senderEmail = senderEmail;
        this.senderName = senderName;
        this.frontendUrl = frontendUrl;
        this.baseUrl = baseUrl;
    }

    @Async
    public void sendVerificationEmail(String toEmail, String toName, String token) {
        String verificationLink = baseUrl + "/api/auth/verify-email?token=" + token;
        String subject = "Verify Your Email - Shop API";
        String htmlContent = buildVerificationEmailTemplate(toName, verificationLink);
        String textContent = "Hi " + toName + ",\n\nPlease verify your email by clicking the following link:\n"
                + verificationLink + "\n\nThis link will expire in 15 minutes.";

        sendEmail(toEmail, toName, subject, htmlContent, textContent);
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String toName, String token) {
        String resetLink = frontendUrl + "/reset-password?token=" + token;
        String subject = "Reset Your Password - Shop API";
        String htmlContent = buildPasswordResetEmailTemplate(toName, resetLink);
        String textContent = "Hi " + toName + ",\n\nYou requested to reset your password. Click the following link:\n"
                + resetLink + "\n\nThis link will expire in 15 minutes.\n\nIf you didn't request this, please ignore this email.";

        sendEmail(toEmail, toName, subject, htmlContent, textContent);
    }

    @Async
    public void sendOrderConfirmationEmail(String toEmail, String toName, String orderId, String totalAmount) {
        String subject = "Order Confirmation #" + orderId + " - Shop API";
        String htmlContent = buildOrderConfirmationTemplate(toName, orderId, totalAmount);
        String textContent = "Hi " + toName + ",\n\nThank you for your order!\n\nOrder ID: " + orderId
                + "\nTotal Amount: $" + totalAmount + "\n\nWe'll notify you when your order ships.";

        sendEmail(toEmail, toName, subject, htmlContent, textContent);
    }

    @Async
    public void sendPaymentSuccessEmail(String toEmail, String toName, String orderId, String amount) {
        String subject = "Payment Successful - Order #" + orderId;
        String htmlContent = buildPaymentSuccessTemplate(toName, orderId, amount);
        String textContent = "Hi " + toName + ",\n\nYour payment of $" + amount + " for Order #" + orderId
                + " was successful.\n\nThank you for shopping with us!";

        sendEmail(toEmail, toName, subject, htmlContent, textContent);
    }

    @Async
    public void sendPaymentFailedEmail(String toEmail, String toName, String orderId, String reason) {
        String subject = "Payment Failed - Order #" + orderId;
        String htmlContent = buildPaymentFailedTemplate(toName, orderId, reason);
        String textContent = "Hi " + toName + ",\n\nUnfortunately, your payment for Order #" + orderId
                + " failed.\n\nReason: " + reason + "\n\nPlease try again or contact support.";

        sendEmail(toEmail, toName, subject, htmlContent, textContent);
    }

    @Async
    public void sendOrderShippedEmail(String toEmail, String toName, String orderId, String trackingNumber) {
        String subject = "Your Order Has Shipped - #" + orderId;
        String htmlContent = buildOrderShippedTemplate(toName, orderId, trackingNumber);
        String textContent = "Hi " + toName + ",\n\nGreat news! Your order #" + orderId + " has been shipped.\n\n"
                + "Tracking Number: " + trackingNumber;

        sendEmail(toEmail, toName, subject, htmlContent, textContent);
    }

    private void sendEmail(String toEmail, String toName, String subject, String htmlContent, String textContent) {
        try {
            MailjetRequest request = new MailjetRequest(Emailv31.resource)
                    .property(Emailv31.MESSAGES, new JSONArray()
                            .put(new JSONObject()
                                    .put(Emailv31.Message.FROM, new JSONObject()
                                            .put("Email", senderEmail)
                                            .put("Name", senderName))
                                    .put(Emailv31.Message.TO, new JSONArray()
                                            .put(new JSONObject()
                                                    .put("Email", toEmail)
                                                    .put("Name", toName != null ? toName : toEmail)))
                                    .put(Emailv31.Message.SUBJECT, subject)
                                    .put(Emailv31.Message.TEXTPART, textContent)
                                    .put(Emailv31.Message.HTMLPART, htmlContent)));

            MailjetResponse response = mailjetClient.post(request);

            if (response.getStatus() == 200) {
                log.info("Email sent successfully to {}", toEmail);
            } else {
                log.error("Failed to send email to {}. Status: {}, Data: {}",
                        toEmail, response.getStatus(), response.getData());
            }
        } catch (Exception e) {
            log.error("Error sending email to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    private String buildVerificationEmailTemplate(String name, String link) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background: #4F46E5; color: white; padding: 20px; text-align: center; }
                        .content { padding: 30px; background: #f9fafb; }
                        .button { display: inline-block; padding: 12px 30px; background: #4F46E5; color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                        .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Email Verification</h1>
                        </div>
                        <div class="content">
                            <p>Hi %s,</p>
                            <p>Thank you for registering with Shop API! Please verify your email address by clicking the button below:</p>
                            <p style="text-align: center;">
                                <a href="%s" class="button">Verify Email</a>
                            </p>
                            <p>This link will expire in 15 minutes.</p>
                            <p>If you didn't create an account, please ignore this email.</p>
                        </div>
                        <div class="footer">
                            <p>&copy; 2024 Shop API. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(name != null ? name : "User", link);
    }

    private String buildPasswordResetEmailTemplate(String name, String link) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background: #DC2626; color: white; padding: 20px; text-align: center; }
                        .content { padding: 30px; background: #f9fafb; }
                        .button { display: inline-block; padding: 12px 30px; background: #DC2626; color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                        .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Password Reset</h1>
                        </div>
                        <div class="content">
                            <p>Hi %s,</p>
                            <p>We received a request to reset your password. Click the button below to create a new password:</p>
                            <p style="text-align: center;">
                                <a href="%s" class="button">Reset Password</a>
                            </p>
                            <p>This link will expire in 15 minutes.</p>
                            <p>If you didn't request a password reset, please ignore this email or contact support if you're concerned.</p>
                        </div>
                        <div class="footer">
                            <p>&copy; 2024 Shop API. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(name != null ? name : "User", link);
    }

    private String buildOrderConfirmationTemplate(String name, String orderId, String totalAmount) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background: #059669; color: white; padding: 20px; text-align: center; }
                        .content { padding: 30px; background: #f9fafb; }
                        .order-details { background: white; padding: 20px; border-radius: 5px; margin: 20px 0; }
                        .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Order Confirmed!</h1>
                        </div>
                        <div class="content">
                            <p>Hi %s,</p>
                            <p>Thank you for your order! We're getting it ready for you.</p>
                            <div class="order-details">
                                <p><strong>Order ID:</strong> %s</p>
                                <p><strong>Total Amount:</strong> $%s</p>
                            </div>
                            <p>We'll send you another email when your order ships.</p>
                        </div>
                        <div class="footer">
                            <p>&copy; 2024 Shop API. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(name, orderId, totalAmount);
    }

    private String buildPaymentSuccessTemplate(String name, String orderId, String amount) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background: #059669; color: white; padding: 20px; text-align: center; }
                        .content { padding: 30px; background: #f9fafb; }
                        .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Payment Successful!</h1>
                        </div>
                        <div class="content">
                            <p>Hi %s,</p>
                            <p>Your payment of <strong>$%s</strong> for Order #%s has been processed successfully.</p>
                            <p>Thank you for shopping with us!</p>
                        </div>
                        <div class="footer">
                            <p>&copy; 2024 Shop API. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(name, amount, orderId);
    }

    private String buildPaymentFailedTemplate(String name, String orderId, String reason) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background: #DC2626; color: white; padding: 20px; text-align: center; }
                        .content { padding: 30px; background: #f9fafb; }
                        .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Payment Failed</h1>
                        </div>
                        <div class="content">
                            <p>Hi %s,</p>
                            <p>Unfortunately, we couldn't process your payment for Order #%s.</p>
                            <p><strong>Reason:</strong> %s</p>
                            <p>Please try again or use a different payment method.</p>
                        </div>
                        <div class="footer">
                            <p>&copy; 2024 Shop API. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(name, orderId, reason);
    }

    private String buildOrderShippedTemplate(String name, String orderId, String trackingNumber) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background: #2563EB; color: white; padding: 20px; text-align: center; }
                        .content { padding: 30px; background: #f9fafb; }
                        .tracking { background: white; padding: 20px; border-radius: 5px; margin: 20px 0; text-align: center; }
                        .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Your Order Has Shipped!</h1>
                        </div>
                        <div class="content">
                            <p>Hi %s,</p>
                            <p>Great news! Your order #%s is on its way.</p>
                            <div class="tracking">
                                <p><strong>Tracking Number:</strong></p>
                                <p style="font-size: 18px;">%s</p>
                            </div>
                        </div>
                        <div class="footer">
                            <p>&copy; 2024 Shop API. All rights reserved.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(name, orderId, trackingNumber != null ? trackingNumber : "N/A");
    }
}

