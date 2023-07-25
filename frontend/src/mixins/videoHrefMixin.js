export default {
	methods: {
		videoHref(video) {
			return `${this.baseDownloadUrl}${new URL(video.downloadUrl).pathname}`;
		},
	}
}