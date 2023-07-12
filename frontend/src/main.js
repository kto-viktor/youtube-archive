import { createApp } from 'vue';
import App from './App.vue';
import router from './router';
import Service from '@/service/';

const app = createApp(App);

app.config.globalProperties.$ServiceApi = new Service();
app.use(router)
app.mount('#app');
