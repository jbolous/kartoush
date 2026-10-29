# SMS notifications

SMS support is additive and disabled by default. No existing customer, authentication,
email, controller, or job flow invokes it. No dependency or application configuration
files need to change.

Inject `SmsDeliveryService` in a new caller and send an
`SmsMessage(new PhoneNumber("+13125551234"), "Your notification")`.
The recipient and sender must use international E.164 format; messages must contain
1–1600 characters. A successful request means the provider accepted the message,
not that the handset received it.

To enable the Twilio adapter, supply these environment variables at runtime:

```text
KARTOUSH_SMS_DELIVERY_ENABLED=true
KARTOUSH_SMS_DELIVERY_ACCOUNTSID=<Twilio account SID>
KARTOUSH_SMS_DELIVERY_AUTHTOKEN=<Twilio auth token>
KARTOUSH_SMS_DELIVERY_FROMNUMBER=<Twilio sender number>
KARTOUSH_SMS_DELIVERY_TIMEOUT=10s
```

The default adapter is a no-op and requires no credentials. Enabled configuration
is validated at startup. Requests use a fixed HTTPS endpoint, connection and request
timeouts, and no redirects. Provider error bodies, credentials, phone numbers, and
message contents are not logged. The adapter does not retry failed requests because
an ambiguous timeout could otherwise result in duplicate messages.

Provider contract: [Twilio Messages API](https://www.twilio.com/docs/messaging/api/message-resource).
