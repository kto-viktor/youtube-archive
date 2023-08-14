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
      @click="savePlaylist(link)"
    >
      сохранить в яндекс облако
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
    v-else-if="listGetError"
    class="mt-20 text-danger"
  >
    Не удалось загрузить плейлисты :(
  </p>

  <p
    v-else
    class="mt-20"
  >
    Плейлисты не найдены
  </p>

  <div>
    <PaginationComponent
      :total-pages="totalPages"
      @page-change="changePage($event)"
    />
  </div>
</template>

<script>
import SpinnerLoader from '@/components/SpinnerLoader.vue';
import VideosList from '@/components/VideosList.vue';
import videosMixin from '@/mixins/videosMixin.js';
import videoHrefMixin from '@/mixins/videoHrefMixin';
import PaginationComponent from "@/components/PaginationComponent.vue";

export default {
	components: {
    PaginationComponent,
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
    this.getAllItems();
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

    async getAllItems() {
			try {
        let res = await this.$ServiceApi.getAllPlaylists(this.currentPage - 1, this.pageSize);
        this.videos = res.content;
        this.totalPages = res.totalPages;
			} catch (error) {
				this.listGetError = true;
			} finally {
				this.isListLoading = false;
			}
    },

    async searchPlaylists() {
			try {
        let res = await this.$ServiceApi.searchPlaylists(this.search, this.currentPage - 1, this.pageSize);
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
        await this.searchPlaylists();
      }
    },

    async savePlaylist(url) {
      try {
				const res = await this.$ServiceApi.savePlaylist(url);

        if (res.status === 200 || res.status === 201) {
          this.id = res.data;
          await this.addPlaylistToList();
        }

      } catch (error) {
        console.error(error);
        if (error.response.status === 409 || error.response.status === 400) {
          this.requestError = true;
          this.errorMessage = error.response.data;
        }
      }
    },

    async addPlaylistToList() {
      try {
        let res = await this.$ServiceApi.checkVideoListStatus(this.id);
        this.playlists.unshift(res.data);
        if (this.playlists.length > this.pageSize) this.playlists.pop()
        let index = this.playlists.findIndex(playlist => playlist.url === res.data.url)
        if (index === -1) {
          this.playlists.unshift(res.data);
        } else {
          this.playlists[index] = res.data;
        }
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