<template>
  <div class="add-panel">
    <input
      id="link"
      v-model="link"
      v-focus
      type="text"
      name="link"
      placeholder="Ссылка на видео в youtube"
    >

    <button
      type="button"
      @click="saveModalHandler"
    >
      Сохранить в яндекс облако
    </button>
  </div>

  <div
    v-show="pageError"
    class="errorMessage"
  >
    <p
      class="text-danger"
    >
      Ошибка: {{ errorMessage }}
    </p>
  </div>

  <input
    id="search"
    v-model="search"
    type="text"
    placeholder="Искать в архиве"
  >

  <table 
    v-if="videos.length > 0"
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
          :href="baseDownloadUrl + '/' + video.downloadUrl.split('/').slice(-1)[0]"
          :download="`${video.title}.mp4`"
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

  <div
    v-else-if="isLoading"
    class="mt-20 text-center"
  >
    <SpinnerLoader />
  </div>

  <p
    v-else-if="isError"
    class="mt-20 text-danger"
  >
    Не удалось загрузить видео :(
  </p>

  <p
    v-else
    class="mt-20"
  >
    Видео не найдены
  </p>

  <div
    v-if="showPopup"
    ref="modal"
    class="popup"
    @click="clickCloseModal"
  >
    <form
      class="popup__content"
      @submit.prevent="saveVideo"
    >
      <input
        v-model="videoInfo.title"
        v-focus
        readonly
        placeholder="Название видео"
      >
      <p>Размер видео: {{ sizeInfo(videoInfo.sizeMb) }}</p>
      <button
        :disabled="popupError"
        type="submit"
      >
        сохранить
      </button>
      <div
        v-show="popupError"
        class="errorMessage"
      >
        <p
          class="text-danger"
        >
          Ошибка: {{ errorMessage }}
        </p>
      </div>
    </form>
  </div>
</template>

<script>
import debounce from 'lodash.debounce';
import SpinnerLoader from '@/components/SpinnerLoader.vue';
import videosMixin from '@/mixins/videosMixin.js';

export default {

	components: {
		SpinnerLoader
	},

	mixins: [ videosMixin ],

  data() {
    return {
      link: '',
      showPopup: false,
      popupError: false,
      pageError: false,
      errorMessage: '',
			isLoading: true,
      search: '',
      id: null,
      videos: [],
			isError: false,
			isVideoMode: true,
      videoInfo: {
        url: '',
        title: '',
        sizeMb: ''
      },
    }
  },

	computed: {},

  watch: {
    search: debounce(function () {
      this.searchArchive();
    }, 1200),
  },

  mounted() {
		this.getAllVideos();
  },

  methods: {
    saveModalHandler() {
      if (this.link) {
        this.getVideoMetadata()
        this.pageError = false;
        this.showPopup = true;
      }
    },

    clickCloseModal(e) {
      if (e.target.classList[0] === this.$refs.modal.classList[0]) {
        this.closeModal();
        this.clearMetadata();
      }
    },

    closeModal() {
      this.showPopup = false;
      this.popupError = false;
      this.link = '';
    },

    clearMetadata() {
      this.videoInfo.url = '';
      this.videoInfo.title = '';
      this.videoInfo.sizeMb = '';
    },

    async getAllVideos() {
			try {
				const data = await this.$ServiceApi.getAllVideos();
      	this.videos = data;
			} catch (error) {
				console.error(error);
				this.isError = true;
			} finally {
				this.isLoading = false;
			}
    },

    async searchVideos() {
			try {
				const data = await this.$ServiceApi.getAllVideos(this.search);
      	this.videos = data;
			} catch (error) {
				console.error(error);
				this.isError = true;
			}
    },

    async getVideoMetadata() {
      try {
        const res = await this.$ServiceApi.getMetadata(this.link);

        if (res.status === 200 || res.status === 201) {
          this.videoInfo.url = res.data.url;
          this.videoInfo.title = res.data.title;
          this.videoInfo.sizeMb = res.data.sizeMb;
        }
      } catch (error) {
        console.error(error);
        if (error.response.status === 400) {
          this.popupError = true;
          this.errorMessage = error.response.data;
          this.link = '';
        }
      }
    },

    async searchArchive() {
      if (!this.search) {
        await this.getAllVideos();
      }

      if (this.search) {
        await this.searchVideos();
      }
    },

    async saveVideo() {
      if (!this.videoInfo.title) {
        return
      }
      this.closeModal()

      const data = JSON.stringify({
        url: this.videoInfo.url,
        title: this.videoInfo.title,
        sizeMb: this.videoInfo.sizeMb
      })

      this.clearMetadata()

      try {
				console.log(data);
				const res = await this.$ServiceApi.saveVideo(data);

        if (res.status === 200 || res.status === 201) {
          this.id = res.data;
          await this.addToList();
        }

      } catch (error) {
        console.error(error);
        if (error.response.status === 409 || error.response.status === 400) {
          this.pageError = true;
          this.errorMessage = error.response.data;
        }
      }
    },

		async addToList() {
			try {
				let res = await this.$ServiceApi.checkStatus(this.id);
				this.videos.unshift(res.data);

				while (res.data.status !== 'DOWNLOADED') {
					res = await this.$ServiceApi.checkStatus(this.id);
					await new Promise(resolve => setTimeout(resolve, 2500));
					
					if (res.data.status === "ERROR") {
						throw new Error(res.data.status);
					}
				}

				this.videos = this.videos.map((el) => {
					if (el.id === res.data.id) {
						return {
							...el,
							downloadUrl: res.data.downloadUrl,
							status: "DOWNLOADED"
						}
					}

					return el
				})
			} catch (error) {
				this.videos = this.videos.map((el) => {
					if (el.id === res.data.id) {
						return {
							...el,
							status: "ERROR"
						}
					}

					return el
				})

				console.error(error);
				return;
			}
		},
  },
}
</script>
