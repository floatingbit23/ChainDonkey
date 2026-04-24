/**
 * GLOBAL STATE
 * currentBlocks: Array to store the full blockchain locally
 * currentPage: Index for the pagination
 * pageSize: Number of blocks to display per page
 */
let currentBlocks = [];
let currentPage = 0;
const pageSize = 20;

/**
 * DATA POLLING
 * Efficiently fetches the entire blockchain from the Javalin API.
 * Only triggers a re-render if the hash or content has changed.
 */
async function fetchBlockchain() {
    try {
        const response = await fetch('/api/blockchain');
        const data = await response.json();
        
        // Content-based comparison to avoid unnecessary DOM updates
        if (JSON.stringify(data) !== JSON.stringify(currentBlocks)) {
            currentBlocks = data;
            renderBlockchain();
        }
    } catch (err) {
        console.error("Failed to fetch blockchain:", err);
    }
}

/**
 * MAIN RENDERING ENGINE
 * Transforms the raw blockchain array into interactive DOM elements.
 * Handles pagination logic and newest-first sorting.
 */
function renderBlockchain() {
    const container = document.getElementById('blockchain-view');
    const totalBlocks = currentBlocks.length;
    const totalPages = Math.ceil(totalBlocks / pageSize);

    // Update real-time stats (Time, Supply) independently
    updateStats(currentBlocks);

    // PAGINATION LOGIC
    const paginationEl = document.getElementById('pagination-controls');
    if (totalBlocks > pageSize) {
        paginationEl.style.display = 'flex';
        document.getElementById('page-info').textContent = `Page ${currentPage + 1} of ${totalPages}`;
        document.getElementById('prev-btn').disabled = currentPage === 0;
        document.getElementById('next-btn').disabled = currentPage >= totalPages - 1;
    } else {
        paginationEl.style.display = 'none';
    }

    container.innerHTML = '';

    // SLICING FOR VIEWPORT (Reverse clone to keep the actual chain order intact)
    const reversedAll = currentBlocks.slice().reverse();
    const pageBlocks = reversedAll.slice(currentPage * pageSize, (currentPage + 1) * pageSize);

    // ELEMENT GENERATION
    pageBlocks.forEach((block, index) => {
        // Calculate original index to keep labels correct (Genesis = Block #0)
        const actualIndex = totalBlocks - 1 - (index + (currentPage * pageSize));
        
        const card = document.createElement('div');
        card.className = 'block-card';
        card.onclick = () => showBlockDetails(block, actualIndex);

        const timeStr = new Date(block.timeStamp).toLocaleTimeString();
        
        // Calculate Mining Time (diff with previous block)
        let mineTimeStr = "";
        if (actualIndex > 0) {
            const prevBlock = currentBlocks[actualIndex - 1];
            const diffInSeconds = ((block.timeStamp - prevBlock.timeStamp) / 1000).toFixed(2);
            mineTimeStr = ` (${diffInSeconds}s)`;
        } else {
            mineTimeStr = " (Genesis)";
        }

        // PROFESSIONAL METRICS
        const confirmations = totalBlocks - actualIndex;
        const blockSize = (block.transactions.length * 284).toLocaleString(); // Estimated bytes (avg transaction size)

        card.innerHTML = `
            <div class="block-header">
                <div style="display:flex; flex-direction:column; gap:2px">
                   <span class="block-index">Block #${actualIndex}</span>
                   <span style="font-size:0.6rem; color:var(--success); font-weight:700">${confirmations} CONFIRMATIONS</span>
                </div>
                <span class="block-time">${timeStr}${mineTimeStr}</span>
            </div>
            
            <div class="data-label">Block Hash</div>
            <div class="data-value">${block.hash}</div>
            
            <div style="display:grid; grid-template-columns: 1fr 1fr; gap: 1rem">
                <div>
                   <div class="data-label">Logical Size</div>
                   <div class="data-value">${blockSize} B</div>
                </div>
                <div>
                   <div class="data-label">Transactions</div>
                   <div class="data-value">${block.transactions.length} TX</div>
                </div>
            </div>

            <div class="data-label">Previous Hash</div>
            <div class="data-value">${block.previousHash}</div>
        `;
        container.appendChild(card);
    });

    if (totalBlocks === 0) {
        container.innerHTML = '<div class="empty-msg">Waiting for node synchronization...</div>';
    }
}

