import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import type { Connection } from "../api/client";

interface ConnectionContextValue {
  connection: Connection;
  setConnection: (c: Connection) => void;
}

const STORAGE_KEY = "aus.connection";

const defaultConnection: Connection = {
  baseUrl: "http://localhost:8080",
  apiKey: "local-dev-key",
};

function load(): Connection {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) return { ...defaultConnection, ...JSON.parse(raw) };
  } catch {
    // ignore malformed storage
  }
  return defaultConnection;
}

const ConnectionContext = createContext<ConnectionContextValue | null>(null);

export function ConnectionProvider({ children }: { children: ReactNode }) {
  const [connection, setConnectionState] = useState<Connection>(load);

  const setConnection = useCallback((c: Connection) => {
    setConnectionState(c);
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(c));
    } catch {
      // storage unavailable; keep in-memory only
    }
  }, []);

  const value = useMemo(
    () => ({ connection, setConnection }),
    [connection, setConnection],
  );

  return (
    <ConnectionContext.Provider value={value}>
      {children}
    </ConnectionContext.Provider>
  );
}

export function useConnection(): ConnectionContextValue {
  const ctx = useContext(ConnectionContext);
  if (!ctx) throw new Error("useConnection must be used within ConnectionProvider");
  return ctx;
}
