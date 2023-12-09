<template>
  <div class="add-panel">
    <input
      id="link"
      v-model.trim="link"
      v-focus
      type="text"
      name="link"
      placeholder="Ссылка на видео в youtube"
    >

    <button
      type="button"
      class="button"
      @click="saveVideo(link)"
    >
      Сохранить в яндекс облако
    </button>
  </div>

  <div
    v-show="requestError"
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
    v-model.trim="search"
    type="text"
    placeholder="Искать в архиве"
  >

  <div
    v-if="videos.length > 0"
  >
    <VideosList
      :videos="videos"
    />
    <PaginationComponent
      :total-pages="totalPages"
      @page-change="changePage($event)"
    />
  </div>

  <div
    v-else-if="isListLoading"
    class="mt-20 text-center"
  >
    <SpinnerLoader />
  </div>

  <p
    v-else-if="listGetError"
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
</template>

<script>
import SpinnerLoader from '@/components/SpinnerLoader.vue';
import videosMixin from '@/mixins/videosMixin.js';
import VideosList from '@/components/VideosList.vue'
import PaginationComponent from "@/components/PaginationComponent.vue";

export default {
	components: {
    PaginationComponent,
		SpinnerLoader,
		VideosList,
	},

	mixins: [ videosMixin ],

  data() {
    return {
      videos: [],
			isVideoMode: true,
    }
  },

  mounted() {
		this.getAllItems();
  },

  methods: {
    async getAllItems() {
			try {
        let res = await this.$ServiceApi.getAllVideos(this.currentPage - 1, this.pageSize)
        this.videos = res.content;
        this.totalPages = res.totalPages;
			} catch (error) {
				console.error(error);
				this.listGetError = true;
			} finally {
				this.isListLoading = false;
			}
    },

    async searchVideos() {
			try {
        let res = await this.$ServiceApi.searchVideos(this.search, this.currentPage - 1, this.pageSize);
        this.videos = res.content;
        this.totalPages = res.totalPages;
			} catch (error) {
				console.error(error);
				this.listGetError = true;
			} finally {
				this.isListLoading = false;
			}
    },

    async searchArchive() {
      this.isListLoading = true;
      if (!this.search) {
        await this.getAllItems();
      }

      if (this.search) {
        await this.searchVideos();
      }
    },

    async saveVideo(url) {
      try {
				const res = await this.$ServiceApi.saveVideo(url);

        if (res.status === 200 || res.status === 201) {
          this.requestError = false;
          this.id = res.data;
          this.link = ''
          await this.addToVideoList();
        }
      } catch (error) {
        console.error(error);
        if (error.response.status === 409 || error.response.status === 400) {
          this.requestError = true;
          this.errorMessage = error.response.data;
        }
      } finally {
        this.link = ''
      }
    },

		async addToVideoList() {
			try {
				let res = await this.$ServiceApi.checkVideoStatus(this.id);
        let index = this.videos.findIndex(video => video.id === res.data.id)
        if (index === -1) {
          this.videos.unshift(res.data);
          index = 0
        } else {
          this.videos[index] = res.data;
        }
        if (this.videos.length > this.pageSize) this.videos.pop()
				while (res.data.status !== 'DOWNLOADED') {
					res = await this.$ServiceApi.checkVideoStatus(this.id);
          this.videos[index] = res.data
					await new Promise(resolve => setTimeout(resolve, 2500));

					if (res.data.status === "ERROR") {
						throw new Error(res.data.status);
					}
				}
			} catch (error) {
				console.error(error);
			}
		},
  },
}
</script>
