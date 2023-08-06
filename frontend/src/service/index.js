import axios from 'axios';

const isDev = Boolean(import.meta.env.MODE === 'develop');
const baseURL = isDev ? 'http://localhost:3001/api/' : '/api';

const api = axios.create({
  baseURL,
	headers: {
		'Content-Type': 'application/json'
	},
});

export default class {
	async getAllVideos() {
		const res = await api.get('/video/archives?query=');
		return res.data;
	}

	async searchVideos(query) {
		const res = await api.get(`/video/archives?query=${query}`)
		return res.data;
	}

	async getVideoMetadata(link) {
		return await api.get(`/video/metadata?url=${link}`);
	}

	async saveVideo(data) {
		return await api.post(isDev ? '/video/archives/' : '/video', {
			...data
		});
	}

	async checkVideoStatus(id) {
		return await api.get(`/video/archives/${id}`);
	}

	async getAllPlaylists() {
		const res = await api.get(isDev ? '/playlist/archives' : '/playlist/archives?query=');
		return res.data;
	}

	async searchPlaylists(query) {
		const res = await api.get(`/playlist/archives?query=${query}`)
		return res.data;
	}

	async getPlaylistMetadata(query) {
		return await api.get(`/playlist/metadata?url=${query}`)
	}

	async savePlaylist() {
		return await api.post('/playlist', {
			data
		});
	}

	async checkVideoListStatus(id) {
		return await api.get(`/playlist/archives/${id}`);
	}
}
