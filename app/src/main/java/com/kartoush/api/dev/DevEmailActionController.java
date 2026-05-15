package com.kartoush.api.dev;

import com.kartoush.api.auth.CustomerPasswordResetApplicationService;
import com.kartoush.api.auth.ResetCustomerPasswordRequest;
import com.kartoush.customer.facade.CustomerFacade;
import com.kartoush.customer.facade.model.CustomerActivationView;
import com.kartoush.customer.facade.model.InitialCustomerPasswordInput;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.HtmlUtils;

@Controller
@Profile("dev")
@RequestMapping("/dev")
public class DevEmailActionController {
    // Temporary local-development bridge for email links until Kartoush has a real UI.

    private final CustomerFacade customerFacade;

    private final CustomerPasswordResetApplicationService customerPasswordResetApplicationService;

    public DevEmailActionController(
        final CustomerFacade customerFacade,
        final CustomerPasswordResetApplicationService customerPasswordResetApplicationService
    ) {
        this.customerFacade = customerFacade;
        this.customerPasswordResetApplicationService = customerPasswordResetApplicationService;
    }

    @GetMapping(value = "/customers/activate", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> showActivationPage(
        @RequestParam final String customerId,
        @RequestParam final String token
    ) {
        return htmlOk(
            "Activate customer",
            """
                <p>Use this page to activate the pending customer in your local development environment.</p>
                <form method="post" action="/dev/customers/activate">
                  <input type="hidden" name="customerId" value="%s">
                  <input type="hidden" name="token" value="%s">
                  <button type="submit">Activate customer</button>
                </form>
                <p>Customer id: <code>%s</code></p>
                <p>Token: <code>%s</code></p>
                """.formatted(
                escape(customerId),
                escape(token),
                escape(customerId),
                escape(token)
            )
        );
    }

    @PostMapping(value = "/customers/activate", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> activateCustomer(
        @RequestParam final String customerId,
        @RequestParam final String token
    ) {
        try {
            final CustomerActivationView activationView = customerFacade.activateCustomer(customerId, token);
            return htmlOk(
                "Customer activated",
                """
                    <p>Customer <strong>%s</strong> was activated successfully.</p>
                    <p>Status: <code>%s</code></p>
                    <form method="post" action="/dev/customers/initial-password">
                      <input type="hidden" name="customerId" value="%s">
                      <input type="hidden" name="setupToken" value="%s">
                      <label>
                        Password
                        <input type="password" name="password" required>
                      </label>
                      <label>
                        Confirm password
                        <input type="password" name="confirmPassword" required>
                      </label>
                      <button type="submit">Set initial password</button>
                    </form>
                    """.formatted(
                    escape(activationView.email()),
                    escape(activationView.status().name()),
                    escape(activationView.customerId()),
                    escape(activationView.passwordSetupToken())
                )
            );
        }
        catch (final RuntimeException exception) {
            return htmlError("Activation failed", exception.getMessage());
        }
    }

    @PostMapping(value = "/customers/initial-password", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> setInitialPassword(
        @RequestParam final String customerId,
        @RequestParam final String setupToken,
        @RequestParam final String password,
        @RequestParam final String confirmPassword
    ) {
        try {
            customerFacade.setInitialPassword(
                customerId,
                new InitialCustomerPasswordInput(setupToken, password, confirmPassword)
            );
            return htmlOk(
                "Initial password set",
                """
                    <p>The initial password was set successfully for customer <code>%s</code>.</p>
                    <p>You can now use the normal sign-in API flow.</p>
                    """.formatted(escape(customerId))
            );
        }
        catch (final RuntimeException exception) {
            return htmlError("Initial password setup failed", exception.getMessage());
        }
    }

    @GetMapping(value = "/auth/password-reset", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> showPasswordResetPage(
        @RequestParam final String email,
        @RequestParam final String token
    ) {
        return htmlOk(
            "Reset customer password",
            """
                <p>Use this page to complete a password reset in your local development environment.</p>
                <form method="post" action="/dev/auth/password-reset">
                  <input type="hidden" name="email" value="%s">
                  <input type="hidden" name="token" value="%s">
                  <label>
                    Password
                    <input type="password" name="password" required>
                  </label>
                  <label>
                    Confirm password
                    <input type="password" name="confirmPassword" required>
                  </label>
                  <button type="submit">Reset password</button>
                </form>
                <p>Email: <code>%s</code></p>
                <p>Token: <code>%s</code></p>
                """.formatted(
                escape(email),
                escape(token),
                escape(email),
                escape(token)
            )
        );
    }

    @PostMapping(value = "/auth/password-reset", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> resetPassword(
        @RequestParam final String email,
        @RequestParam final String token,
        @RequestParam final String password,
        @RequestParam final String confirmPassword
    ) {
        try {
            customerPasswordResetApplicationService.resetPassword(
                new ResetCustomerPasswordRequest(email, token, password, confirmPassword)
            );
            return htmlOk(
                "Password reset complete",
                """
                    <p>The password reset completed successfully for <code>%s</code>.</p>
                    <p>You can now sign in with the new password.</p>
                    """.formatted(escape(email))
            );
        }
        catch (final RuntimeException exception) {
            return htmlError("Password reset failed", exception.getMessage());
        }
    }

    private ResponseEntity<String> htmlOk(final String title, final String body) {
        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_HTML)
            .body(page(title, body));
    }

    private ResponseEntity<String> htmlError(final String title, final String message) {
        return ResponseEntity.badRequest()
            .contentType(MediaType.TEXT_HTML)
            .body(page(title, "<p>" + escape(message) + "</p>"));
    }

    private String page(final String title, final String body) {
        return """
            <!doctype html>
            <html lang="en">
              <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>%s</title>
                <style>
                  body { font-family: sans-serif; margin: 2rem; line-height: 1.5; }
                  form { display: grid; gap: 0.75rem; max-width: 28rem; margin-top: 1rem; }
                  label { display: grid; gap: 0.25rem; }
                  input, button { font: inherit; padding: 0.6rem; }
                  code { background: #f5f5f5; padding: 0.1rem 0.3rem; }
                  button { cursor: pointer; }
                </style>
              </head>
              <body>
                <h1>%s</h1>
                %s
              </body>
            </html>
            """.formatted(escape(title), escape(title), body);
    }

    private String escape(final String value) {
        return HtmlUtils.htmlEscape(value);
    }
}
