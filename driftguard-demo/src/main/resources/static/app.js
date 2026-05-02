const elements = {
    scenarioSelect: document.querySelector("#scenarioSelect"),
    runButton: document.querySelector("#runButton"),
    liveButton: document.querySelector("#liveButton"),
    stopButton: document.querySelector("#stopButton"),
    scenarioDescription: document.querySelector("#scenarioDescription"),
    scenarioTitle: document.querySelector("#scenarioTitle"),
    metricName: document.querySelector("#metricName"),
    pointCount: document.querySelector("#pointCount"),
    runMode: document.querySelector("#runMode"),
    eventCount: document.querySelector("#eventCount"),
    precision: document.querySelector("#precision"),
    recall: document.querySelector("#recall"),
    delay: document.querySelector("#delay"),
    chart: document.querySelector("#metricChart"),
    eventsTable: document.querySelector("#eventsTable"),
    eventStatus: document.querySelector("#eventStatus"),
    qualityStatus: document.querySelector("#qualityStatus"),
    tp: document.querySelector("#tp"),
    fp: document.querySelector("#fp"),
    detectedIntervals: document.querySelector("#detectedIntervals"),
    missedIntervals: document.querySelector("#missedIntervals"),
    kafkaScenarioSelect: document.querySelector("#kafkaScenarioSelect"),
    kafkaStartButton: document.querySelector("#kafkaStartButton"),
    kafkaStopButton: document.querySelector("#kafkaStopButton"),
    kafkaStatus: document.querySelector("#kafkaStatus"),
    kafkaPointCount: document.querySelector("#kafkaPointCount"),
    kafkaEventCount: document.querySelector("#kafkaEventCount"),
    kafkaInputTopic: document.querySelector("#kafkaInputTopic"),
    kafkaOutputTopic: document.querySelector("#kafkaOutputTopic"),
    producerGrid: document.querySelector("#producerGrid"),
    kafkaCharts: document.querySelector("#kafkaCharts"),
    kafkaEventsTable: document.querySelector("#kafkaEventsTable"),
    kafkaError: document.querySelector("#kafkaError"),
    toolsGrid: document.querySelector("#toolsGrid")
};

let scenarios = [];
let polling = null;
let kafkaPolling = null;

async function init() {
    scenarios = await fetchJson("/api/demo/scenarios");
    const options = scenarios
        .map(scenario => `<option value="${scenario.id}">${scenario.title}</option>`)
        .join("");
    elements.scenarioSelect.innerHTML = options;
    elements.kafkaScenarioSelect.innerHTML = options;
    elements.scenarioSelect.addEventListener("change", updateScenarioDescription);
    elements.runButton.addEventListener("click", runSelectedScenario);
    elements.liveButton.addEventListener("click", startLiveScenario);
    elements.stopButton.addEventListener("click", stopLiveScenario);
    elements.kafkaStartButton.addEventListener("click", startKafkaScenario);
    elements.kafkaStopButton.addEventListener("click", stopKafkaScenario);
    updateScenarioDescription();
    render(await fetchJson("/api/demo"));
    renderKafka(await fetchJson("/api/demo/kafka"));
    renderTools(await fetchJson("/api/demo/tools"));
}

async function runSelectedScenario() {
    stopPolling();
    elements.runButton.disabled = true;
    try {
        const scenario = elements.scenarioSelect.value;
        render(await fetchJson(`/api/demo/run/${scenario}`, {method: "POST"}));
    } finally {
        elements.runButton.disabled = false;
    }
}

async function startLiveScenario() {
    stopPolling();
    elements.liveButton.disabled = true;
    try {
        const scenario = elements.scenarioSelect.value;
        render(await fetchJson(`/api/demo/live/${scenario}`, {method: "POST"}));
        polling = window.setInterval(refreshLiveResult, 500);
    } finally {
        elements.liveButton.disabled = false;
    }
}

async function stopLiveScenario() {
    stopPolling();
    render(await fetchJson("/api/demo/live/stop", {method: "POST"}));
}

async function refreshLiveResult() {
    const result = await fetchJson("/api/demo");
    render(result);
    if (!result.running) {
        stopPolling();
    }
}

async function startKafkaScenario() {
    stopKafkaPolling();
    elements.kafkaStartButton.disabled = true;
    elements.kafkaError.textContent = "";
    try {
        const scenario = elements.kafkaScenarioSelect.value;
        renderKafka(await fetchJson(`/api/demo/kafka/start/${scenario}`, {method: "POST"}));
        kafkaPolling = window.setInterval(refreshKafkaResult, 700);
    } catch (error) {
        elements.kafkaError.textContent = `${error.message}. Проверь, что docker compose с Kafka запущен.`;
    } finally {
        elements.kafkaStartButton.disabled = false;
    }
}

async function stopKafkaScenario() {
    stopKafkaPolling();
    renderKafka(await fetchJson("/api/demo/kafka/stop", {method: "POST"}));
}

async function refreshKafkaResult() {
    const status = await fetchJson("/api/demo/kafka");
    renderKafka(status);
    if (!status.running) {
        stopKafkaPolling();
    }
}

