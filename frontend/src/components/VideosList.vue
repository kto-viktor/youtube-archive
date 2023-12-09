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
          {{ sizeInfo(video.sizeMb) }}
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
          {{ `${$options.statuses[video.status]} (${video.progress}%)` }}
        </p>
      </td>
    </tr>
  </table>
</template>

<script>
import videosMixin from '@/mixins/videosMixin';
import videoHrefMixin from '@/mixins/videoHrefMixin';

	export default {
		mixins: [videosMixin, videoHrefMixin ],

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
			sizeInfo(size) {
        return size > 0 ? ` (${size} мб)` : '(размер не известен)'
			},
		}
	}
</script>

<style lang="scss" scoped></style>