import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path';

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],

	server: {
    proxy: {
      '^/api': {
        target: 'https://xn----7sbbdd9a5ctjp6h.online',
        changeOrigin: true,
      },
    },
		open: true,
		port: 3000,
	},

	resolve: {
		alias: [{ find: '@', replacement: path.resolve(__dirname, 'src') }],
	},
})
