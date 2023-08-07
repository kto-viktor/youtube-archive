import { createRouter, createWebHistory } from "vue-router";
import VideoPage from '@/pages/VideoPage.vue';
import PlaylistPage from '@/pages/PlaylistPage.vue';

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
				component: PlaylistPage
			},
			{ 
				path: '/:pathMatch(.*)', 
				component: () => import("@/pages/NotFoundPage.vue") 
			}
	]
})