import re
with open('C:/DeQueue/src/main/resources/static/js/orders.js', 'r', encoding='utf-8') as f:
    content = f.read()

new_code = r"""
    let customFieldsHtml = '';
    const displayCustomFields = order.customFields || {};
    if (Object.keys(displayCustomFields).length > 0) {
        customFieldsHtml = '<div class="details-section" style="margin-top: 1rem;"><div class="details-section-title">Order Information</div>';
        for (const [k, v] of Object.entries(displayCustomFields)) {
            let label = k.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase());
            if (ordersApp.settings && ordersApp.settings.customFields) {
                const cfDef = ordersApp.settings.customFields.find(f => f.id === k);
                if (cfDef && cfDef.label) label = cfDef.label;
            }
            customFieldsHtml += `<div style="display:flex; justify-content:space-between; padding: 4px 0; border-bottom: 1px solid #f1f5f9; font-size: 0.9rem;">
                <span style="color: #64748b;">${this.esc(label)}</span>
                <span style="font-weight: 500;">${this.esc(v)}</span>
            </div>`;
        }
        customFieldsHtml += '</div>';
    }

    document.getElementById('order-modal-title').innerHTML = `Order #${this.esc(order.queueNumber)} ${this.statusPill(order.status)}`;
    document.getElementById('order-modal-body').innerHTML = `
      <div class="order-details-body">
        <div class="details-hero">
          <div><div class="details-table-number">${this.esc(table || '-')}</div><div class="details-meta">${this.esc(this.orderTypeLabel(order))} &bull; Placed ${this.waitingLabel(order)} ago</div></div>
          ${this.statusPill(order.status)}
        </div>
        <div class="details-section"><div class="details-section-title">Items (${this.itemCount(order)})</div>${itemHtml}</div>
        ${customFieldsHtml}
        ${order.customerNote ? `<div class="details-note"><strong>Customer note</strong><br>${this.esc(order.customerNote)}</div>` : ''}
        ${price}${billActions}
      </div>`;
"""

pattern = re.compile(r'document\.getElementById\(\'order-modal-title\'\)\.innerHTML = `Order #\$\{this\.esc\(order\.queueNumber\)\} \$\{this\.statusPill\(order\.status\)\}`;.*?</div>`;', re.DOTALL)
content = pattern.sub(lambda m: new_code.strip(), content)

with open('C:/DeQueue/src/main/resources/static/js/orders.js', 'w', encoding='utf-8') as f:
    f.write(content)
