package com.myhourly.jewellery.billing.service;

import com.myhourly.jewellery.billing.dto.InvoiceResponse;

/**
 * Called by Billing before an invoice is cancelled. Another module (for example Payment)
 * implements this interface as a Spring bean and throws BusinessValidationException to block
 * the cancellation. Billing never depends on that module.
 */
public interface InvoiceCancellationCheck {

    void assertCanCancel(InvoiceResponse invoice);
}