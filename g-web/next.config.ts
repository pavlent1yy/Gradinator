import type { NextConfig } from 'next';

const G_CORE_URL = process.env.G_CORE_URL || 'http://localhost:9091';

const nextConfig: NextConfig = {
  reactStrictMode: true,

  async rewrites() {
    return [
      {
        source: '/api/:path*',
        destination: `${G_CORE_URL}/:path*`,
      },
      // Вход через Google: браузер идёт на наш origin,
      // Next проксирует рукопожатие OAuth2 в g-core.
      {
        source: '/oauth2/:path*',
        destination: `${G_CORE_URL}/oauth2/:path*`,
      },
      // Колбэк Google после авторизации
      {
        source: '/login/oauth2/:path*',
        destination: `${G_CORE_URL}/login/oauth2/:path*`,
      },
    ];
  },
};

export default nextConfig;
