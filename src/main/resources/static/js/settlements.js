document.addEventListener('DOMContentLoaded', () => {
    // Check auth
    const token = localStorage.getItem('token');
    if (!token) {
        window.location.href = 'index.html';
        return;
    }

    // Tabs logic
    const tabs = document.querySelectorAll('.tab');
    const contents = document.querySelectorAll('.tab-content');
    
    tabs.forEach(tab => {
        tab.addEventListener('click', () => {
            tabs.forEach(t => t.classList.remove('active'));
            contents.forEach(c => c.classList.remove('active'));
            
            tab.classList.add('active');
            const target = document.getElementById(`${tab.dataset.tab}-tab`);
            if (target) target.classList.add('active');
            
            loadTabData(tab.dataset.tab);
        });
    });

    // Initial load
    loadSummaryDashboard();

    // Ledger variables
    let ledgerPage = 0;
    const ledgerSize = 20;

    // Event listeners
    document.getElementById('btnFilterLedger').addEventListener('click', () => {
        ledgerPage = 0;
        loadLedger();
    });
    
    document.getElementById('btnPrevLedger').addEventListener('click', () => {
        if (ledgerPage > 0) {
            ledgerPage--;
            loadLedger();
        }
    });
    
    document.getElementById('btnNextLedger').addEventListener('click', () => {
        ledgerPage++;
        loadLedger();
    });

    document.getElementById('btnExportLedger').addEventListener('click', () => {
        const from = document.getElementById('ledgerStart').value;
        const to = document.getElementById('ledgerEnd').value;
        if (!from || !to) {
            alert('Please select both From and To dates for export.');
            return;
        }
        
        // Use auth token for download with fetch and blob
        const token = localStorage.getItem('token');
        fetch(`${API_BASE_URL}/vendor/financial-report/export?from=${from}&to=${to}`, {
            headers: { 'Authorization': `Bearer ${token}` }
        })
        .then(res => res.blob())
        .then(blob => {
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `financial-report-${from}-to-${to}.csv`;
            document.body.appendChild(a);
            a.click();
            a.remove();
        })
        .catch(err => {
            console.error('Export failed:', err);
            alert('Failed to export report');
        });
    });

    // --- Loading functions ---

    function loadTabData(tabName) {
        if (tabName === 'dashboard') loadSummaryDashboard();
        else if (tabName === 'ledger') loadLedger();
        else if (tabName === 'history') loadHistory();
        else if (tabName === 'pending') loadPending();
    }

    async function loadSummaryDashboard() {
        try {
            const res = await api.get('/vendor/settlements/summary');
            if (res.success && res.data) {
                const d = res.data;
                document.getElementById('dashTotalSales').textContent = formatCurrency(d.totalSales);
                document.getElementById('dashCashfreeSales').textContent = formatCurrency(d.cashfreeSales);
                document.getElementById('dashOfflineSales').textContent = formatCurrency(d.offlineSales || (d.cashSales + d.offlineSales));
                
                const totalDed = (d.totalCashfreeFees || 0) + (d.totalCashfreeTax || 0) + (d.totalPlatformCharges || 0);
                document.getElementById('dashTotalDeductions').textContent = formatCurrency(totalDed);
                document.getElementById('dashPlatformFee').textContent = formatCurrency(d.totalPlatformCharges);
                document.getElementById('dashCFFee').textContent = formatCurrency(d.totalCashfreeFees);
                document.getElementById('dashCFTax').textContent = formatCurrency(d.totalCashfreeTax);
                
                document.getElementById('dashSettled').textContent = formatCurrency(d.alreadySettled);
                document.getElementById('dashSettledTill').textContent = d.settledTillDate ? `Settled Till: ${formatDate(d.settledTillDate)}` : 'Never settled';
                
                document.getElementById('dashPending').textContent = formatCurrency(d.pendingSettlement);
                document.getElementById('dashPendingFrom').textContent = d.pendingFrom ? `Pending From: ${formatDate(d.pendingFrom)}` : 'Pending From: -';
                
                document.getElementById('dashNetEarnings').textContent = formatCurrency(d.totalVendorEarnings);
            }
        } catch (error) {
            console.error('Failed to load summary:', error);
        }
    }

    async function loadLedger() {
        try {
            const from = document.getElementById('ledgerStart').value;
            const to = document.getElementById('ledgerEnd').value;
            let url = `/vendor/transactions?page=${ledgerPage}&size=${ledgerSize}`;
            if (from) url += `&from=${from}`;
            if (to) url += `&to=${to}`;

            const res = await api.get(url);
            const tbody = document.getElementById('ledgerTableBody');
            tbody.innerHTML = '';
            
            if (res.success && res.data && res.data.content) {
                const data = res.data;
                if (data.content.length === 0) {
                    tbody.innerHTML = `<tr><td colspan="8" class="text-center text-muted">No transactions found</td></tr>`;
                } else {
                    data.content.forEach(tx => {
                        const cfDed = (tx.cashfreeFee || 0) + (tx.cashfreeTax || 0);
                        const statusBadge = getStatusBadge(tx.settlementStatus);
                        const sourceBadge = getSourceBadge(tx.paymentSource);
                        
                        const tr = document.createElement('tr');
                        tr.innerHTML = `
                            <td>${formatDateTime(tx.orderDate || tx.recordedAt)}</td>
                            <td>${tx.queueNumber || '-'} <br><small class="text-muted">${tx.orderId.substring(0,8)}...</small></td>
                            <td>${sourceBadge}</td>
                            <td>${formatCurrency(tx.orderAmount)}</td>
                            <td class="text-danger">-${formatCurrency(cfDed)}</td>
                            <td class="text-danger">-${formatCurrency(tx.platformFeeAmount)}</td>
                            <td style="font-weight:bold; color:var(--primary)">${formatCurrency(tx.vendorNetAmount)}</td>
                            <td>${statusBadge}</td>
                        `;
                        tbody.appendChild(tr);
                    });
                }
                
                document.getElementById('ledgerPageInfo').textContent = `Page ${data.page + 1} of ${data.totalPages} (${data.totalElements} total)`;
                document.getElementById('btnPrevLedger').disabled = data.page === 0;
                document.getElementById('btnNextLedger').disabled = data.last;
            }
        } catch (error) {
            console.error('Failed to load ledger:', error);
        }
    }

    async function loadHistory() {
        try {
            const res = await api.get('/vendor/settlements');
            const tbody = document.getElementById('historyTableBody');
            tbody.innerHTML = '';
            
            if (res.success && res.data && res.data.content) {
                if (res.data.content.length === 0) {
                    tbody.innerHTML = `<tr><td colspan="8" class="text-center text-muted">No settlements history</td></tr>`;
                } else {
                    res.data.content.forEach(s => {
                        const ded = (s.totalCashfreeFees || 0) + (s.totalCashfreeTax || 0) + (s.totalPlatformCharges || 0) + (s.totalRefunds || 0);
                        const tr = document.createElement('tr');
                        tr.innerHTML = `
                            <td><strong>${s.settlementRef}</strong></td>
                            <td>${formatDate(s.periodFrom)} to ${formatDate(s.periodTo)}</td>
                            <td>${s.orderCount}</td>
                            <td>${formatCurrency(s.totalSales)}</td>
                            <td class="text-danger">-${formatCurrency(ded)}</td>
                            <td style="font-weight:bold; color:var(--primary)">${formatCurrency(s.netSettlementAmount)}</td>
                            <td>${getStatusBadge(s.settlementStatus)}</td>
                            <td>
                                <button class="btn btn-secondary btn-sm" onclick="viewSettlementDetail('${s.id}')">View Details</button>
                            </td>
                        `;
                        tbody.appendChild(tr);
                    });
                }
            }
        } catch (error) {
            console.error('Failed to load history:', error);
        }
    }

    async function loadPending() {
        try {
            const res = await api.get('/vendor/settlements/pending');
            const tbody = document.getElementById('pendingTableBody');
            tbody.innerHTML = '';
            
            if (res.success && res.data) {
                const d = res.data;
                document.getElementById('pendingOverviewText').textContent = d.pendingFrom ? `Pending from: ${formatDate(d.pendingFrom)}` : 'No pending settlements';
                document.getElementById('pendingTotalAmount').textContent = formatCurrency(d.pendingAmount);
                document.getElementById('pendingOrderCount').textContent = d.pendingOrderCount;
                document.getElementById('pendingGross').textContent = formatCurrency(d.grossSales);
                
                const totalFees = (d.cashfreeFees || 0) + (d.cashfreeTax || 0) + (d.platformCharges || 0) + (d.refunds || 0);
                document.getElementById('pendingFees').textContent = `-${formatCurrency(totalFees)}`;
                
                if (!d.pendingTransactions || d.pendingTransactions.length === 0) {
                    tbody.innerHTML = `<tr><td colspan="6" class="text-center text-muted">No pending transactions</td></tr>`;
                } else {
                    d.pendingTransactions.forEach(tx => {
                        const sourceBadge = getSourceBadge(tx.paymentSource);
                        const ded = (tx.cashfreeFee || 0) + (tx.cashfreeTax || 0) + (tx.platformFeeAmount || 0);
                        
                        const tr = document.createElement('tr');
                        tr.innerHTML = `
                            <td>${formatDateTime(tx.orderDate || tx.recordedAt)}</td>
                            <td>${tx.queueNumber || '-'}</td>
                            <td>${sourceBadge}</td>
                            <td>${formatCurrency(tx.orderAmount)}</td>
                            <td class="text-danger">-${formatCurrency(ded)}</td>
                            <td style="font-weight:bold;">${formatCurrency(tx.vendorNetAmount)}</td>
                        `;
                        tbody.appendChild(tr);
                    });
                }
            }
        } catch (error) {
            console.error('Failed to load pending:', error);
        }
    }

    // Exported to window for onclick handlers
    window.viewSettlementDetail = async function(id) {
        try {
            const res = await api.get(`/vendor/settlements/${id}`);
            if (res.success && res.data) {
                const s = res.data;
                document.getElementById('modalSettlementRef').textContent = `Settlement ${s.settlementRef}`;
                document.getElementById('modalSettlementPeriod').textContent = `Period: ${formatDate(s.periodFrom)} to ${formatDate(s.periodTo)}`;
                
                document.getElementById('modalTotalSales').textContent = formatCurrency(s.totalSales);
                document.getElementById('modalCFSales').textContent = formatCurrency(s.cashfreeSales);
                document.getElementById('modalOffSales').textContent = formatCurrency(s.offlineSales);
                
                const ded = (s.totalCashfreeFees || 0) + (s.totalCashfreeTax || 0) + (s.totalPlatformCharges || 0);
                document.getElementById('modalDeductions').textContent = `-${formatCurrency(ded)}`;
                document.getElementById('modalPlatFee').textContent = formatCurrency(s.totalPlatformCharges);
                document.getElementById('modalGateFee').textContent = formatCurrency((s.totalCashfreeFees||0) + (s.totalCashfreeTax||0));
                
                document.getElementById('modalNet').textContent = formatCurrency(s.netSettlementAmount);
                document.getElementById('modalStatus').innerHTML = getStatusBadge(s.settlementStatus);
                
                const tbody = document.getElementById('modalTxnBody');
                tbody.innerHTML = '';
                if (s.transactions && s.transactions.length > 0) {
                    s.transactions.forEach(tx => {
                        const tr = document.createElement('tr');
                        tr.innerHTML = `
                            <td>${tx.queueNumber || '-'} <br><small class="text-muted">${formatDate(tx.orderDate||tx.recordedAt)}</small></td>
                            <td>${getSourceBadge(tx.paymentSource)}</td>
                            <td>${formatCurrency(tx.orderAmount)}</td>
                            <td style="font-weight:bold;">${formatCurrency(tx.vendorNetAmount)}</td>
                        `;
                        tbody.appendChild(tr);
                    });
                } else {
                    tbody.innerHTML = `<tr><td colspan="4" class="text-center text-muted">No transactions data</td></tr>`;
                }
                
                document.getElementById('settlementModal').style.display = 'flex';
            }
        } catch (error) {
            console.error('Failed to load detail:', error);
        }
    }

    // --- Formatters ---
    
    function formatCurrency(val) {
        if(val === null || val === undefined) return '₹0.00';
        return `₹${Number(val).toFixed(2)}`;
    }
    
    function formatDate(dateStr) {
        if(!dateStr) return '-';
        return new Date(dateStr).toLocaleDateString();
    }
    
    function formatDateTime(dateStr) {
        if(!dateStr) return '-';
        const d = new Date(dateStr);
        return `${d.toLocaleDateString()} ${d.toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}`;
    }
    
    function getStatusBadge(status) {
        if(status === 'PENDING') return `<span class="badge badge-pending">PENDING</span>`;
        if(status === 'SETTLED') return `<span class="badge badge-settled">SETTLED</span>`;
        return `<span class="badge" style="background:#e9ecef;color:#495057">${status || 'UNKNOWN'}</span>`;
    }
    
    function getSourceBadge(source) {
        if(source === 'CASHFREE') return `<span class="badge badge-cashfree">CASHFREE</span>`;
        if(source === 'CASH' || source === 'OFFLINE') return `<span class="badge badge-offline">${source}</span>`;
        return `<span class="badge" style="background:#e9ecef">${source || '-'}</span>`;
    }
});
