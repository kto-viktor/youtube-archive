<template>
  <div class="pages">
    <button
      v-if="totalPages > 1"
      class="page-item"
      :disabled="currentPage === 1"
      @click="changePage(-1)"
    >
      Prev
    </button>
    <div>
      <span
        v-for="page in visiblePages"
        :key="page"
        class="page-item"
        :class="{ 'page-item_active': page === currentPage }"
        @click="changePage(page)"
      >
        {{ page }}
      </span>
    </div>
    <button
      v-if="totalPages > 1"
      class="page-item"
      :disabled="currentPage === totalPages"
      @click="changePage(1)"
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
      required: false,
      default: 0
    }
  },
  emits: ['page-change'],

  data() {
    return {
      currentPage: 1
    }
  },

  computed: {
    visiblePages() {
      if (this.totalPages <= 0) {
        return [];
      }

      const visiblePageNumbers = [];

      if (this.totalPages <= 6) {
        visiblePageNumbers.push(...Array.from({length: this.totalPages}, (_, i) => i + 1));
      } else {
        visiblePageNumbers.push(1, 2);

        if (this.currentPage - 1 > 2) {
          visiblePageNumbers.push('...');
        }

        for (let i = Math.max(this.currentPage - 1, 3); i <= Math.min(this.currentPage + 1, this.totalPages - 2); i++) {
          visiblePageNumbers.push(i);
        }

        if (this.currentPage + 1 < this.totalPages - 1) {
          visiblePageNumbers.push('...');
        }

        visiblePageNumbers.push(this.totalPages - 1, this.totalPages);
      }

      return visiblePageNumbers;
    },
  },
  methods: {
    changePage(page) {
      if (page === -1) {
        this.currentPage -= 1;
      } else if (page === 1) {
        this.currentPage += 1;
      } else {
        this.currentPage = page;
      }
      this.$emit('page-change', this.currentPage)
    },
  },
};
</script>

<style lang="scss">
@import '@/assets/main.scss';
</style>