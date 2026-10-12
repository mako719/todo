import TodoApp from "@/components/TodoApp";

export default function Home() {
  return (
    <main className="mx-auto w-full max-w-xl px-4 py-12">
      <h1 className="mb-6 text-3xl font-bold tracking-tight">TODO</h1>
      <TodoApp />
    </main>
  );
}
