import type { NextConfig } from "next";

// Java の API サーバーの URL（./run.sh serve で起動）
const apiUrl = process.env.API_URL ?? "http://localhost:8080";

const nextConfig: NextConfig = {
  /* config options here */
  cacheComponents: true,
  partialPrefetching: true,
  turbopack: {
    rules: {
      "*.css": {
        loaders: ["@tailwindcss/turbopack"],
        as: "*.css",
      },
    },
  },
  // ブラウザからの /api/* を Java サーバーへ転送する。
  // ブラウザから見ると同じオリジンへのリクエストになるので CORS 設定が不要になる。
  async rewrites() {
    return [{ source: "/api/:path*", destination: `${apiUrl}/api/:path*` }];
  },
};

export default nextConfig;
