import crypto from "node:crypto";
import fs from "node:fs";
import path from "node:path";
import { execFileSync } from "node:child_process";

const root = path.resolve(process.argv[2] ?? ".");
const uaDir = path.join(root, ".understand-anything");
const graphPath = path.join(uaDir, "knowledge-graph.json");
const metaPath = path.join(uaDir, "meta.json");
const configPath = path.join(uaDir, "config.json");
const fingerprintsPath = path.join(uaDir, "fingerprints.json");
const git = process.platform === "win32"
  ? "C:\\Program Files\\Git\\cmd\\git.exe"
  : "git";

const graph = readJson(graphPath);
const meta = readJson(metaPath);
const commit = gitText(["rev-parse", "HEAD"]);
const baseCommit = meta.gitCommitHash;
const changed = gitText(["diff", `${baseCommit}..${commit}`, "--name-only"])
  .split(/\r?\n/)
  .filter(Boolean);
const javaFiles = changed.filter((file) => file.endsWith(".java"));

if (javaFiles.length > 0) {
  updateJavaNodes(graph, javaFiles);
  updateLayers(graph);
}

graph.project.gitCommitHash = commit;
graph.project.analyzedAt = new Date().toISOString();
writeJson(graphPath, graph);

const config = readJson(configPath);
writeJson(configPath, { ...config, autoUpdate: true, outputLanguage: config.outputLanguage ?? "ru" });
writeJson(metaPath, {
  ...meta,
  lastAnalyzedAt: new Date().toISOString(),
  gitCommitHash: commit,
  analyzedFiles: new Set(graph.nodes.map((node) => node.filePath).filter(Boolean)).size,
});
updateFingerprints(changed, commit);

execFileSync(process.execPath, [
  path.join(root, ".github", "scripts", "generate-english-knowledge-graph.mjs"),
  root,
], { cwd: root, stdio: "inherit" });

console.log(`Knowledge graph updated: ${baseCommit.slice(0, 7)} -> ${commit.slice(0, 7)}; Java files: ${javaFiles.length}`);

function updateJavaNodes(targetGraph, files) {
  const existingFiles = new Map(
    targetGraph.nodes
      .filter((node) => node.type === "file" && node.filePath)
      .map((node) => [path.basename(node.filePath, ".java"), node.id]),
  );
  for (const file of files) {
    const fileId = `file:${file}`;
    const oldIds = new Set(
      targetGraph.nodes.filter((node) => node.filePath === file).map((node) => node.id),
    );
    targetGraph.nodes = targetGraph.nodes.filter((node) => node.filePath !== file);
    targetGraph.edges = targetGraph.edges.filter((edge) =>
      !oldIds.has(edge.source)
      && (!oldIds.has(edge.target) || edge.target === fileId)
      && edge.source !== fileId,
    );

    const fullPath = path.join(root, file);
    if (!fs.existsSync(fullPath)) {
      continue;
    }

    const source = fs.readFileSync(fullPath, "utf8");
    const lines = source.split(/\r?\n/);
    const isTest = file.includes("/src/test/");
    const layer = layerFor(file);
    const typeMatch = source.match(/\b(?:class|interface|record|enum)\s+([A-Za-z_$][\w$]*)/);
    const typeName = typeMatch?.[1] ?? path.basename(file, ".java");
    const complexity = lines.length > 250 ? "complex" : lines.length > 80 ? "moderate" : "simple";

    targetGraph.nodes.push({
      id: fileId,
      type: "file",
      name: path.basename(file),
      filePath: file,
      summary: isTest
        ? `Проверяет поведение ${typeName} и его взаимодействие с компонентами DriftGuard.`
        : summaryFor(file, typeName),
      tags: ["java", isTest ? "тесты" : "drift-detection", layer.replace("layer:", ""), isTest ? "проверка-поведения" : "component"],
      complexity,
    });

    if (typeMatch) {
      const line = lineOf(lines, typeMatch.index);
      const classId = `class:${file}:${typeName}`;
      targetGraph.nodes.push({
        id: classId,
        type: "class",
        name: typeName,
        filePath: file,
        lineRange: [line, findBlockEnd(lines, line)],
        summary: `Компонент ${typeName} реализует ${roleFor(file)}.`,
        tags: ["java", typeMatch[0].split(/\s+/)[0], "domain-component"],
        complexity,
      });
      addEdge(targetGraph, fileId, classId, "contains", 1);
      addEdge(targetGraph, fileId, classId, "exports", 0.8);
    }

    for (const method of javaMethods(lines, typeName)) {
      const methodId = `function:${file}:${method.name}`;
      if (targetGraph.nodes.some((node) => node.id === methodId)) {
        continue;
      }
      targetGraph.nodes.push({
        id: methodId,
        type: "function",
        name: method.name,
        filePath: file,
        lineRange: [method.line, findBlockEnd(lines, method.line)],
        summary: isTest
          ? `Проверяет сценарий ${humanize(method.name)}.`
          : `Реализует операцию ${humanize(method.name)} в ${typeName}.`,
        tags: ["java", isTest ? "test" : "method", "behavior"],
        complexity: "simple",
      });
      addEdge(targetGraph, fileId, methodId, "contains", 1);
    }

    for (const imported of javaImports(source)) {
      const target = existingFiles.get(imported);
      if (target && target !== fileId) {
        addEdge(targetGraph, fileId, target, "imports", 0.7);
      }
    }

    existingFiles.set(typeName, fileId);
  }
}

