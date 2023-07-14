<template>
  <table 
    class="list-videos"
  >
    <tr
      v-for="video in videos"
      :key="video.id"
      class="list-videos__item"
    >
      <td>{{ video.title }}</td>
      <td>
        <a
          v-if="video.status === 'DOWNLOADED'"
          :href="videoHref(video)"
          download
          target="_blank"
        >
          {{ $options.statuses[video.status] }}
          {{ sizeInfo(video) }}
        </a>
        <p
          v-else-if="video.status === 'ERROR'"
          class="text-danger"
        >
          {{ $options.statuses[video.status] }}
        </p>
        <p
          v-else
          class="text-secondary"
        >
          {{ $options.statuses[video.status] }}
        </p>
      </td>
    </tr>
  </table>
</template>

<script>
	export default {
		props: {
			videos: {
				type: Array,
				default: () => []
			}
		},

		statuses: {
			'IN_PROGRESS': 'Загружается...',
			'DOWNLOADED': 'Скачать...',
			'ERROR': 'Ошибка загрузки :(',
  	},

		methods: {
			videoHref(video) {
				return this.baseDownloadUrl + '/' + video.downloadUrl.split('//')[1].split('/').slice(1).join('/')
			},

			sizeInfo(video) {
				return video.sizeMb > 0 ? ` (${video.sizeMb} мб)` : '(размер не известен)'
			},
		}
	}
</script>

<style lang="scss" scoped>

</style>