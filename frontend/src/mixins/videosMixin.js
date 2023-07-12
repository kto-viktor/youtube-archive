import { computed } from "vue"

const focus = {
  mounted: (el) => el.focus()
}

export default {
  directives: {
    focus
  },

	statuses: {
    'IN_PROGRESS': 'Загружается...',
    'DOWNLOADED': 'Скачать...',
    'ERROR': 'Ошибка загрузки :(',
  },

	methods: {
		sizeInfo(video) {
			return video.sizeMb > 0 ? ` (${video.sizeMb} мб)` : '(размер не известен)'
		}
	},

	computed: {
		videoHref() {
			return baseDownloadUrl + '/' + video.downloadUrl.split('//')[1].split('/').slice(1).join('/')
		}
	}
}