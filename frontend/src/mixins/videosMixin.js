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
			isLoading: true,
			isError: false,
      search: '',
      id: null,
    }
  },

	methods: {
		openModal() {
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
		
		sizeInfo(size) {
			return size > 0 ? ` (${size} мб)` : '(размер не известен)'
		},
	},

	computed: {
		baseApiUrl() {
      return this.$isDev ? '/' : '/api'
    },

    baseDownloadUrl() {
      return this.$isDev ? '' : '/download'
    }
	}
}