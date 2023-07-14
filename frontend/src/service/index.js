import axios from 'axios';

const isDev = import.meta.env.MODE === 'develop' ? 'develop' : 'production';
const baseURL = isDev ? 'http://localhost:3001/api/' : '/api';

const api = axios.create({
  baseURL,
	headers: {
		'Content-Type': 'application/json'
	},
});

export default class {
	async getAllVideos() {
		try {
			const res = await api.get('/video/archives?query=');
			return res.data;
		} catch (error) {
			throw new Error(error)
		}
	}

	async searchVideos(query) {
		try {
			const res = await api.get(`/video/archives?query=${query}`)
			return res.data;
		} catch (error) {
			throw new Error(error)
		}
	}

	async getMetadata(link) {
		try {
			const res = await api.get(`/video/metadata?url=${link}`);
			return res;

		} catch (error) {
			throw new Error(error);
		}
	}

	async saveVideo(data) {
		try {
			const res = await api.post(isDev ? '/video/archives/' : '/video', {
				data
			});

			return res;

		} catch (error) {
			throw new Error(error);
		}
	}

	async checkStatus(id) {
		try {
			return await api.get(`/archives/${id}`);
		} catch (error) {
			throw new Error(error);
		}
	}
}
