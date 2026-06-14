import fs from "node:fs";
import path from "node:path";

const projectRoot = path.resolve(process.argv[2] ?? ".");
const sourcePath = path.join(projectRoot, ".understand-anything", "knowledge-graph.json");
const outputPath = path.join(projectRoot, ".understand-anything", "knowledge-graph.en.json");
const graph = JSON.parse(fs.readFileSync(sourcePath, "utf8"));

const layerText = {
  "layer:core": [
    "Core",
    "Domain contracts, the DriftGuard facade, detector engine, state stores, and base event sinks.",
  ],
  "layer:algorithms": [
    "Algorithms",
    "Built-in Page-Hinkley, ADWIN, PSI, Kolmogorov-Smirnov, and chi-square detectors with supporting structures.",
  ],
  "layer:kafka": [
    "Kafka Streams",
    "Kafka SerDes, topology builders, state integration, telemetry, and stream-processing error handling.",
  ],
  "layer:spring-boot": [
    "Spring Boot Integration",
    "Auto-configuration, property binding, Micrometer input, and observability adapters for integrating DriftGuard.",
  ],
  "layer:testkit": [
    "Testkit and Quality Gates",
    "Synthetic metric scenarios, benchmark runners, quality gates, and reports for evaluating detectors.",
  ],
  "layer:tests": [
    "Tests",
    "JUnit behavioral and integration tests covering real interactions across the core, algorithms, Kafka, and Spring Boot modules.",
  ],
  "layer:project-support": [
    "Configuration and Documentation",
    "Root Maven configuration, module manifests, scripts, fixtures, and README files supporting project development.",
  ],
};

const tourText = {
  1: [
    "Project Overview",
    "Start with the README to understand DriftGuard's purpose, repository boundaries, and Maven modules. Its runtime flow provides a map for reading the implementation.",
  ],
  2: [
    "Metric Domain",
    "MetricKey and MetricPoint define the stable input contract. They establish metric-stream identity and the observations processed by every detector.",
    "Java records and immutable value objects make domain contracts explicit and provide reliable value equality.",
  ],
  3: [
    "Detector Contracts",
    "DetectorAlgorithm, DetectorConfig, and DetectorDefinition form the extension API. These contracts separate core orchestration from specific statistical implementations.",
  ],
  4: [
    "Runtime Engine",
    "The DriftGuard facade forwards MetricPoint values to DriftDetectorEngine, which selects detector instances, updates runtime state, and publishes results.",
  ],
  5: [
    "Drift Events",
    "DriftEvent, DriftSeverity, and DriftDirection describe detection results for consumers. Event sinks separate drift computation from alert and telemetry delivery.",
  ],
  6: [
    "Built-in Algorithms",
    "Compare Page-Hinkley and ADWIN, then follow their shared support utilities. They show how one DetectorAlgorithm contract supports different statistical models.",
  ],
  7: [
    "Kafka Topology",
    "KafkaDriftGuardTopologyBuilder connects input topics, SerDes, detector execution, and output events in a Kafka Streams topology, including persisted detector state.",
  ],
  8: [
    "Spring Boot Starter",
    "Auto-configuration assembles core, algorithms, Kafka, and Micrometer adapters from application properties, exposing the main integration surface for applications.",
  ],
  9: [
    "Scenarios and Quality Gates",
    "The testkit generates reproducible metric streams with known drift intervals, runs benchmarks, and applies quality gates to evaluate detector behavior.",
  ],
  10: [
    "Integration Tests",
    "Finish with engine, Kafka topology, and Spring auto-configuration tests. They document real module interactions as executable usage examples.",
  ],
};

const tagTranslations = new Map([
  ["проверка-поведения", "behavior-test"],
  ["точка-входа", "entry-point"],
  ["утилиты", "utilities"],
  ["конфигурация", "configuration"],
  ["документация", "documentation"],
  ["инфраструктура", "infrastructure"],
  ["тесты", "tests"],
  ["архитектура", "architecture"],
  ["обзор", "overview"],
]);

function moduleName(filePath = "") {
  return filePath.split("/")[0] || "DriftGuard";
}

function ownerName(node) {
  const fileName = path.basename(node.filePath ?? "", path.extname(node.filePath ?? ""));
  return fileName || node.name;
}

function humanize(name) {
  return String(name)
    .replace(/([a-z0-9])([A-Z])/g, "$1 $2")
    .replaceAll("_", " ")
    .replace(/\s+/g, " ")
    .trim()
    .toLowerCase();
}

function fileRole(filePath = "") {
  if (filePath.includes("/src/test/")) return "behavior and integration tests";
  if (filePath.includes("/domain/")) return "domain modeling";
  if (filePath.includes("/detector/")) return "detector execution";
  if (filePath.includes("/state/")) return "detector state management";
  if (filePath.includes("/config/")) return "detector configuration";
  if (filePath.includes("/alert/")) return "drift alert delivery";
  if (filePath.includes("/serde/")) return "Kafka serialization";
  if (filePath.includes("/topology/")) return "Kafka Streams topology construction";
  if (filePath.includes("/autoconfigure/")) return "Spring Boot auto-configuration";
  if (filePath.includes("/benchmark/")) return "detector benchmarking";
  if (filePath.includes("/scenario/")) return "synthetic drift scenarios";
  if (filePath.includes("/quality/")) return "detector quality gates";
  return "data-drift detection";
}

function summaryFor(node) {
  const module = moduleName(node.filePath);
  const owner = ownerName(node);
  const role = fileRole(node.filePath);

  if (node.type === "document") {
    return "Documents DriftGuard's architecture, modules, runtime flow, integrations, algorithms, and quality practices.";
  }
  if (node.type === "config") {
    return `Defines Maven build configuration and dependencies for ${module}.`;
  }
  if (node.type === "file") {
    if (node.filePath?.includes("/src/test/")) {
      return `Tests ${owner} behavior and its integration with the ${module} module.`;
    }
    return `Provides ${owner} for ${role} in the ${module} module.`;
  }
  if (node.type === "class") {
    return `Encapsulates ${humanize(node.name)} behavior for ${role} in ${module}.`;
  }
  if (node.type === "function") {
    if (node.filePath?.includes("/src/test/")) {
      return `Verifies ${humanize(node.name)} behavior in ${owner}.`;
    }
    if (node.name === owner) {
      return `Constructs ${owner} instances used for ${role}.`;
    }
    return `Implements ${humanize(node.name)} behavior in ${owner}.`;
  }
  return `Represents ${humanize(node.name)} in the ${module} module.`;
}

graph.project.description =
  "A modular Java library for detecting data drift in streaming technical metrics and publishing structured drift alerts.";

graph.nodes = graph.nodes.map((node) => ({
  ...node,
  summary: summaryFor(node),
  tags: node.tags?.map((tag) => tagTranslations.get(tag) ?? tag),
}));

graph.layers = graph.layers.map((layer) => {
  const translated = layerText[layer.id];
  return translated
    ? { ...layer, name: translated[0], description: translated[1] }
    : layer;
});

graph.tour = graph.tour.map((step) => {
  const translated = tourText[step.order];
  if (!translated) return step;
  const result = {
    ...step,
    title: translated[0],
    description: translated[1],
  };
  if (translated[2]) result.languageLesson = translated[2];
  else delete result.languageLesson;
  return result;
});

fs.writeFileSync(outputPath, `${JSON.stringify(graph, null, 2)}\n`);
console.log(`Generated ${outputPath}`);
