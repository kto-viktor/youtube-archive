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

	async getVideoMetadata(link) {
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
				...data
			});

			return res;

		} catch (error) {
			throw new Error(error);
		}
	}

	async checkVideoStatus(id) {
		try {
			return await api.get(`/video/archives/${id}`);
		} catch (error) {
			throw new Error(error);
		}
	}

	async getAllPlaylists() {
		try {
			const res = await api.get(isDev ? '/playlist/archives' : '/playlist/archives?query=');
			return res.data;
		} catch (error) {
			throw new Error(error);
		}
	}

	async searchPlaylists(query) {
		try {
			const res = await api.get(`/playlist/archives?query=${query}`)
			return res.data;
		} catch (error) {
			throw new Error(error)
		}
	}

	async getPlaylistMetadata(query) {
		try {
			const res = await api.get(`/playlist/metadata?url=${query}`)
			return res;
		} catch (error) {
			throw new Error(error)
		}
	}

	async savePlaylist() {
		try {
			const res = await api.post('/playlist', {
				data
			});

			return res;

		} catch (error) {
			throw new Error(error);
		}
	}

	async checkVideoListStatus(id) {
		try {
			return await api.get(`/playlist/archives/${id}`);
		} catch (error) {
			throw new Error(error);
		}
	}
}
