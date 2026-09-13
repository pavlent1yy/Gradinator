import type { NextConfig } from 'next';

const nextConfig: NextConfig = {
  reactStrictMode: true,

  async rewrites() {
    return [
      {
        source: '/api/:path*',
        destination: `${process.env.G_CORE_URL}/:path*`,
      },
    ];
  },
};

export default nextConfig;
