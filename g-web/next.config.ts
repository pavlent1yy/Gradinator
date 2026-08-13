/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
};

if (process.env.G_CORE_URL) {
  // Proxy /api/:path* -> ${G_CORE_URL}/:path*
  // This keeps G_CORE_URL server-side only and avoids CORS in dev.
  nextConfig.rewrites = async () => [
    {
      source: '/api/:path*',
      destination: `${process.env.G_CORE_URL}/:path*`
    }
  ];
}

module.exports = nextConfig;