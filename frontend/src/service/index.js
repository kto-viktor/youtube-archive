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
	async getAllVideos(page, size) {
		const res = await api.get(`/video/archives?page=${page}&size=${size}`);
		return res.data;
	}

	async searchVideos(query, page, size) {
		const res = await api.get(`/video/archives?page=${page}&size=${size}&query=${query}`)
		return res.data;
	}

	async saveVideo(url) {
		return await api.post(isDev ? '/video/archives/' : `/video?url=${url}`);
	}

	async checkVideoStatus(id) {
		return await api.get(`/video/archives/${id}`);
	}

	async getAllPlaylists(page, size) {
		const res = await api.get(isDev ? '/playlist/archives' : `/playlist/archives?page=${page}&size=${size}`);
		return res.data;
	}

	async searchPlaylists(query, page, size) {
		const res = await api.get(`/playlist/archives?page=${page}&size=${size}&query=${query}`)
		return res.data;
	}

	async savePlaylist(url) {
		return await api.post(`/playlist?url=${url}`);
	}

	async checkVideoListStatus(id) {
		return await api.get(`/playlist/archives/${id}`);
	}
}
