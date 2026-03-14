/**
 * CryptoTrack Analytics - Frontend Application
 *
 * This script handles:
 * 1. Fetching data from our Java Spring Boot REST API
 * 2. Rendering the market overview table
 * 3. Drawing price history charts (Chart.js)
 * 4. Rendering market cap donut chart
 * 5. Showing top gainers & losers
 * 6. Auto-refreshing every 5 minutes
 */

// =============================================
//  CONFIGURATION
// =============================================
const API_BASE = '/api';
const AUTO_REFRESH_MS = 5 * 60 * 1000; // 5 minutes

// Chart.js instance references
let priceChartInstance = null;
let donutChartInstance = null;

// Coin data cache
let allCoins = [];

// =============================================
//  INITIALIZATION
// =============================================
document.addEventListener('DOMContentLoaded', () => {
    loadDashboard();

    // Auto-refresh every 5 minutes
    setInterval(loadDashboard, AUTO_REFRESH_MS);
});

/**
 * Loads all dashboard components in parallel.
 */
async function loadDashboard() {
    await Promise.all([
        loadMarketStats(),
        loadCoins(),
        loadGainers(),
        loadLosers(),
    ]);
}

// =============================================
//  MARKET STATS BANNER
// =============================================
async function loadMarketStats() {
    try {
        const data = await fetchJSON(`${API_BASE}/market/stats`);
        if (!data) return;

        document.getElementById('totalMarketCap').textContent = formatLargeNumber(data.totalMarketCap, '$');
        document.getElementById('totalVolume').textContent = formatLargeNumber(data.totalVolume24h, '$');
        document.getElementById('btcDominance').textContent =
            (data.btcDominanceLive || data.btcDominance || 0).toFixed(1) + '%';
        document.getElementById('gainersLosers').innerHTML =
            `<span style="color:var(--green)">${data.gainers24h}</span> / <span style="color:var(--red)">${data.losers24h}</span>`;
        document.getElementById('dataPoints').textContent =
            Number(data.totalDataPoints).toLocaleString();

        updateTimestamp();
    } catch (e) {
        console.error('Stats error:', e);
    }
}

// =============================================
//  MARKET TABLE
// =============================================
async function loadCoins() {
    try {
        const coins = await fetchJSON(`${API_BASE}/coins`);
        if (!coins || coins.length === 0) {
            showTableError('No data available. The pipeline is fetching data...');
            return;
        }

        allCoins = coins;
        renderTable(coins);
        populateChartSelector(coins);
        renderDonutChart(coins);

    } catch (e) {
        showTableError('Failed to load market data.');
        console.error('Coins error:', e);
    }
}

/**
 * Renders the market overview table.
 */
function renderTable(coins) {
    const tbody = document.getElementById('coinsTableBody');

    if (coins.length === 0) {
        tbody.innerHTML = `<tr><td colspan="9" style="text-align:center;padding:40px;color:var(--text-muted);">No results found.</td></tr>`;
        return;
    }

    tbody.innerHTML = coins.map(coin => {
        const ch1h = coin.priceChangePercentage1h;
        const ch24h = coin.priceChangePercentage24h;
        const ch7d = coin.priceChangePercentage7d;

        return `
        <tr onclick="selectCoinForChart('${coin.id}')" title="Click to view ${coin.name} chart">
            <td class="rank-cell">${coin.marketCapRank || '—'}</td>
            <td>
                <div class="coin-cell">
                    <img class="coin-icon" src="${coin.imageUrl || ''}" alt="${coin.name}" 
                         onerror="this.style.display='none'" loading="lazy"/>
                    <div>
                        <div class="coin-name">${coin.name}</div>
                        <div class="coin-symbol">${coin.symbol}</div>
                    </div>
                </div>
            </td>
            <td class="price-cell">${formatPrice(coin.currentPrice)}</td>
            <td>${formatChange(ch1h)}</td>
            <td>${formatChange(ch24h)}</td>
            <td>${formatChange(ch7d)}</td>
            <td class="price-cell">${formatLargeNumber(coin.marketCap, '$')}</td>
            <td class="price-cell">${formatLargeNumber(coin.totalVolume, '$')}</td>
            <td class="price-cell">${formatPrice(coin.ath)}</td>
        </tr>`;
    }).join('');
}

/**
 * Filters the coin table based on search input.
 */
function filterCoins() {
    const query = document.getElementById('searchInput').value.toLowerCase();
    const filtered = allCoins.filter(c =>
        c.name.toLowerCase().includes(query) ||
        c.symbol.toLowerCase().includes(query) ||
        c.id.toLowerCase().includes(query)
    );
    renderTable(filtered);
}

function showTableError(msg) {
    document.getElementById('coinsTableBody').innerHTML =
        `<tr><td colspan="9" style="text-align:center;padding:40px;color:var(--text-muted);">${msg}</td></tr>`;
}