function stopKafkaPolling() {
    if (kafkaPolling) {
        window.clearInterval(kafkaPolling);
        kafkaPolling = null;
    }
}

function stopPolling() {
    if (polling) {
        window.clearInterval(polling);
        polling = null;
    }
}

function updateScenarioDescription() {
    const selected = scenarios.find(scenario => scenario.id === elements.scenarioSelect.value);
    elements.scenarioDescription.textContent = selected ? selected.description : "";
}

async function fetchJson(url, options = {}) {
    const response = await fetch(url, options);
    if (!response.ok) {
        throw new Error(`Request failed: ${response.status}`);
    }
    return response.json();
}

function render(result) {
    elements.scenarioTitle.textContent = result.title;
    elements.metricName.textContent = result.samplePoints[0]?.key?.metric ?? "metric";
    elements.pointCount.textContent = `${result.processedPoints}/${result.metricPoints}`;
    elements.runMode.textContent = result.running ? "LIVE" : String(result.mode).toUpperCase();
    elements.eventCount.textContent = result.events.length;
    elements.precision.textContent = formatRatio(result.quality.precision);
    elements.recall.textContent = formatRatio(result.quality.recall);
    elements.delay.textContent = formatDelay(result.quality.firstDetectionDelay);
    elements.tp.textContent = result.quality.truePositiveEvents;
    elements.fp.textContent = result.quality.falsePositiveEvents;
    elements.detectedIntervals.textContent = result.quality.detectedDriftIntervals;
    elements.missedIntervals.textContent = result.quality.missedDriftIntervals;
    elements.eventStatus.textContent = result.events.length === 0 ? "none" : `${result.events.length} event`;
    elements.qualityStatus.textContent = result.quality.detected ? "drift detected" : "no expected drift";
    elements.qualityStatus.className = result.quality.detected ? "badge status-ok" : "badge";
    renderEvents(result.events);
    drawChart(result.samplePoints, result.events, result.expectedDrifts);
}

function renderKafka(status) {
    elements.kafkaStatus.textContent = status.running ? "running" : "stopped";
    elements.kafkaStatus.className = status.running ? "badge status-ok" : "badge";
    elements.kafkaPointCount.textContent = `${status.producedPoints}/${status.totalPoints}`;
    elements.kafkaEventCount.textContent = status.consumedEvents.length;
    elements.kafkaInputTopic.textContent = status.inputTopic;
    elements.kafkaOutputTopic.textContent = status.outputTopic;
    elements.kafkaError.textContent = status.error ? `Ошибка Kafka demo: ${status.error}` : "";
    renderProducers(status.producers);
    renderKafkaEvents(status.consumedEvents);
    renderKafkaCharts(status.samplePoints, status.consumedEvents);
}

function renderKafkaCharts(points = [], events = []) {
    if (!points.length) {
        elements.kafkaCharts.innerHTML = `<div class="empty-chart">Точки из Kafka пока не опубликованы</div>`;
        return;
    }
    const groups = groupPointsByStream(points);
    elements.kafkaCharts.innerHTML = groups.map((group, index) => `
        <div class="stream-chart">
            <div class="stream-chart-head">
                <div>
                    <strong>${group.service}</strong>
                    <span>${group.metric} · ${group.operation ?? "-"}</span>
                </div>
                <span class="badge">${group.points.length} points · ${eventsForStream(events, group).length} events</span>
            </div>
            <canvas data-stream-index="${index}" width="1100" height="220"></canvas>
        </div>
    `).join("");
    groups.forEach((group, index) => {
        const canvas = elements.kafkaCharts.querySelector(`canvas[data-stream-index="${index}"]`);
        drawChartOn(canvas, group.points, eventsForStream(events, group), []);
    });
}

function groupPointsByStream(points) {
    const byStream = new Map();
    points.forEach(point => {
        const id = streamId(point.key);
        if (!byStream.has(id)) {
            byStream.set(id, {
                id,
                service: point.key.service,
                metric: point.key.metric,
                operation: point.key.operation,
                points: []
            });
        }
        byStream.get(id).points.push(point);
    });
    return Array.from(byStream.values())
        .map(group => ({...group, points: [...group.points].sort((left, right) => Date.parse(left.timestamp) - Date.parse(right.timestamp))}))
        .sort((left, right) => left.id.localeCompare(right.id));
}

function eventsForStream(events, group) {
    return events.filter(event => streamId(event.key) === group.id);
}

function streamId(key) {
    return `${key.service}|${key.metric}|${key.operation ?? ""}`;
}

function renderProducers(producers = []) {
    if (producers.length === 0) {
        elements.producerGrid.innerHTML = "";
        return;
    }
    elements.producerGrid.innerHTML = producers.map(producer => `
        <div class="producer-card">
            <strong>${producer.service}</strong>
            <span>${producer.metric} · ${producer.operation ?? "-"}</span>
            <span>${producer.id}</span>
            <span>${producer.producedPoints}/${producer.totalPoints} points · ${producer.running ? "running" : "done"}</span>
        </div>
    `).join("");
}

