// Java API サーバー（/api/todos）を呼び出す関数群

export type Todo = {
  id: number;
  title: string;
  done: boolean;
  createdAt: string; // "2026-10-12" 形式
};

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`/api/todos${path}`, {
    ...init,
    headers: { "Content-Type": "application/json", ...init?.headers },
  });
  // エラー時も API は {"error": "..."} を返すので、まず JSON として読む
  const body = await res.json().catch(() => null);
  if (!res.ok) {
    throw new Error(body?.error ?? `API エラー (HTTP ${res.status})`);
  }
  return body as T;
}

export const api = {
  list: () => request<Todo[]>(""),

  add: (title: string) =>
    request<Todo>("", { method: "POST", body: JSON.stringify({ title }) }),

  update: (id: number, patch: Partial<Pick<Todo, "title" | "done">>) =>
    request<Todo>(`/${id}`, { method: "PATCH", body: JSON.stringify(patch) }),

  remove: (id: number) => request<Todo>(`/${id}`, { method: "DELETE" }),

  clearDone: () =>
    request<{ removed: number }>("/clear-done", { method: "POST" }),
};
