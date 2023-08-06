<template>
  <div class="add-panel">
    <input
      id="link"
      v-model="link"
      v-focus
      type="text"
      name="link"
      placeholder="Ссылка на плейлист в youtube"
    >

    <button
      type="button"
      class="button"
      @click="openModal($ServiceApi.getPlaylistMetadata(link))"
    >
      сохранить в яндекс облако
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
  <ul
    v-if="playlists.length > 0"
    class="list-playlists"
  >
    <li
      v-for="playlist in playlists"
      :key="playlist.id"
      class="list-playlists__item"
    >
      <div class="list-playlists__item-heading">
        <span>
          {{ playlist.title }}
        </span>
	
        <div class="buttons-group">
          <button
            class="button button_sm"
            :class="{ 'button_active': playlist.showDropdown }"
            @click="switchDropdown(playlist.id)"
          >
            {{ !playlist.showDropdown ? 'Смотреть список видео' : 'Скрыть список видео' }}  
          </button>
	
          <button
            class="button button_sm"
            @click="downloadAllVideosFromPlaylist(playlist.id)"
          >
            Скачать
          </button>
        </div>
      </div>

      <VideosList
        v-show="playlist.showDropdown"
        :videos="playlist.videoArchives"
      />
    </li>
  </ul>

  <div
    v-else-if="isListLoading"
    class="mt-20 text-center"
  >
    <SpinnerLoader />
  </div>

  <p
    v-else-if="isError"
    class="mt-20 text-danger"
  >
    Не удалось загрузить плейлист :(
  </p>

  <p
    v-else
    class="mt-20"
  >
    Плейлисты не найдены
  </p>

  <div
    v-if="showPopup"
    ref="modal"
    class="popup"
    @click="clickCloseModal"
  >
    <form
      class="popup__content__playlist"
      @submit.prevent="savePlaylist"
    >
      <div
        v-if="isMetadataLoading"
        class="text-center spinner-wrapper"
      >
        <SpinnerLoader
          width="30px"
          height="30px"
          border-width="4px"
        />
      </div>

      <input
        v-else
        v-model="videoInfo.title"
        v-focus
        readonly
        placeholder="Название плейлиста"
      >
      <ul
        v-if="videoInfo.videos.length > 0"
        class="list-playlist-videos"
      >
        <li
          v-for="video in videoInfo.videos"
          :key="video.id"
          class="list-playlist-videos__item"
        >
          <span>{{ video.title }}</span>
          <p>Размер видео: {{ sizeInfo(video) }}</p>
        </li>
      </ul>
      <button
        :disabled="popupError"
        type="submit"
        class="button"
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
import SpinnerLoader from '@/components/SpinnerLoader.vue';
import VideosList from '@/components/VideosList.vue';
import videosMixin from '@/mixins/videosMixin.js';
import videoHrefMixin from '@/mixins/videoHrefMixin';

export default {
	components: {
		SpinnerLoader,
		VideosList,
	},

	mixins: [ 
		videosMixin, 
		videoHrefMixin,
	],

  data() {
    return {
      playlists: [],
    }
  },

  mounted() {
    this.getAllPlaylists();
  },

  methods: {
    switchDropdown(id) {
      const index = this.getIndex(id)
      this.playlists[index].showDropdown = !this.playlists[index].showDropdown
    },

    getIndex(id) {
      return this.playlists.findIndex(playlist => playlist.id === id)
    },

    downloadAllVideosFromPlaylist(id) {
      const a = document.createElement("a");
      a.setAttribute('target', '_blank');
      this.playlists[this.getIndex(id)].videoArchives.forEach(el => {
        if (el.status === "DOWNLOADED") {
          a.setAttribute('href', this.videoHref(el.downloadUrl));
          a.setAttribute('download', el.title);
          a.click();
        }
      })
    },

    async getAllPlaylists() {
			try {
        const data = await this.$ServiceApi.getAllPlaylists();
        this.playlists = data;
			} catch (error) {
				this.isError = true;
			} finally {
				this.isListLoading = false;
			}
    },

    async searchPlaylists() {
			this.isListLoading = true;
			try {
        const data = await this.$ServiceApi.searchPlaylists(this.search);
        this.playlists = data;
			} catch (error) {
				console.error(error);
				this.isError = true;
			} finally {
				this.isListLoading = false;
			}
    },

    async searchArchive() {
      if (!this.search) {
        await this.getAllPlaylists();
      }

      if (this.search) {
        await this.searchPlaylists();
      }
    },

    async savePlaylist() {
      if (!this.videoInfo.title) {
        return;
      }

      this.closeModal()

      const data = {
        url: this.videoInfo.url,
        title: this.videoInfo.title,
        sizeMb: this.videoInfo.sizeMb,
        videos: this.videoInfo.videos
      }

      this.clearMetadata()

      try {
				const res = await this.$ServiceApi.savePlaylist(data);

        if (res.status === 200 || res.status === 201) {
          this.id = res.data;
          await this.addPlaylistToList();
        }

      } catch (error) {
        console.error(error);
        if (error.response.status === 409 || error.response.status === 400) {
          this.pageError = true;
          this.errorMessage = error.response.data;
        }
      }
    },

    async addPlaylistToList() {
      try {
        let res = await this.$ServiceApi.checkVideoListStatus(this.id);
        this.playlists.unshift(res.data);
        await this.listenForStatusChange(res.data.id);
      } catch (error) {
        console.error(error);
      }
    },

    async listenForStatusChange(id) {
      try {
        let res, playlistDownloaded;
        do {
          res = await this.$ServiceApi.checkVideoListStatus(this.id);
          const playlistIndex = this.getIndex(id)
          this.playlists[playlistIndex] = {...this.playlists[playlistIndex], ...res.data}
          playlistDownloaded = this.playlists[playlistIndex].videoArchives.reduce((acc, curr) => {
            return acc && (curr.status === 'DOWNLOADED' || curr.status === 'ERROR')
          }, true)
          await new Promise(resolve => setTimeout(resolve, 2500));
        } while (!playlistDownloaded);
      } catch (error) {
        console.error(error);
      }
    }
  },
}
</script>


<style lang="scss" scoped>
.buttons-group {
	display: flex;
	gap: 10px;

	@media (max-width: 1023px) {
		flex-direction: column;
	}
}
</style>