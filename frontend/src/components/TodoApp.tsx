"use client";

import { useEffect, useState, type FormEvent } from "react";
import { api, type Todo } from "@/lib/api";
import TodoItem from "./TodoItem";

type Filter = "all" | "open" | "done";

const FILTERS: { value: Filter; label: string }[] = [
  { value: "all", label: "すべて" },
  { value: "open", label: "未完了" },
  { value: "done", label: "完了" },
];

const CONNECTION_ERROR =
  "API サーバーに接続できません。プロジェクト直下で ./run.sh serve を実行してください。";

export default function TodoApp() {
  const [todos, setTodos] = useState<Todo[]>([]);
  const [filter, setFilter] = useState<Filter>("all");
  const [title, setTitle] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // 初回表示時に一覧を取得する
  useEffect(() => {
    api
      .list()
      .then(setTodos)
      .catch(() => setError(CONNECTION_ERROR))
      .finally(() => setLoading(false));
  }, []);

  // API 呼び出しを共通化: 失敗したらエラーメッセージを画面に出す
  async function run(action: () => Promise<void>) {
    setError(null);
    try {
      await action();
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    }
  }

  // 一覧の1件をサーバーから返ってきた最新の値で置き換える
  function replaceTodo(updated: Todo) {
    setTodos((prev) => prev.map((t) => (t.id === updated.id ? updated : t)));
  }

  function handleAdd(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const trimmed = title.trim();
    if (!trimmed) return;
    run(async () => {
      const todo = await api.add(trimmed);
      setTodos((prev) => [...prev, todo]);
      setTitle("");
    });
  }

  const openCount = todos.filter((t) => !t.done).length;
  const doneCount = todos.length - openCount;
  const visible = todos.filter((t) =>
    filter === "all" ? true : filter === "open" ? !t.done : t.done,
  );
  const counts: Record<Filter, number> = {
    all: todos.length,
    open: openCount,
    done: doneCount,
  };

  return (
    <div className="space-y-4">
      <form onSubmit={handleAdd} className="flex gap-2">
        <input
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder="やることを入力…"
          aria-label="新しい TODO"
          className="min-w-0 flex-1 rounded-lg border border-zinc-300 bg-white px-4 py-2 outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-500/30 dark:border-zinc-700 dark:bg-zinc-900"
        />
        <button
          type="submit"
          disabled={!title.trim()}
          className="shrink-0 rounded-lg bg-blue-600 px-5 py-2 font-medium text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-40"
        >
          追加
        </button>
      </form>

      {error && (
        <p
          role="alert"
          className="rounded-lg border border-red-300 bg-red-50 px-4 py-2 text-sm text-red-700 dark:border-red-900 dark:bg-red-950 dark:text-red-300"
        >
          {error}
        </p>
      )}

      <div className="flex gap-1 rounded-lg bg-zinc-100 p-1 dark:bg-zinc-900">
        {FILTERS.map(({ value, label }) => (
          <button
            key={value}
            type="button"
            onClick={() => setFilter(value)}
            aria-pressed={filter === value}
            className={`flex-1 rounded-md px-3 py-1.5 text-sm transition-colors ${
              filter === value
                ? "bg-white font-medium shadow-sm dark:bg-zinc-700"
                : "text-zinc-500 hover:text-zinc-800 dark:hover:text-zinc-200"
            }`}
          >
            {label}
            <span className="ml-1.5 text-xs tabular-nums opacity-60">
              {counts[value]}
            </span>
          </button>
        ))}
      </div>

      <div className="overflow-hidden rounded-lg border border-zinc-200 dark:border-zinc-800">
        {loading ? (
          <p className="px-4 py-8 text-center text-sm text-zinc-400">
            読み込み中…
          </p>
        ) : visible.length === 0 ? (
          <p className="px-4 py-8 text-center text-sm text-zinc-400">
            {filter === "done" ? "完了した TODO はありません" : "TODO はありません"}
          </p>
        ) : (
          <ul className="divide-y divide-zinc-200 dark:divide-zinc-800">
            {visible.map((todo) => (
              <TodoItem
                key={todo.id}
                todo={todo}
                onToggle={() =>
                  run(async () => replaceTodo(await api.update(todo.id, { done: !todo.done })))
                }
                onRename={(newTitle) =>
                  run(async () => replaceTodo(await api.update(todo.id, { title: newTitle })))
                }
                onDelete={() =>
                  run(async () => {
                    await api.remove(todo.id);
                    setTodos((prev) => prev.filter((t) => t.id !== todo.id));
                  })
                }
              />
            ))}
          </ul>
        )}
      </div>

      {todos.length > 0 && (
        <div className="flex items-center justify-between text-sm text-zinc-500">
          <span>残り {openCount} 件</span>
          <button
            type="button"
            onClick={() =>
              run(async () => {
                await api.clearDone();
                setTodos((prev) => prev.filter((t) => !t.done));
              })
            }
            disabled={doneCount === 0}
            className="rounded px-2 py-1 hover:bg-zinc-100 hover:text-zinc-800 disabled:invisible dark:hover:bg-zinc-800 dark:hover:text-zinc-200"
          >
            完了済みを削除
          </button>
        </div>
      )}
    </div>
  );
}
