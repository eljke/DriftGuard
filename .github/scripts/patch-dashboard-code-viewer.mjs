import fs from "node:fs";
import path from "node:path";

const checkoutRoot = path.resolve(process.argv[2] ?? ".");

function findFile(directory, suffix) {
  for (const entry of fs.readdirSync(directory, { withFileTypes: true })) {
    if (entry.name === "node_modules" || entry.name === ".git") continue;

    const entryPath = path.join(directory, entry.name);
    if (entry.isDirectory()) {
      const match = findFile(entryPath, suffix);
      if (match) return match;
    } else if (entryPath.replaceAll("\\", "/").endsWith(suffix)) {
      return entryPath;
    }
  }
  return null;
}

const codeViewerPath = findFile(
  checkoutRoot,
  "/packages/dashboard/src/components/CodeViewer.tsx",
);

if (!codeViewerPath) {
  throw new Error("CodeViewer.tsx was not found in the dashboard checkout");
}

let source = fs.readFileSync(codeViewerPath, "utf8").replaceAll("\r\n", "\n");

const fileContentUrlBlock = `function fileContentUrl(filePath: string, token: string): string {
  const params = new URLSearchParams({ token, path: filePath });
  return \`/file-content.json?\${params.toString()}\`;
}
`;

const sourceUrlBlock = `${fileContentUrlBlock}
const DEMO_SOURCE_BASE_URL = import.meta.env.VITE_SOURCE_BASE_URL as string | undefined;

function sourceRepositoryUrl(
  filePath: string,
  lineRange: readonly [number, number] | undefined,
): string | null {
  if (!DEMO_SOURCE_BASE_URL) return null;

  const encodedPath = filePath
    .split("/")
    .map((segment) => encodeURIComponent(segment))
    .join("/");
  const lineFragment = lineRange
    ? \`#L\${lineRange[0]}-L\${lineRange[1]}\`
    : "";
  return \`\${DEMO_SOURCE_BASE_URL.replace(/\\/$/, "")}/\${encodedPath}\${lineFragment}\`;
}
`;

const demoErrorBlock = `    if (accessToken === "__demo__") {
      setState({
        status: "error",
        source: null,
        error: "Source preview is available only when the local dashboard server is running.",
      });
      return;
    }
`;

const demoIdleBlock = `    if (accessToken === "__demo__") {
      setState({ status: "idle", source: null, error: null });
      return;
    }
`;

const highlightedRangeBlock = `  const highlightedRange = useMemo(() => {
    if (!node?.lineRange) return null;
    return { start: node.lineRange[0], end: node.lineRange[1] };
  }, [node?.lineRange]);
`;

const sourceLinkBlock = `${highlightedRangeBlock}
  const sourceRepositoryLink = useMemo(() => {
    if (accessToken !== "__demo__" || !node?.filePath) return null;
    return sourceRepositoryUrl(node.filePath, node.lineRange);
  }, [accessToken, node?.filePath, node?.lineRange]);
`;

const loadingBlock = `        {state.status === "loading" && (
          <div className="p-5 text-sm text-text-muted">{t.codeViewer.loading}</div>
        )}
`;

const githubLinkBlock = `${loadingBlock}
        {sourceRepositoryLink && (
          <div className="p-5">
            <a
              href={sourceRepositoryLink}
              target="_blank"
              rel="noreferrer"
              className="inline-flex items-center gap-2 rounded-md bg-accent px-4 py-2 text-sm font-medium text-root transition-opacity hover:opacity-90"
            >
              Открыть исходный код на GitHub
              <svg className="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M14 5h5v5M10 14L19 5M19 14v5H5V5h5" />
              </svg>
            </a>
          </div>
        )}
`;

const replacements = [
  [fileContentUrlBlock, sourceUrlBlock],
  [demoErrorBlock, demoIdleBlock],
  [highlightedRangeBlock, sourceLinkBlock],
  [loadingBlock, githubLinkBlock],
];

for (const [current, replacement] of replacements) {
  if (!source.includes(current)) {
    throw new Error(`Expected CodeViewer.tsx block was not found:\n${current}`);
  }
  source = source.replace(current, replacement);
}

fs.writeFileSync(codeViewerPath, source);
console.log(`Patched ${codeViewerPath}`);
