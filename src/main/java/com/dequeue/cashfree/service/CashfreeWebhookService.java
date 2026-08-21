package com.dequeue.cashfree.service;

/**
 * Webhook event processing and signature verification.
 */
public interface CashfreeWebhookService {

    /**
     * Verify the Cashfree webhook signature.
     * Cashfree uses timestamp + body HMAC-SHA256 with the webhook secret.
     *
     * @param rawBody       Raw request body as string.
     * @param signature     Value of x-webhook-signature header.
     * @param timestamp     Value of x-webhook-timestamp header.
     * @return              true if signature is valid.
     */
    boolean verifySignature(String rawBody, String signature, String timestamp);

    /**
     * Process a verified webhook event.
     *
     * @param rawBody  Raw JSON body of the webhook.
     */
    void processWebhookEvent(String rawBody);
}
