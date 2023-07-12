import { createRouter, createWebHistory } from "vue-router";
import VideoPage from '@/pages/VideoPage.vue';

export default createRouter({
	history: createWebHistory(),
	routes: [
			{ 
				path: '/',
				redirect: { path: "/video" },
			},
			{ 
				path: '/video',
				component: VideoPage,
			},
			{ 
				path: '/playlist', 
				component: () => import("@/pages/PlaylistPage.vue")
			},
			{ 
				path: '/:pathMatch(.*)', 
				component: () => import("@/pages/NotFoundPage.vue") 
			}
	]
})