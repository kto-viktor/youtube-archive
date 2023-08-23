<template>
  <div class="pagination">
    <button
      v-if="totalPages > 1"
      class="pagination__page"
      :disabled="innerCurrentPage === 1"
      @click="changePage(innerCurrentPage - 1)"
    >
      Prev
    </button>
    <div class="pagination__pages">
      <span
        v-for="page in totalPages"
        :key="page"
        class="pagination__page"
        :class="{ 'pagination__page_active': page === innerCurrentPage }"
        @click="changePage(page)"
      >
        {{ page }}
      </span>
    </div>
    <button
      v-if="totalPages > 1"
      class="pagination__page"
      :disabled="innerCurrentPage === totalPages"
      @click="changePage(innerCurrentPage + 1)"
    >
      Next
    </button>
  </div>
</template>

<script>

export default {
  props: {
    totalPages: {
      type: Number,
      default: 0
    },

		currentPage: {
      type: [Number, null],
      default: null
		}
  },

  emits: ['page-change'],

  data() {
    return {
      innerCurrentPage: 1
    }
  },

	mounted() {
		if (this.currentPage) {
			this.innerCurrentPage = +this.currentPage;
		}
	},

  methods: {
    changePage(page) {
      this.innerCurrentPage = page;
      this.$emit('page-change', this.innerCurrentPage)
    },
  },
};
</script>

<style scoped lang="scss">
.pagination {
	display: flex;
	justify-content: space-between;
	align-items: center;
	margin-top: 20px;
	
	&__page {
		border: 1px solid var(--color-indigo);
		padding: 5px;
		border-radius: 6px;
		background-color: #ffffff;
		color: var(--color-indigo);
		margin: 0 5px;
		cursor: pointer;

		&_active,
		&:hover {
			font-weight: bold;
			background-color: var(--color-indigo);
			color: #ffffff;
		}

		&:disabled {
			border-color: #7b7684;
			background: #FFFFFF;
			color: #7b7684;
		}
	}
}
</style>