function renderKafkaEvents(events) {
    if (events.length === 0) {
        elements.kafkaEventsTable.innerHTML = `<tr><td colspan="7">Событий из Kafka пока нет</td></tr>`;
        return;
    }
    elements.kafkaEventsTable.innerHTML = events.map(event => `
        <tr>
            <td>${formatTime(event.detectedAt)}</td>
            <td>${event.key.service}<br><span class="muted">${event.key.metric}</span></td>
            <td>${event.detector}</td>
            <td class="severity-${String(event.severity).toLowerCase()}">${event.severity}</td>
            <td>${formatNumber(event.score)}</td>
            <td>${formatNumber(event.currentValue)}</td>
            <td>${formatNumber(event.baselineValue)}</td>
        </tr>
    `).join("");
}

function renderTools(tools) {
    elements.toolsGrid.innerHTML = tools.map(tool => `
        <a class="tool-link" href="${tool.url}" target="_blank" rel="noreferrer">
            <strong>${tool.title}</strong>
            <span>${tool.description}</span>
            <span>${tool.url}</span>
        </a>
    `).join("");
}

function renderEvents(events) {
    if (events.length === 0) {
        elements.eventsTable.innerHTML = `<tr><td colspan="6">Событий нет</td></tr>`;
        return;
    }
    elements.eventsTable.innerHTML = events.map(event => `
        <tr>
            <td>${formatTime(event.detectedAt)}</td>
            <td>${event.detector}</td>
            <td class="severity-${String(event.severity).toLowerCase()}">${event.severity}</td>
            <td>${formatNumber(event.score)}</td>
            <td>${formatNumber(event.currentValue)}</td>
            <td>${formatNumber(event.baselineValue)}</td>
        </tr>
    `).join("");
}

function drawChart(points, events, intervals) {
    drawChartOn(elements.chart, points, events, intervals);
}

function drawChartOn(canvas, points, events, intervals) {
    const ctx = canvas.getContext("2d");
    const width = canvas.width;
    const height = canvas.height;
    ctx.clearRect(0, 0, width, height);
    if (!points.length) {
        return;
    }
    const padding = {left: 54, right: 22, top: 24, bottom: 34};
    const values = points.map(point => point.value);
    const min = Math.min(...values);
    const max = Math.max(...values);
    const range = Math.max(1, max - min);
    const startTime = Date.parse(points[0].timestamp);
    const endTime = Date.parse(points[points.length - 1].timestamp);
    const timeRange = Math.max(1, endTime - startTime);

    const x = timestamp => padding.left + ((Date.parse(timestamp) - startTime) / timeRange) * (width - padding.left - padding.right);
    const y = value => height - padding.bottom - ((value - min) / range) * (height - padding.top - padding.bottom);

    ctx.strokeStyle = "#d9e1ea";
    ctx.lineWidth = 1;
    for (let i = 0; i <= 4; i++) {
        const yy = padding.top + i * (height - padding.top - padding.bottom) / 4;
        ctx.beginPath();
        ctx.moveTo(padding.left, yy);
        ctx.lineTo(width - padding.right, yy);
        ctx.stroke();
    }

    ctx.fillStyle = "rgba(194, 65, 12, 0.10)";
    intervals.forEach(interval => {
        const left = x(interval.start);
        const right = x(interval.end);
        ctx.fillRect(left, padding.top, Math.max(2, right - left), height - padding.top - padding.bottom);
    });

    ctx.strokeStyle = "#007c89";
    ctx.lineWidth = 2;
    ctx.beginPath();
    points.forEach((point, index) => {
        const px = x(point.timestamp);
        const py = y(point.value);
        if (index === 0) {
            ctx.moveTo(px, py);
        } else {
            ctx.lineTo(px, py);
        }
    });
    ctx.stroke();

    ctx.fillStyle = "#c2410c";
    events.forEach(event => {
        const px = x(event.detectedAt);
        const py = y(event.currentValue);
        ctx.beginPath();
        ctx.arc(px, py, 5, 0, Math.PI * 2);
        ctx.fill();
    });

    ctx.fillStyle = "#637083";
    ctx.font = "13px Segoe UI, Arial";
    ctx.fillText(formatNumber(max), 8, padding.top + 4);
    ctx.fillText(formatNumber(min), 8, height - padding.bottom);
    ctx.fillText(formatTime(points[0].timestamp), padding.left, height - 10);
    ctx.textAlign = "right";
    ctx.fillText(formatTime(points[points.length - 1].timestamp), width - padding.right, height - 10);
    ctx.textAlign = "left";
}

function formatRatio(value) {
    return `${Math.round((value ?? 0) * 100)}%`;
}

function formatDelay(value) {
    if (!value) {
        return "-";
    }
    const match = String(value).match(/PT(\d+)S/);
    return match ? `${match[1]}s` : value;
}

function formatNumber(value) {
    return Number(value).toLocaleString("ru-RU", {maximumFractionDigits: 3});
}

function formatTime(value) {
    return new Date(value).toLocaleTimeString("ru-RU", {
        timeZone: "Europe/Moscow",
        hour: "2-digit",
        minute: "2-digit",
        second: "2-digit"
    });
}

init().catch(error => {
    elements.scenarioDescription.textContent = error.message;
});
