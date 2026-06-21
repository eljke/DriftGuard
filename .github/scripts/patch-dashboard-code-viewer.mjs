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

const appPath = findFile(checkoutRoot, "/packages/dashboard/src/App.tsx");
if (!appPath) {
  throw new Error("App.tsx was not found in the dashboard checkout");
}

let appSource = fs.readFileSync(appPath, "utf8").replaceAll("\r\n", "\n");

const appReplacements = [
  [
    `const ONBOARDING_DISMISSED_KEY = "ua-onboarding-dismissed-v1";
type SidebarTab = "info" | "files";
`,
    `const ONBOARDING_DISMISSED_KEY = "ua-onboarding-dismissed-v1";
const LANGUAGE_STORAGE_KEY = "ua-output-language";
type SidebarTab = "info" | "files";
`,
  ],
  [
    `      "knowledge-graph.json": import.meta.env.VITE_GRAPH_URL,
      "domain-graph.json": import.meta.env.VITE_DOMAIN_GRAPH_URL,
`,
    `      "knowledge-graph.json": import.meta.env.VITE_GRAPH_URL,
      "knowledge-graph.en.json": import.meta.env.VITE_GRAPH_EN_URL,
      "domain-graph.json": import.meta.env.VITE_DOMAIN_GRAPH_URL,
`,
  ],
  [
    `function Dashboard({ accessToken }: { accessToken: string }) {
`,
    `function LanguageToggle({
  language,
  onChange,
}: {
  language: string;
  onChange: (language: "ru" | "en") => void;
}) {
  return (
    <div className="fixed bottom-4 right-4 z-[70] flex rounded-md border border-border-medium bg-surface p-0.5 shadow-lg">
      {(["ru", "en"] as const).map((option) => (
        <button
          key={option}
          type="button"
          onClick={() => onChange(option)}
          className={\`min-w-10 rounded px-2.5 py-1.5 text-xs font-semibold uppercase transition-colors \${
            language === option
              ? "bg-accent text-root"
              : "text-text-muted hover:bg-elevated hover:text-text-primary"
          }\`}
          aria-pressed={language === option}
        >
          {option}
        </button>
      ))}
    </div>
  );
}

function VersionBadge({ accessToken }: { accessToken: string }) {
  const [version, setVersion] = useState<{
    projectVersion: string;
    hash: string;
    date: string;
  } | null>(null);
  const [open, setOpen] = useState(false);

  useEffect(() => {
    let cancelled = false;
    fetch(dataUrl("knowledge-graph.json", accessToken))
      .then((response) => (response.ok ? response.json() : null))
      .then((graph) => {
        if (cancelled || !graph?.project) return;
        const hash =
          graph.project.gitCommitShort ??
          String(graph.project.gitCommitHash ?? "").slice(0, 7);
        if (!hash) return;
        const projectVersion = String(graph.project.version ?? "1.0.0");
        const rawDate = graph.project.gitCommitDate ?? graph.project.analyzedAt;
        const date = rawDate
          ? new Date(rawDate).toLocaleDateString("ru-RU")
          : "";
        setVersion({ projectVersion, hash, date });
      })
      .catch(() => {});

    return () => {
      cancelled = true;
    };
  }, [accessToken]);

  if (!version) return null;

  return (
    <div className="fixed bottom-4 left-4 z-[70]">
      {open && (
        <div className="mb-2 w-52 rounded-md border border-border-medium bg-surface/95 p-3 text-xs text-text-muted shadow-lg">
          <div className="text-sm font-semibold text-text-primary">DriftGuard</div>
          <dl className="mt-2 space-y-1.5">
            <div className="flex items-center justify-between gap-3">
              <dt>Версия</dt>
              <dd className="font-mono text-text-primary">{version.projectVersion}</dd>
            </div>
            <div className="flex items-center justify-between gap-3">
              <dt>Коммит</dt>
              <dd className="font-mono text-text-primary">{version.hash}</dd>
            </div>
            {version.date && (
              <div className="flex items-center justify-between gap-3">
                <dt>Дата</dt>
                <dd className="text-text-primary">{version.date}</dd>
              </div>
            )}
          </dl>
        </div>
      )}
      <button
        type="button"
        onClick={() => setOpen((value) => !value)}
        className="rounded-md border border-border-medium bg-surface/95 px-3 py-1.5 text-xs font-semibold text-text-primary shadow-lg transition-colors hover:bg-elevated"
        aria-expanded={open}
      >
        DriftGuard <span className="ml-1 font-mono text-text-muted">{version.projectVersion}</span>
      </button>
    </div>
  );
}

function Dashboard({ accessToken }: { accessToken: string }) {
`,
  ],
  [
    `  const [outputLanguage, setOutputLanguage] = useState<string | undefined>();
`,
    `  const [outputLanguage, setOutputLanguage] = useState<string>(() => {
    return window.localStorage.getItem(LANGUAGE_STORAGE_KEY) ?? "ru";
  });
  const changeOutputLanguage = useCallback((language: "ru" | "en") => {
    window.localStorage.setItem(LANGUAGE_STORAGE_KEY, language);
    setOutputLanguage(language);
  }, []);
`,
  ],
  [
    `        if (config?.outputLanguage) setOutputLanguage(config.outputLanguage);
`,
    `        if (
          config?.outputLanguage &&
          !window.localStorage.getItem(LANGUAGE_STORAGE_KEY)
        ) {
          setOutputLanguage(config.outputLanguage);
        }
`,
  ],
  [
    `  useEffect(() => {
    fetch(dataUrl("knowledge-graph.json", accessToken))
`,
    `  useEffect(() => {
    const graphFile =
      outputLanguage === "en" ? "knowledge-graph.en.json" : "knowledge-graph.json";
    fetch(dataUrl(graphFile, accessToken))
`,
  ],
  [
    `  }, [setGraph]);
`,
    `  }, [outputLanguage, setGraph]);
`,
  ],
  [
    `    <I18nProvider language={outputLanguage ?? "en"}>
      <ThemeProvider metaTheme={metaTheme}>
        <DashboardContent
`,
    `    <I18nProvider language={outputLanguage}>
      <ThemeProvider metaTheme={metaTheme}>
        <LanguageToggle language={outputLanguage} onChange={changeOutputLanguage} />
        <VersionBadge accessToken={accessToken} />
        <DashboardContent
`,
  ],
];

for (const [current, replacement] of appReplacements) {
  if (!appSource.includes(current)) {
    throw new Error(`Expected App.tsx block was not found:\n${current}`);
  }
  appSource = appSource.replace(current, replacement);
}

fs.writeFileSync(appPath, appSource);
console.log(`Patched ${appPath}`);