/** 
 * PAGINATION HANDLER 
 */
function changePage(direction) {
    currentPage += direction;
    renderBlockchain();
    window.scrollTo({ top: 0, behavior: 'smooth' }); // Visual Feedback
}

/**
 * ADDRESS FORMATTER
 * Truncates long Public Key strings for UI cleanliness.
 */
function truncateAddress(addr) {
    if (!addr) return "Genesis / Coinbase";
    if (addr.length < 16) return addr;
    return addr.substring(0, 8) + "..." + addr.substring(addr.length - 8);
}

/**
 * MODAL LOGIC
 * Populates and opens the transaction detail view for a specific block.
 */
function showBlockDetails(block, index) {
    const overlay = document.getElementById('modal-overlay');
    const content = document.getElementById('modal-content');
    overlay.style.display = 'flex';

    // Generate detailed transaction cards with From/To info
    let txHtml = block.transactions.map(tx => `
        <div class="tx-card">
            <div class="data-label">Transaction ID</div>
            <div class="data-value">${tx.transactionId}</div>
            
            <div class="address-box">
                <span class="address-label">From:</span>
                <span class="address-value" onclick="showWalletDetails('${tx.senderPK}')" title="${tx.senderPK || 'Genesis' }">Wallet ${truncateAddress(tx.senderPK)}</span>
            </div>

            <div class="address-box">
                <span class="address-label">To:</span>
                <span class="address-value" onclick="showWalletDetails('${tx.recipientPK}')" title="${tx.recipientPK}">Wallet ${truncateAddress(tx.recipientPK)}</span>
            </div>

            <div style="display:flex; justify-content:space-between; margin-top:1rem">
                <div>
                    <div class="data-label">Amount</div>
                    <div style="font-size:1.5rem; font-weight:800; color:var(--secondary)">${tx.value} Donks</div>
                </div>
                <div style="text-align:right">
                    <div class="data-label">Status</div>
                    <div style="color:var(--success); font-weight:600">✓ Verified</div>
                </div>
            </div>
        </div>
    `).join('');

    content.innerHTML = `
        <h2 style="font-size:2rem; margin-bottom:0.5rem">Block #${index} Details</h2>
        <p style="color:var(--text-dim); margin-bottom:2rem">Nounce: ${block.nonce} | Difficulty: ${block.hash.substring(0, 10).replace(/[^0]/g, '').length} (Leading Zeros)</p>
        
        <h3>Transactions</h3>
        <div class="tx-list">${txHtml || '<p class="empty-msg">No transactions in this block</p>'}</div>
    `;
}

/**
 * WALLET EXPLORER LOGIC
 * Aggregates all transactions and calculates the current balance for a PK.
 */
