// Shared front-end helpers for the Smart Inventory System.

const SI = {
    palette: ['#4f46e5', '#16a34a', '#f59e0b', '#dc2626', '#0ea5e9', '#8b5cf6',
              '#14b8a6', '#ec4899', '#f97316', '#64748b'],

    async fetchJson(url) {
        const res = await fetch(url);
        if (!res.ok) throw new Error('Request failed: ' + url);
        return res.json();
    },

    money(v) {
        const sym = window.SIMS_CURRENCY || '';
        const num = Number(v || 0).toLocaleString(undefined, { minimumFractionDigits: 0, maximumFractionDigits: 2 });
        return sym ? sym + ' ' + num : num;
    },

    // number only (no currency) — for chart axes
    num(v) {
        return Number(v || 0).toLocaleString(undefined, { minimumFractionDigits: 0, maximumFractionDigits: 2 });
    },

    // adds the currency symbol to a chart tooltip value
    tooltipMoney(ctx) {
        return (window.SIMS_CURRENCY ? window.SIMS_CURRENCY + ' ' : '') + SI.num(ctx.parsed.y);
    },

    // Line chart with an optional dashed forecast tail.
    salesTrendChart(canvasId, data) {
        const labels = [...data.labels, ...(data.forecastLabels || [])];
        const history = data.values.concat((data.forecastLabels || []).map(() => null));
        const lastIdx = data.values.length - 1;
        const forecast = data.labels.map(() => null);
        if (lastIdx >= 0) forecast[lastIdx] = data.values[lastIdx]; // connect the lines
        const forecastSeries = forecast.concat(data.forecastValues || []);

        return new Chart(document.getElementById(canvasId), {
            type: 'line',
            data: {
                labels,
                datasets: [
                    {
                        label: 'Actual Revenue',
                        data: history,
                        borderColor: SI.palette[0],
                        backgroundColor: 'rgba(79,70,229,0.12)',
                        fill: true,
                        tension: 0.3,
                        pointRadius: 2
                    },
                    {
                        label: 'AI Forecast',
                        data: forecastSeries,
                        borderColor: SI.palette[3],
                        borderDash: [6, 4],
                        fill: false,
                        tension: 0.3,
                        pointRadius: 2
                    }
                ]
            },
            options: { responsive: true, maintainAspectRatio: false,
                plugins: {
                    legend: { position: 'bottom' },
                    tooltip: { callbacks: { label: (c) => c.dataset.label + ': ' + SI.money(c.parsed.y) } }
                } }
        });
    },

    barChart(canvasId, data, label, color, moneyTooltip) {
        return new Chart(document.getElementById(canvasId), {
            type: 'bar',
            data: {
                labels: data.labels,
                datasets: [{
                    label: label,
                    data: data.values,
                    backgroundColor: color || SI.palette[0],
                    borderRadius: 6
                }]
            },
            options: { responsive: true, maintainAspectRatio: false,
                plugins: {
                    legend: { display: false },
                    tooltip: { callbacks: { label: (c) => moneyTooltip ? SI.money(c.parsed.y) : SI.num(c.parsed.y) } }
                },
                scales: { y: { beginAtZero: true } } }
        });
    },

    doughnut(canvasId, data, colors) {
        return new Chart(document.getElementById(canvasId), {
            type: 'doughnut',
            data: {
                labels: data.labels,
                datasets: [{ data: data.values, backgroundColor: colors || SI.palette }]
            },
            options: { responsive: true, maintainAspectRatio: false,
                plugins: { legend: { position: 'bottom' } } }
        });
    },

    // Export an HTML table to a downloadable CSV file.
    exportTableCsv(tableId, filename) {
        const table = document.getElementById(tableId);
        if (!table) return;
        const rows = [...table.querySelectorAll('tr')];
        const csv = rows.map(r => [...r.querySelectorAll('th,td')]
            .map(c => '"' + c.innerText.replace(/"/g, '""').trim() + '"').join(',')).join('\n');
        const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
        const a = document.createElement('a');
        a.href = URL.createObjectURL(blob);
        a.download = filename || 'report.csv';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(a.href);
    }
};

// Dark-theme friendly chart text/grid colours
if (window.Chart) {
    Chart.defaults.color = '#cbd5e1';
    Chart.defaults.borderColor = 'rgba(148,163,184,0.18)';
}
