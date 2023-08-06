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
            isListLoading: true,
            isMetadataLoading: true,
            isError: false,
            search: '',
            id: null,
            videoInfo: {
                url: '',
                title: '',
                sizeMb: '',
                videos: []
            },
        }
    },

    methods: {
        openModal(fn) {
            if (this.link) {
                if (fn) {
                    this.getMetadata(fn);
                }
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
            this.videoInfo.videos = [];
        },

        sizeInfo(size) {
            return size > 0 ? ` (${size} мб)` : '(размер не известен)'
        },

        async getMetadata(fn) {
            try {
                this.isMetadataLoading = true;
                const res = await fn;

                if (res.status === 200 || res.status === 201) {
                    this.videoInfo.url = res.data.url;
                    this.videoInfo.title = res.data.title;
                    this.videoInfo.sizeMb = res.data.sizeMb;
                    this.videoInfo.videos = res.data.videos || [];
                }
            } catch (error) {
                console.error(error);
                this.popupError = true;
                this.link = '';
                if (typeof error.response !== 'undefined' && error.response.status === 400) {
                    this.errorMessage = error.response.data;
                } else {
                    this.errorMessage = 'Что то пошло не так :('
                }
            } finally {
                this.isMetadataLoading = false;
            }
        },
    },

    watch: {
        search: debounce(function () {
            this.searchArchive();
        }, 1200),
    },

    computed: {
        baseApiUrl() {
            return this.$isDev ? '/' : '/api';
        },

        baseDownloadUrl() {
            return this.$isDev ? '' : '/download';
        }
    }
}