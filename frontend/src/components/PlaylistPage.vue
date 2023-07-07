<template>
  <div class="container">
    <header class="header">
      <a href="/video">Видео</a>
    </header>
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
        @click="saveModalHandler"
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

    <main class="body">
      <input
        id="search"
        v-model="search"
        type="text"
        placeholder="Искать в архиве"
      >
      <ul class="list-playlists">
        <li
          v-for="playlist in playlists"
          :key="playlist.id"
          class="list-playlists__item"
        >
          <div class="dropdown-line">
            <span
              @click="switchDropdown(playlist.id)"
            >
              {{ playlist.title }}
            </span>
            <button @click="downloadAllVideosFromPlaylist(playlist.id)">
              Скачать весь плейлист
            </button>
          </div>
          <ul
            v-if="playlist.showDropdown"
            class="list-videos"
          >
            <li
              v-for="video in playlist.videoArchives"
              :key="video.id"
              class="list-videos__item"
            >
              <span>{{ video.title }}</span>
              <a
                v-if="video.status === 'DOWNLOADED'"
                :href="convertDownloadUrlToProxy(video.downloadUrl)"
                :download="`${video.title}.mp4`"
                target="_blank"
              >
                {{ $options.statuses[video.status] }}
                {{ video.sizeMb > 0 ? ` (${video.sizeMb} мб)` : '(размер не известен)' }}
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
            </li>
          </ul>
        </li>
      </ul>
    </main>

    <footer class="footer">
      <p>
        by <a
          href="https://true-programmers.com"
          target="_blank"
        >true-programmers.com</a>
      </p>
    </footer>
  </div>

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
      <input
        v-model="playlistInfo.title"
        v-focus
        readonly
        placeholder="Название плейлиста"
      >
      <ul class="list-playlist-videos">
        <li
          v-for="video in playlistInfo.videos"
          :key="video.id"
          class="list-playlist-videos__item"
        >
          <span>{{ video.title }}</span>
          <p>Размер видео: {{ video.sizeMb > 0 ? `${video.sizeMb} мб` : 'размер не известен' }}</p>
        </li>
      </ul>
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
import axios from 'axios';
import debounce from 'lodash.debounce';

const focus = {
  mounted: (el) => el.focus()
}

export default {

  directives: {
    focus
  },

  data() {
    return {
      link: '',
      showPopup: false,
      popupError: false,
      pageError: false,
      errorMessage: '',
      search: '',
      id: null,
      playlists: null,
      playlistInfo: {
        url: '',
        title: '',
        videos: null
      },
    }
  },

  statuses: {
    'IN_PROGRESS': 'Загружается...',
    'DOWNLOADED': 'Скачать...',
    'ERROR': 'Ошибка загрузки :(',
  },

  computed: {
    baseApiUrl() {
      // return import.meta.env.BASE_URL
      return '/api'
    },
    baseDownloadUrl() {
      // return import.meta.env.DOWNLOAD_BASE_URL
      return '/download'
    }
  },

  watch: {
    search: debounce(function () {
      this.searchArchive();
    }, 1200),
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

    saveModalHandler() {
      if (this.link) {
        this.getPlaylistMetadata()
        this.showPopup = true;
      }
    },

    clickCloseModal(e) {
      if (e.target.classList[0] === this.$refs.modal.classList[0]) {
        this.closeModal();
        this.clearMetadata()
      }
    },

    closeModal() {
      this.showPopup = false;
      this.link = '';
    },

    clearMetadata() {
      this.playlistInfo.url = '';
      this.playlistInfo.title = '';
      this.playlistInfo.sizeMb = '';
    },

    convertDownloadUrlToProxy(url) {
      return this.baseDownloadUrl + '/' + url.split('/').slice(-1)[0];
    },

    downloadAllVideosFromPlaylist(id) {
      const a = document.createElement("a");
      a.setAttribute('target', '_blank');
      this.playlists[this.getIndex(id)].videoArchives.forEach(el => {
        if (el.status === "DOWNLOADED") {
          // window.open(this.convertDownloadUrlToProxy(el.downloadUrl), "download")
          a.setAttribute('href', this.convertDownloadUrlToProxy(el.downloadUrl));
          a.setAttribute('download', el.title);
          a.click();
        }
      })
    },

    async getAllPlaylists() {
      const res = await axios.get(`${this.baseApiUrl}/playlist/archives?query=`);
      this.playlists = res.data;
    },

    async searchPlaylists() {
      const res = await axios.get(`${this.baseApiUrl}/playlist/archives?query=${this.search}`)
      this.playlists = res.data;
    },

    async getPlaylistMetadata() {
      try {
        const res = await axios.get(`${this.baseApiUrl}/playlist/metadata?url=${this.link}`);

        if (res.status === 200 || res.status === 201) {
          this.playlistInfo.url = res.data.url;
          this.playlistInfo.title = res.data.title;
          this.playlistInfo.videos = res.data.videos;
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
        await this.getAllPlaylists();
      }

      if (this.search) {
        await this.searchPlaylists();
      }
    },

    async savePlaylist() {
      if (!this.playlistInfo.title) {
        return
      }
      this.closeModal()

      // this.showPopup = false;
      // this.link = '';

      const data = JSON.stringify({
        url: this.playlistInfo.url,
        title: this.playlistInfo.title,
        videos: this.playlistInfo.videos
      })
      this.clearMetadata()

      try {
        const res = await axios(`${this.baseApiUrl}/playlist`, {
          method: 'post',
          headers: {
            'Content-Type': 'application/json'
          },
          data
        });

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
        let res = await axios.get(`${this.baseApiUrl}/playlist/archives/${this.id}`)
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
          res = await axios.get(`${this.baseApiUrl}/playlist/archives/${this.id}`);
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

<style lang="scss">
@import '@/assets/main.scss';
</style>
