/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  env: {
    SSO_SERVER_URL: process.env.SSO_SERVER_URL || 'http://localhost:8081',
    NEXT_PUBLIC_SSO_SERVER_URL: process.env.NEXT_PUBLIC_SSO_SERVER_URL || 'http://localhost:8081',
  },
  output: 'standalone',
  async headers() {
    return [
      {
        source: '/(.*)',
        headers: [
          { key: 'X-Content-Type-Options', value: 'nosniff' },
          { key: 'X-Frame-Options', value: 'DENY' },
          { key: 'X-XSS-Protection', value: '1; mode=block' },
          { key: 'Referrer-Policy', value: 'strict-origin-when-cross-origin' },
          { key: 'Permissions-Policy', value: 'camera=(), microphone=(), geolocation=()' },
        ],
      },
    ];
  },
  async rewrites() {
    return [
      {
        source: '/api/sso/:path*',
        destination: `${process.env.SSO_SERVER_URL || 'http://localhost:8081'}/api/v1/:path*`,
      },
    ];
  },
};

module.exports = nextConfig;
