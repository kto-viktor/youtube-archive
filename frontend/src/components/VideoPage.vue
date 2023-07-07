<template>
  <div class="container">
        <header class="header">
      <a href="/playlist">Плейлисты</a>
        </header>
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

      <ul class="list-videos">
        <li
          v-for="video in videos"
          :key="video.id"
          class="list-videos__item"
        >
          <span>{{ video.title }}</span>
          <a
            v-if="video.status === 'DOWNLOADED'"
            :href="baseDownloadUrl + '/' + video.downloadUrl.split('/').slice(-1)[0]"
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
      class="popup__content"
      @submit.prevent="saveVideo"
    >
      <input
        v-model="videoInfo.title"
        v-focus
        readonly
        placeholder="Название видео"
      >
      <p>Размер видео: {{ videoInfo.sizeMb > 0 ? `${videoInfo.sizeMb} мб` : 'размер не известен' }}</p>
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
      videos: null,
      videoInfo: {
        url: '',
        title: '',
        sizeMb: ''
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
      const res = await axios.get(`${this.baseApiUrl}/video/archives?query=`);
      this.videos = res.data;
    },

    async searchVideos() {
      const res = await axios.get(`${this.baseApiUrl}/video/archives?query=${this.search}`)
      this.videos = res.data;
    },

    async getVideoMetadata() {
      try {
        const res = await axios.get(`${this.baseApiUrl}/video/metadata?url=${this.link}`);

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

      // this.showPopup = false;
      // this.link = '';

      const data = JSON.stringify({
        url: this.videoInfo.url,
        title: this.videoInfo.title,
        sizeMb: this.videoInfo.sizeMb
      })
      this.clearMetadata()

      try {
        const res = await axios(`${this.baseApiUrl}/video`, {
          method: 'post',
          headers: {
            'Content-Type': 'application/json'
          },
          data
        });

        if (res.status === 200 || res.status === 201) {
          this.id = res.data;
          await this.addVideoToList();
        }

      } catch (error) {
        console.error(error);
        if (error.response.status === 409 || error.response.status === 400) {
          this.pageError = true;
          this.errorMessage = error.response.data;
        }
      }
    },

    async addVideoToList() {
      try {
        let res = await axios.get(`${this.baseApiUrl}/video/archives/${this.id}`)
        this.videos.unshift(res.data);
        await this.listenForStatusChange(res.data.id);
      } catch (error) {
        console.error(error);
      }
    },

    async listenForStatusChange(id) {
      try {
        let res;
        do {
          res = await axios.get(`${this.baseApiUrl}/video/archives/${this.id}`);
          await new Promise(resolve => setTimeout(resolve, 2500));

          if (res.data.status === "ERROR") {
            throw new Error(res.data.status);
          }
        } while (res.data.status !== 'DOWNLOADED');
        this.videos = this.videos.map((el) => {
          if (el.id === id) {
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
          if (el.id === id) {
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
    }
  },
}
</script>

<style lang="scss">
@import '@/assets/main.scss';
</style>