// =============================================
//  PRICE HISTORY CHART
// =============================================
function populateChartSelector(coins) {
    const select = document.getElementById('chartCoinSelect');
    const current = select.value;
    select.innerHTML = '<option value="">Select a coin...</option>' +
        coins.map(c => `<option value="${c.id}" ${c.id === current ? 'selected' : ''}>${c.name}</option>`).join('');

    // Auto-select bitcoin by default
    if (!current && coins.length > 0) {
        const btc = coins.find(c => c.id === 'bitcoin') || coins[0];
        select.value = btc.id;
        loadCoinChart();
    }
}

function selectCoinForChart(coinId) {
    document.getElementById('chartCoinSelect').value = coinId;
    loadCoinChart();
    // Scroll to chart on mobile
    document.querySelector('.card-chart').scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

async function loadCoinChart() {
    const coinId = document.getElementById('chartCoinSelect').value;
    const placeholder = document.getElementById('chartPlaceholder');

    if (!coinId) {
        placeholder.classList.remove('hidden');
        if (priceChartInstance) { priceChartInstance.destroy(); priceChartInstance = null; }
        return;
    }

    try {
        const history = await fetchJSON(`${API_BASE}/coins/${coinId}/history?days=7`);

        if (!history || history.length === 0) {
            placeholder.innerHTML = '<span>📡</span><p>No history yet. Data is being collected...</p>';
            placeholder.classList.remove('hidden');
            return;
        }

        placeholder.classList.add('hidden');

        // Prepare chart data
        const labels = history.map(h => {
            const d = new Date(h.recordedAt);
            return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
        });
        const prices = history.map(h => h.price);

        // Determine trend color
        const isUp = prices.length > 1 && prices[prices.length - 1] >= prices[0];
        const lineColor = isUp ? '#22c55e' : '#ef4444';
        const gradientColor = isUp ? 'rgba(34, 197, 94, 0.12)' : 'rgba(239, 68, 68, 0.12)';

        // Destroy previous chart
        if (priceChartInstance) priceChartInstance.destroy();

        const ctx = document.getElementById('priceChart').getContext('2d');

        // Create gradient fill
        const gradient = ctx.createLinearGradient(0, 0, 0, 180);
        gradient.addColorStop(0, isUp ? 'rgba(34,197,94,0.3)' : 'rgba(239,68,68,0.3)');
        gradient.addColorStop(1, 'rgba(0,0,0,0)');

        // Get coin name for label
        const coinName = allCoins.find(c => c.id === coinId)?.name || coinId;

        priceChartInstance = new Chart(ctx, {
            type: 'line',
            data: {
                labels,
                datasets: [{
                    label: `${coinName} Price (USD)`,
                    data: prices,
                    borderColor: lineColor,
                    borderWidth: 2,
                    backgroundColor: gradient,
                    fill: true,
                    tension: 0.4,
                    pointRadius: history.length > 20 ? 0 : 3,
                    pointHoverRadius: 5,
                    pointBackgroundColor: lineColor,
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                interaction: { mode: 'index', intersect: false },
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        backgroundColor: '#131b2b',
                        borderColor: '#1e2d47',
                        borderWidth: 1,
                        titleColor: '#7a90b5',
                        bodyColor: '#e8f0ff',
                        bodyFont: { family: 'JetBrains Mono', size: 12 },
                        callbacks: {
                            label: ctx => ` $${ctx.parsed.y.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 6 })}`
                        }
                    }
                },
                scales: {
                    x: {
                        grid: { color: '#1e2d47', drawBorder: false },
                        ticks: {
                            color: '#4a5f80',
                            font: { size: 10 },
                            maxTicksLimit: 6,
                            maxRotation: 0,
                        }
                    },
                    y: {
                        grid: { color: '#1e2d47' },
                        ticks: {
                            color: '#4a5f80',
                            font: { family: 'JetBrains Mono', size: 10 },
                            callback: val => '$' + formatShortNumber(val)
                        }
                    }
                }
            }
        });

    } catch (e) {
        console.error('Chart error:', e);
    }
}

// =============================================
//  DONUT CHART (Market Cap Distribution)
// =============================================
function renderDonutChart(coins) {
    const top7 = coins.slice(0, 7);
    const labels = top7.map(c => c.name);
    const data = top7.map(c => c.marketCap || 0);

    const colors = [
        '#3b82f6', '#8b5cf6', '#06b6d4', '#22c55e',
        '#f97316', '#ec4899', '#eab308'
    ];

    if (donutChartInstance) donutChartInstance.destroy();

    const ctx = document.getElementById('donutChart').getContext('2d');
    donutChartInstance = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels,
            datasets: [{
                data,
                backgroundColor: colors,
                borderColor: '#0f1623',
                borderWidth: 3,
                hoverOffset: 8,
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            cutout: '68%',
            plugins: {
                legend: {
                    position: 'right',
                    labels: {
                        color: '#7a90b5',
                        font: { size: 11, family: 'Inter' },
                        padding: 12,
                        boxWidth: 10,
                        boxHeight: 10,
                    }
                },
                tooltip: {
                    backgroundColor: '#131b2b',
                    borderColor: '#1e2d47',
                    borderWidth: 1,
                    titleColor: '#e8f0ff',
                    bodyColor: '#7a90b5',
                    callbacks: {
                        label: ctx => ` ${formatLargeNumber(ctx.raw, '$')}`
                    }
                }
            }
        }
    });
}

