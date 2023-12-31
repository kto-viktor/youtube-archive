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
            requestError: false,
            errorMessage: '',
            isListLoading: true,
            listGetError: false,
            search: '',
            id: null,
            currentPage: 1,
            pageSize: 10,
            totalPages: 0
        }
    },

    methods: {
        sizeInfo(size) {
            return size > 0 ? ` (${size} мб)` : '(размер не известен)'
        },
        changePage(currentPage) {
            this.currentPage = currentPage
        }
    },

    watch: {
        search: debounce(function () {
            this.searchArchive();
        }, 1200),
        currentPage: debounce(function () {
            this.searchArchive();
        }, 1),
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