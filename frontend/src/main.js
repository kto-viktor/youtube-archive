import { createApp } from 'vue';
import App from './App.vue';
import router from './router';
import Service from '@/service/';

const app = createApp(App);

app.config.globalProperties.$ServiceApi = new Service();
app.config.globalProperties.$isDev = import.meta.env.MODE === 'develop' ? 'develop' : 'production';
app.use(router);
app.mount('#app');