function updateLayers(targetGraph) {
  const nodeIds = new Set(targetGraph.nodes.map((node) => node.id));
  for (const layer of targetGraph.layers) {
    layer.nodeIds = layer.nodeIds.filter((id) => nodeIds.has(id));
  }
  for (const node of targetGraph.nodes.filter((candidate) => candidate.type === "file")) {
    const layerId = layerFor(node.filePath);
    const layer = targetGraph.layers.find((candidate) => candidate.id === layerId);
    if (layer && !layer.nodeIds.includes(node.id)) {
      layer.nodeIds.push(node.id);
    }
  }
}

function updateFingerprints(files, commitHash) {
  const store = fs.existsSync(fingerprintsPath)
    ? readJson(fingerprintsPath)
    : { version: "1.0.0", files: {} };
  store.gitCommitHash = commitHash;
  store.generatedAt = new Date().toISOString();
  for (const file of files) {
    const fullPath = path.join(root, file);
    if (!fs.existsSync(fullPath)) {
      delete store.files[file];
      continue;
    }
    const content = fs.readFileSync(fullPath, "utf8");
    const lines = content.split(/\r?\n/);
    store.files[file] = {
      filePath: file,
      contentHash: crypto.createHash("sha256").update(content).digest("hex"),
      functions: file.endsWith(".java")
        ? javaMethods(lines, path.basename(file, ".java")).map((method) => ({
            name: method.name,
            params: [],
            exported: true,
            lineCount: findBlockEnd(lines, method.line) - method.line + 1,
          }))
        : [],
      classes: [...content.matchAll(/\b(?:class|interface|record|enum)\s+([A-Za-z_$][\w$]*)/g)]
        .map((match) => ({ name: match[1], exported: true })),
      imports: file.endsWith(".java") ? javaImports(content) : [],
      exports: [],
      totalLines: lines.length,
      hasStructuralAnalysis: file.endsWith(".java"),
    };
  }
  writeJson(fingerprintsPath, store);
}

function javaMethods(lines, typeName) {
  const result = [];
  const pattern = /^\s*(?:public|protected|private|static|final|synchronized|abstract|default|\s)+\s*(?:<[^>]+>\s*)?(?:[\w$<>\[\],.?]+\s+)?([A-Za-z_$][\w$]*)\s*\([^;]*\)\s*(?:throws [^{]+)?\{/;
  lines.forEach((line, index) => {
    const match = line.match(pattern);
    if (match && !["if", "for", "while", "switch", "catch", "return", "new"].includes(match[1])) {
      result.push({ name: match[1] === typeName ? typeName : match[1], line: index + 1 });
    }
  });
  return result;
}

function javaImports(source) {
  return [...source.matchAll(/^import\s+(?:static\s+)?[\w.]+\.([A-Za-z_$][\w$]*);/gm)]
    .map((match) => match[1]);
}

function addEdge(targetGraph, source, target, type, weight) {
  const key = `${source}|${target}|${type}`;
  if (!targetGraph.edges.some((edge) => `${edge.source}|${edge.target}|${edge.type}` === key)) {
    targetGraph.edges.push({ source, target, type, direction: "forward", weight });
  }
}

function layerFor(file) {
  if (file.includes("/src/test/")) return "layer:tests";
  if (file.startsWith("driftguard-core/")) return "layer:core";
  if (file.startsWith("driftguard-algorithms/")) return "layer:algorithms";
  if (file.startsWith("driftguard-kafka/")) return "layer:kafka";
  if (file.startsWith("driftguard-spring-boot-starter/")) return "layer:spring-boot";
  if (file.startsWith("driftguard-testkit/")) return "layer:testkit";
  return "layer:project-support";
}

function summaryFor(file, typeName) {
  if (file.includes("/adaptive/")) {
    return `Реализует адаптивный выбор профиля чувствительности Page-Hinkley в компоненте ${typeName}.`;
  }
  if (typeName === "StateAwareEmissionPolicy") {
    return "Определяет контракт выбора emission policy на основе сохранённого состояния детектора.";
  }
  if (typeName === "DriftDetectorEngine") {
    return "Координирует запуск алгоритмов, обновление состояния и применение state-aware emission policy.";
  }
  return `Реализует компонент ${typeName} модуля DriftGuard.`;
}

function roleFor(file) {
  if (file.includes("/adaptive/")) return "адаптивную калибровку и выбор профиля Page-Hinkley";
  if (file.includes("/state/")) return "сериализацию и восстановление состояния детектора";
  if (file.includes("/autoconfigure/")) return "Spring Boot auto-configuration";
  if (file.includes("/detector/")) return "runtime pipeline обнаружения drift";
  return "контракт или поведение DriftGuard";
}

function lineOf(lines, offset) {
  return lines.join("\n").slice(0, offset).split("\n").length;
}

function findBlockEnd(lines, startLine) {
  let depth = 0;
  let opened = false;
  for (let index = startLine - 1; index < lines.length; index++) {
    for (const char of lines[index]) {
      if (char === "{") {
        depth++;
        opened = true;
      } else if (char === "}") {
        depth--;
        if (opened && depth === 0) return index + 1;
      }
    }
  }
  return lines.length;
}

function humanize(value) {
  return value.replace(/([a-z0-9])([A-Z])/g, "$1 $2").toLowerCase();
}

function gitText(args) {
  return execFileSync(git, args, { cwd: root, encoding: "utf8" }).trim();
}

function readJson(file) {
  return JSON.parse(fs.readFileSync(file, "utf8"));
}

function writeJson(file, value) {
  fs.writeFileSync(file, `${JSON.stringify(value, null, 2)}\n`);
}
