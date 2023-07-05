import {createRouter, createWebHistory} from "vue-router";
import VideoPage from "./components/VideoPage.vue"
import PlaylistPage from "./components/PlaylistPage.vue"
import NotFoundPage from "./components/NotFoundPage.vue"

export default createRouter({
    history: createWebHistory(),
    routes: [
        { path: '/video', component: VideoPage, alias: '/'},
        { path: '/playlist', component: PlaylistPage},
        { path: '/:pathMatch(.*)', component: NotFoundPage }
    ]
})