function showWalletDetails(pk) {
    // VALIDATION: A wallet public key (Base64 ECDSA) is always significantly longer than 
    // a 64-character SHA-256 hash. We block any string that doesn't meet this criteria.
    if (!pk || pk === "0" || pk.length <= 64) {
        console.warn("[UI] Attempted to query balance of a non-wallet hash:", pk);
        return; 
    }
    
    const overlay = document.getElementById('modal-overlay');
    const content = document.getElementById('modal-content');
    overlay.style.display = 'flex';

    let balance = 0;
    let history = [];

    // Scan entire chain for interactions
    currentBlocks.forEach((block, bIdx) => {
        block.transactions.forEach(tx => {
            if (tx.senderPK === pk || tx.recipientPK === pk) {
                const isIncoming = tx.recipientPK === pk;
                const val = parseFloat(tx.value);
                
                if (isIncoming) balance += val;
                else balance -= val;

                history.push({
                    type: isIncoming ? 'INCOMING' : 'OUTGOING',
                    amount: val,
                    txId: tx.transactionId,
                    time: new Date(block.timeStamp).toLocaleString(),
                    blockIndex: bIdx
                });
            }
        });
    });

    // Sort history newest first
    history.reverse();

    const historyHtml = history.map(h => `
        <div class="history-item ${h.type.toLowerCase()}">
            <div class="history-meta">
                <span class="history-type">${h.type} • Block #${h.blockIndex}</span>
                <span class="history-time">${h.time}</span>
            </div>
            <div class="history-value ${h.type === 'INCOMING' ? 'val-plus' : 'val-minus'}">
                ${h.type === 'INCOMING' ? '+' : '-'}${h.amount.toFixed(2)}
            </div>
        </div>
    `).join('');

    content.innerHTML = `
        <div class="wallet-header">
            <h2 style="font-size:1.2rem; color:var(--primary); text-transform:uppercase">Wallet Profile</h2>
            <div class="data-value" style="margin-top:0.5rem; word-break:break-all; font-size:0.7rem">${pk}</div>
            
            <div class="balance-badge">
                <div class="stat-label">Current Balance</div>
                <div class="balance-amount">${balance.toFixed(2)} <span class="balance-unit">DONKS</span></div>
            </div>
        </div>

        <h3 style="margin-bottom:1rem">Transaction History</h3>
        <div class="history-container">
            ${historyHtml || '<p class="empty-msg">No activity recorded for this address</p>'}
        </div>
    `;
}

/** 
 * CLOSE MODAL HANDLER 
 */
function closeModal() {
    document.getElementById('modal-overlay').style.display = 'none';
}

/**
 * NETWORK STATISTICS CALCULATOR
 * Dynamically computes the health and supply metrics from the current chain state.
 */
function updateStats(data) {
    if (!data || data.length === 0) return;

    // 1. Calculate Average Block Time (Mean Interval)
    let totalInterval = 0;
    let intervalCount = 0;
    for (let i = 1; i < data.length; i++) {
        const diff = (data[i].timeStamp - data[i-1].timeStamp);
        if (diff > 0) {
            totalInterval += diff;
            intervalCount++;
        }
    }
    const avg = intervalCount > 0 ? (totalInterval / intervalCount / 1000).toFixed(2) : "0.00";
    document.getElementById('stat-block-time').innerText = avg;

    // 2. Circulating Supply
    // Based on the first transaction value of the genesis block (The Source)
    let supply = 0;
    if (data.length > 0 && data[0].transactions && data[0].transactions.length > 0) {
        supply = data[0].transactions[0].value;
    }
    document.getElementById('stat-supply').innerText = supply.toFixed(2);
}

/**
 * KADEMLIA NETWORK STATS
 */
async function fetchKadStats() {
    try {
        const response = await fetch('/api/kad');
        const data = await response.json();
        
        const localIdEl = document.getElementById('kad-local-id');
        const totalNodesEl = document.getElementById('kad-total-nodes');
        
        if (localIdEl) localIdEl.textContent = data.localId;
        if (totalNodesEl) totalNodesEl.textContent = data.totalNodes;
        
        renderBuckets(data.buckets);
    } catch (err) {
        console.error("Failed to fetch Kad stats:", err);
    }
}

function renderBuckets(buckets) {
    const container = document.getElementById('kad-buckets-view');
    if (!container || !buckets) return;
    
    container.innerHTML = '';
    buckets.forEach((size, index) => {
        if (size > 0) {
            const dot = document.createElement('div');
            dot.className = 'bucket-indicator';
            dot.title = `Bucket ${index}: ${size} nodes`;
            // Visual mapping: more nodes = brighter/larger
            dot.style.opacity = Math.min(1, 0.3 + (size / 10));
            container.appendChild(dot);
        }
    });
}

/** 
 * INITIALIZATION 
 * Polls for a highly responsive feel.
 */
setInterval(fetchBlockchain, 3000);
setInterval(fetchKadStats, 5000);
fetchBlockchain();
fetchKadStats();
