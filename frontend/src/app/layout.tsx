import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import "./globals.css";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "TODO",
  description: "Java + Next.js の TODO アプリ",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    // ブラウザ拡張機能（Foxified など）が <html> に属性を追加すると、
    // サーバーの HTML と食い違ってハイドレーションエラーになるため、この要素の属性差分だけ無視する
    <html
      lang="ja"
      className={`${geistSans.variable} ${geistMono.variable} h-full antialiased`}
      suppressHydrationWarning
    >
      <body className="min-h-full flex flex-col">{children}</body>
    </html>
  );
}
