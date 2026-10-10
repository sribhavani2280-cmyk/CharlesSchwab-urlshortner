import { useState } from "react";
import { ConnectionBar } from "./components/ConnectionBar";
import { LinksPage } from "./components/links/LinksPage";
import { OrchestrationPage } from "./components/orchestration/OrchestrationPage";

type Tab = "links" | "orchestration";

export function App() {
  const [tab, setTab] = useState<Tab>("orchestration");

  return (
    <>
      <header className="app-header">
        <h1>Agentic URL Shortener</h1>
        <nav className="tabs">
          <button
            className={tab === "links" ? "active" : ""}
            onClick={() => setTab("links")}
          >
            URL Shortener
          </button>
          <button
            className={tab === "orchestration" ? "active" : ""}
            onClick={() => setTab("orchestration")}
          >
            Orchestration Console
          </button>
        </nav>
      </header>
      <div className="container">
        <ConnectionBar />
        {tab === "links" ? <LinksPage /> : <OrchestrationPage />}
      </div>
    </>
  );
}
