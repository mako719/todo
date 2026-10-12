// TODO の保存先とやり取りする関数群。
// 通常は Java API サーバー（/api/todos）を使い、
// NEXT_PUBLIC_STORAGE=local でビルドしたとき（GitHub Pages 版）はブラウザの localStorage を使う。

export type Todo = {
  id: number;
  title: string;
  done: boolean;
  createdAt: string; // "2026-10-12" 形式
};

type Patch = Partial<Pick<Todo, "title" | "done">>;

// 画面側はこの型だけを知っていればよく、保存先がどちらかを意識しない
type TodoApi = {
  list: () => Promise<Todo[]>;
  add: (title: string) => Promise<Todo>;
  update: (id: number, patch: Patch) => Promise<Todo>;
  remove: (id: number) => Promise<Todo>;
  clearDone: () => Promise<{ removed: number }>;
};

// ---- Java API サーバー版 ----

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

const httpApi: TodoApi = {
  list: () => request<Todo[]>(""),

  add: (title) =>
    request<Todo>("", { method: "POST", body: JSON.stringify({ title }) }),

  update: (id, patch) =>
    request<Todo>(`/${id}`, { method: "PATCH", body: JSON.stringify(patch) }),

  remove: (id) => request<Todo>(`/${id}`, { method: "DELETE" }),

  clearDone: () =>
    request<{ removed: number }>("/clear-done", { method: "POST" }),
};

// ---- localStorage 版（Java の TodoList と同じ動きをブラウザ内で再現する） ----

const STORAGE_KEY = "todos";

function load(): Todo[] {
  const json = localStorage.getItem(STORAGE_KEY);
  return json ? (JSON.parse(json) as Todo[]) : [];
}

function save(todos: Todo[]) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(todos));
}

function requireTitle(title: string): string {
  const trimmed = title.trim();
  if (!trimmed) throw new Error("タイトルが空です");
  return trimmed;
}

function find(todos: Todo[], id: number): Todo {
  const todo = todos.find((t) => t.id === id);
  if (!todo) throw new Error(`ID ${id} の TODO は見つかりません`);
  return todo;
}

function today(): string {
  // toISOString() は UTC になるため、ローカルの日付を自分で組み立てる
  const d = new Date();
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

const localApi: TodoApi = {
  list: async () => load(),

  add: async (title) => {
    const todos = load();
    const id = Math.max(0, ...todos.map((t) => t.id)) + 1;
    const todo: Todo = { id, title: requireTitle(title), done: false, createdAt: today() };
    save([...todos, todo]);
    return todo;
  },

  update: async (id, patch) => {
    const todos = load();
    const current = find(todos, id);
    const updated: Todo = {
      ...current,
      ...(patch.title !== undefined && { title: requireTitle(patch.title) }),
      ...(patch.done !== undefined && { done: patch.done }),
    };
    save(todos.map((t) => (t.id === id ? updated : t)));
    return updated;
  },

  remove: async (id) => {
    const todos = load();
    const todo = find(todos, id);
    save(todos.filter((t) => t.id !== id));
    return todo;
  },

  clearDone: async () => {
    const todos = load();
    const remaining = todos.filter((t) => !t.done);
    save(remaining);
    return { removed: todos.length - remaining.length };
  },
};

// NEXT_PUBLIC_ で始まる環境変数はビルド時にコードへ埋め込まれる
export const isLocalStorage = process.env.NEXT_PUBLIC_STORAGE === "local";

export const api: TodoApi = isLocalStorage ? localApi : httpApi;
