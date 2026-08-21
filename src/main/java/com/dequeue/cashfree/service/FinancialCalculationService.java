package com.dequeue.cashfree.service;

import com.dequeue.cashfree.entity.CashfreePlatformConfig;
import com.dequeue.cashfree.entity.CommissionType;
import com.dequeue.cashfree.repository.CashfreePlatformConfigRepository;
import com.dequeue.vendor.entity.Vendor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Single authoritative source of financial calculation for DeQueue.
 *
 * All commission, fee, and split calculations MUST go through this service.
 * No other service should implement its own formula.
 *
 * Formula:
 * <pre>
 * Customer pays:       orderAmount
 * Platform commission: orderAmount * commissionRate / 100  (PERCENTAGE)
 *                   OR commissionRate                       (FIXED)
 * Vendor gross share:  orderAmount - platformCommission
 *
 * After Cashfree fees are known:
 * Cashfree fee:        (from Cashfree settlement data — never hardcoded)
 * Cashfree tax:        (from Cashfree settlement data — never hardcoded)
 * Platform net:        platformCommission - cashfreeFee - cashfreeTax
 * Vendor net:          orderAmount - platformCommission - cashfreeFee - cashfreeTax
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FinancialCalculationService {

    private static final String GLOBAL_CONFIG_ID = "global";

    private final CashfreePlatformConfigRepository platformConfigRepository;

    /**
     * Result of a financial calculation for a single order.
     */
    public record FinancialBreakdown(
            BigDecimal orderAmount,
            BigDecimal platformCommissionRate,
            CommissionType commissionType,
            BigDecimal platformCommissionAmount,
            BigDecimal vendorGrossShare,
            BigDecimal cashfreeFee,
            BigDecimal cashfreeTax,
            BigDecimal vendorNetAmount,
            BigDecimal platformNetAmount
    ) {}

    /**
     * Calculate financial breakdown for an order at the time of payment recording.
     * Cashfree fee and tax are ZERO at this point — they will be updated from
     * Cashfree settlement data later.
     *
     * @param orderAmount  The total amount the customer paid.
     * @param vendor       The vendor owning this order (for vendor-specific overrides).
     * @return             Immutable financial breakdown snapshot.
     */
    public FinancialBreakdown calculateForOrder(BigDecimal orderAmount, Vendor vendor) {
        CashfreePlatformConfig config = getConfig();

        CommissionType type = config.getCommissionType();
        BigDecimal rate = resolveCommissionRate(config, vendor);

        BigDecimal commissionAmount;
        if (type == CommissionType.PERCENTAGE) {
            commissionAmount = orderAmount
                    .multiply(rate)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            // FIXED — cap at order amount
            commissionAmount = rate.min(orderAmount).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal vendorGross = orderAmount.subtract(commissionAmount).setScale(2, RoundingMode.HALF_UP);

        // Cashfree fee and tax are unknown until Cashfree reports them via settlement
        BigDecimal cashfreeFee = BigDecimal.ZERO;
        BigDecimal cashfreeTax = BigDecimal.ZERO;

        BigDecimal vendorNet = vendorGross.subtract(cashfreeFee).subtract(cashfreeTax).setScale(2, RoundingMode.HALF_UP);
        BigDecimal platformNet = commissionAmount.subtract(cashfreeFee).subtract(cashfreeTax).setScale(2, RoundingMode.HALF_UP);

        log.debug("Financial calculation: order={}, rate={} ({}), commission={}, vendorGross={}",
                orderAmount, rate, type, commissionAmount, vendorGross);

        return new FinancialBreakdown(
                orderAmount,
                rate,
                type,
                commissionAmount,
                vendorGross,
                cashfreeFee,
                cashfreeTax,
                vendorNet,
                platformNet
        );
    }

    /**
     * Recalculate vendor net and platform net after Cashfree fees are known.
     * This is called when processing Cashfree settlement webhooks.
     *
     * @param orderAmount          Original customer payment.
     * @param platformCommission   Platform commission amount (snapshot from order).
     * @param cashfreeFee          Actual Cashfree gateway fee from settlement.
     * @param cashfreeTax          Actual Cashfree GST from settlement.
     * @param refundAmount         Any refund applied.
     * @return                     Updated vendor and platform net amounts.
     */
    public record SettlementBreakdown(
            BigDecimal vendorNetAmount,
            BigDecimal platformNetAmount
    ) {}

    public SettlementBreakdown recalculateAfterFees(
            BigDecimal orderAmount,
            BigDecimal platformCommission,
            BigDecimal cashfreeFee,
            BigDecimal cashfreeTax,
            BigDecimal refundAmount) {

        BigDecimal cf = cashfreeFee != null ? cashfreeFee : BigDecimal.ZERO;
        BigDecimal tax = cashfreeTax != null ? cashfreeTax : BigDecimal.ZERO;
        BigDecimal refund = refundAmount != null ? refundAmount : BigDecimal.ZERO;

        BigDecimal vendorNet = orderAmount
                .subtract(platformCommission)
                .subtract(cf)
                .subtract(tax)
                .subtract(refund)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal platformNet = platformCommission
                .subtract(cf)
                .subtract(tax)
                .setScale(2, RoundingMode.HALF_UP);

        return new SettlementBreakdown(vendorNet, platformNet);
    }

    /**
     * Calculate Cashfree split amounts for Easy Split.
     * vendor gets their gross share; platform gets commission.
     * Cashfree fees are deducted by Cashfree automatically before settlement.
     *
     * @param orderAmount         Total customer payment to Cashfree.
     * @param platformCommission  Platform commission amount.
     * @return                    Split amounts: [vendorAmount, platformAmount]
     */
    public BigDecimal[] calculateSplitAmounts(BigDecimal orderAmount, BigDecimal platformCommission) {
        BigDecimal vendorAmount = orderAmount.subtract(platformCommission).setScale(2, RoundingMode.HALF_UP);
        BigDecimal platformAmount = platformCommission.setScale(2, RoundingMode.HALF_UP);
        return new BigDecimal[]{vendorAmount, platformAmount};
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private CashfreePlatformConfig getConfig() {
        return platformConfigRepository.findById(GLOBAL_CONFIG_ID)
                .orElseGet(() -> CashfreePlatformConfig.builder()
                        .id(GLOBAL_CONFIG_ID)
                        .commissionType(CommissionType.PERCENTAGE)
                        .commissionRate(new BigDecimal("5.00"))
                        .build());
    }

    /**
     * Resolve effective commission rate — vendor-specific overrides global config.
     */
    private BigDecimal resolveCommissionRate(CashfreePlatformConfig config, Vendor vendor) {
        if (vendor != null && vendor.getSettings() != null
                && vendor.getSettings().getPlatformFeePercentage() != null
                && vendor.getSettings().getPlatformFeePercentage().compareTo(BigDecimal.ZERO) > 0) {
            // Vendor has a specific override
            return vendor.getSettings().getPlatformFeePercentage();
        }
        return config.getCommissionRate();
    }
}
