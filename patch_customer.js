const fs = require('fs');

const original = fs.readFileSync('original_customer.js', 'utf8');
const current = fs.readFileSync('src/main/resources/static/js/customer.js', 'utf8');

// Extract updateCartUI through renderCartModal from original
const startMarker = '  updateCartUI() {';
const endMarker = '  async placeOrder() {';

const originalCartBlock = original.substring(
    original.indexOf(startMarker),
    original.indexOf(endMarker)
);

// We want to replace the "Payment Method Section" in originalCartBlock
// It looks like:
//       <!-- Payment Method Section -->
//       ...
//       </div>
//
//       <div id="cart-custom-fields"
const paymentSectionStart = originalCartBlock.indexOf('<!-- Payment Method Section -->');
const paymentSectionEnd = originalCartBlock.indexOf('<div id="cart-custom-fields"');

let newCartBlock = originalCartBlock;
if (paymentSectionStart !== -1 && paymentSectionEnd !== -1) {
    const replacement = `<!-- Payment Method Section -->
      <div class="card mt-3 mb-3" style="padding:1rem;background:rgba(99,102,241,.06);border:1px solid rgba(99,102,241,.2);">
        <div class="flex justify-between items-center mb-2">
          <span class="font-bold" style="font-size:.9rem;">Amount to Pay</span>
          <span class="font-bold text-primary" style="font-size:1.2rem;">\${this.formatPrice(finalTotal)}</span>
        </div>
        <div style="font-size:.85rem;color:var(--text-muted);margin-bottom:.75rem;">Payment Method</div>
        <select id="pay-method-select" class="form-control" style="width: 100%; font-weight: 600; border: 1px solid var(--border); padding: 0.75rem; border-radius: 8px; cursor: pointer;">
          <option value="OFFLINE">Counter (Cash/Card)</option>
          <option value="CASHFREE">Pay Online (UPI / Cards / NetBanking)</option>
        </select>
      </div>

      `;
    newCartBlock = originalCartBlock.substring(0, paymentSectionStart) + replacement + originalCartBlock.substring(paymentSectionEnd);
}

// Now replace the block in current customer.js
const currentStart = current.indexOf(startMarker);
const currentEnd = current.indexOf(endMarker);

if (currentStart === -1 || currentEnd === -1) {
    console.error("Could not find markers in current file");
    process.exit(1);
}

// In the newCartBlock, the place order button was original, let's keep it that way.
// We just need to make sure the dropdown logic exists.

const newCurrent = current.substring(0, currentStart) + newCartBlock + current.substring(currentEnd);
fs.writeFileSync('src/main/resources/static/js/customer.js', newCurrent);
console.log("Successfully patched customer.js");