// =============================================
//  GAINERS & LOSERS
// =============================================
async function loadGainers() {
    try {
        const gainers = await fetchJSON(`${API_BASE}/market/gainers`);
        renderGainerLoserList('gainersList', gainers, true);
    } catch (e) {
        document.getElementById('gainersList').innerHTML = '<li class="loading-item">Failed to load</li>';
    }
}

async function loadLosers() {
    try {
        const losers = await fetchJSON(`${API_BASE}/market/losers`);
        renderGainerLoserList('losersList', losers, false);
    } catch (e) {
        document.getElementById('losersList').innerHTML = '<li class="loading-item">Failed to load</li>';
    }
}

function renderGainerLoserList(elementId, coins, isGainer) {
    const list = document.getElementById(elementId);
    if (!coins || coins.length === 0) {
        list.innerHTML = '<li class="loading-item">No data yet</li>';
        return;
    }

    list.innerHTML = coins.slice(0, 5).map(coin => {
        const ch = coin.priceChangePercentage24h;
        const color = isGainer ? 'var(--green)' : 'var(--red)';
        const sign = ch > 0 ? '+' : '';
        return `
        <li class="gainer-loser-item" onclick="selectCoinForChart('${coin.id}')" title="View ${coin.name} chart">
            <div class="gl-coin">
                <img class="gl-icon" src="${coin.imageUrl || ''}" alt="${coin.name}" 
                     onerror="this.style.display='none'" loading="lazy"/>
                <div>
                    <div class="gl-name">${coin.name}</div>
                    <div class="gl-symbol">${coin.symbol}</div>
                </div>
            </div>
            <div style="color:${color};font-weight:700;font-family:var(--font-mono);font-size:12px;">
                ${sign}${ch ? ch.toFixed(2) : '0.00'}%
            </div>
        </li>`;
    }).join('');
}

// =============================================
//  MANUAL REFRESH
// =============================================
async function refreshData() {
    const icon = document.getElementById('refreshIcon');
    icon.classList.add('spinning');

    try {
        await fetchJSON(`${API_BASE}/refresh`, 'POST');
        await loadDashboard();
    } catch (e) {
        console.error('Refresh error:', e);
    } finally {
        setTimeout(() => icon.classList.remove('spinning'), 800);
    }
}

// =============================================
//  UTILITY FUNCTIONS
// =============================================

/**
 * Fetches JSON from a URL with error handling.
 */
async function fetchJSON(url, method = 'GET') {
    const response = await fetch(url, {
        method,
        headers: { 'Content-Type': 'application/json' }
    });
    if (response.status === 204) return []; // No content
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    return response.json();
}

/**
 * Formats a price with appropriate decimal places.
 */
function formatPrice(price) {
    if (price == null) return '—';
    if (price >= 1000) return '$' + price.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    if (price >= 1) return '$' + price.toFixed(4);
    return '$' + price.toFixed(8);
}

/**
 * Formats large numbers (market cap, volume) as readable strings.
 */
function formatLargeNumber(num, prefix = '') {
    if (num == null || num === 0) return '—';
    if (num >= 1e12) return prefix + (num / 1e12).toFixed(2) + 'T';
    if (num >= 1e9) return prefix + (num / 1e9).toFixed(2) + 'B';
    if (num >= 1e6) return prefix + (num / 1e6).toFixed(2) + 'M';
    if (num >= 1e3) return prefix + (num / 1e3).toFixed(2) + 'K';
    return prefix + num.toFixed(2);
}

/**
 * Short number format for chart Y axis.
 */
function formatShortNumber(num) {
    if (num >= 1e9) return (num / 1e9).toFixed(1) + 'B';
    if (num >= 1e6) return (num / 1e6).toFixed(1) + 'M';
    if (num >= 1e3) return (num / 1e3).toFixed(1) + 'K';
    if (num >= 1) return num.toFixed(2);
    return num.toFixed(4);
}

/**
 * Formats a percentage change with color coding.
 */
function formatChange(change) {
    if (change == null) return '<span class="change-neutral">—</span>';
    const sign = change > 0 ? '+' : '';
    const cls = change > 0 ? 'change-positive' : change < 0 ? 'change-negative' : 'change-neutral';
    return `<span class="${cls}">${sign}${change.toFixed(2)}%</span>`;
}

/**
 * Updates the last updated timestamp in the header.
 */
function updateTimestamp() {
    const el = document.getElementById('lastUpdate');
    const now = new Date();
    el.textContent = `Updated: ${now.toLocaleTimeString()}`;
}
