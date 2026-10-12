import { useRef, useState } from "react";
import type { Todo } from "@/lib/api";

type Props = {
  todo: Todo;
  onToggle: () => void;
  onRename: (title: string) => void;
  onDelete: () => void;
};

export default function TodoItem({ todo, onToggle, onRename, onDelete }: Props) {
  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState(todo.title);
  // Esc で編集を取り消したかどうか（onBlur で保存処理をスキップするため）
  const cancelled = useRef(false);

  function startEdit() {
    setDraft(todo.title);
    cancelled.current = false;
    setEditing(true);
  }

  // 編集の確定は onBlur に一本化する（Enter / Esc も blur を起こして、ここに来る）
  function finishEdit() {
    setEditing(false);
    const title = draft.trim();
    if (!cancelled.current && title && title !== todo.title) {
      onRename(title);
    }
  }

  return (
    <li className="group flex items-center gap-3 px-4 py-3">
      <input
        type="checkbox"
        checked={todo.done}
        onChange={onToggle}
        aria-label={`「${todo.title}」を${todo.done ? "未完了に戻す" : "完了にする"}`}
        className="size-5 shrink-0 cursor-pointer accent-blue-600"
      />

      {editing ? (
        <input
          autoFocus
          value={draft}
          onChange={(e) => setDraft(e.target.value)}
          onBlur={finishEdit}
          onKeyDown={(e) => {
            if (e.key === "Enter" && !e.nativeEvent.isComposing) {
              e.currentTarget.blur();
            } else if (e.key === "Escape") {
              cancelled.current = true;
              e.currentTarget.blur();
            }
          }}
          aria-label="タイトルを編集"
          className="min-w-0 flex-1 rounded border border-blue-500 bg-transparent px-2 py-1 outline-none"
        />
      ) : (
        <span
          onDoubleClick={startEdit}
          title="ダブルクリックで編集"
          className={`min-w-0 flex-1 break-words ${
            todo.done ? "text-zinc-400 line-through dark:text-zinc-500" : ""
          }`}
        >
          {todo.title}
        </span>
      )}

      <time
        dateTime={todo.createdAt}
        className="hidden shrink-0 text-xs text-zinc-400 tabular-nums sm:block"
      >
        {todo.createdAt}
      </time>

      {/* PC ではホバー時だけ表示、スマホでは常に表示 */}
      <div className="flex shrink-0 gap-1 transition-opacity sm:opacity-0 sm:group-focus-within:opacity-100 sm:group-hover:opacity-100">
        <button
          type="button"
          onClick={startEdit}
          className="rounded px-2 py-1 text-sm text-zinc-500 hover:bg-zinc-100 dark:hover:bg-zinc-800"
        >
          編集
        </button>
        <button
          type="button"
          onClick={onDelete}
          className="rounded px-2 py-1 text-sm text-red-600 hover:bg-red-50 dark:text-red-400 dark:hover:bg-red-950"
        >
          削除
        </button>
      </div>
    </li>
  );
}
