package com.dequeue.cashfree.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VendorOnboardingRequest {
    /** Vendor legal/business name. */
    @NotBlank
    private String name;

    /** Vendor email. */
    @NotBlank
    private String email;

    /** Vendor phone (10 digits). */
    @NotBlank
    private String phone;

    /** Business type: individual, partnership, private_limited, public_limited, llp, trust, society, others. */
    @NotBlank
    private String businessType;

    /** Bank account number. */
    private String bankAccountNumber;

    /** Bank IFSC code. */
    private String bankIfscCode;

    /** Account holder name. */
    private String bankAccountName;

    /** UPI VPA (alternative to bank account). */
    private String upiId;

    /** PAN number. */
    private String pan;

    /** GST number. */
    private String gstNumber;

    /** Address - city. */
    private String city;

    /** Address - state. */
    private String state;

    /** Address - pincode. */
    private String pincode;
}
