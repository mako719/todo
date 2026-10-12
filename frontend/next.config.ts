import type { NextConfig } from "next";

// GitHub Pages 用のビルドかどうか（GitHub Actions で GITHUB_PAGES=true を指定する）
const isGitHubPages = process.env.GITHUB_PAGES === "true";

// Java の API サーバーの URL（./run.sh serve で起動）
const apiUrl = process.env.API_URL ?? "http://localhost:8080";

const nextConfig: NextConfig = {
  /* config options here */
  // Cache Components（PPR）は静的エクスポートと併用できないため、Pages 用ビルドでは無効にする
  cacheComponents: !isGitHubPages,
  partialPrefetching: !isGitHubPages,
  turbopack: {
    rules: {
      "*.css": {
        loaders: ["@tailwindcss/turbopack"],
        as: "*.css",
      },
    },
  },
  ...(isGitHubPages
    ? {
        // 静的な HTML/JS/CSS を out/ に書き出す（GitHub Pages はサーバー処理ができないため）
        output: "export",
        // https://<ユーザー名>.github.io/<リポジトリ名>/ で公開されるので、パスの先頭にリポジトリ名を付ける
        basePath: process.env.PAGES_BASE_PATH ?? "",
      }
    : {
        // ブラウザからの /api/* を Java サーバーへ転送する。
        // ブラウザから見ると同じオリジンへのリクエストになるので CORS 設定が不要になる。
        // （静的エクスポートでは rewrites が使えないため、ローカル開発時だけ有効にする）
        async rewrites() {
          return [{ source: "/api/:path*", destination: `${apiUrl}/api/:path*` }];
        },
      }),
};

export default nextConfig;
