module.exports = {
  apps: [
    {
      name: 'authfusion-admin',
      script: '.next/standalone/server.js',
      cwd: '/home/ju/authfusion-platform/products/admin-console',
      env: {
        NODE_ENV: 'production',
        PORT: 3001,
        NEXT_PUBLIC_SSO_SERVER_URL: 'https://sso.aines.kr',
      },
      instances: 1,
      autorestart: true,
      max_memory_restart: '512M',
      log_date_format: 'YYYY-MM-DD HH:mm:ss',
      error_file: '/home/ju/authfusion-platform/logs/admin-console-error.log',
      out_file: '/home/ju/authfusion-platform/logs/admin-console-out.log',
    },
  ],
};
