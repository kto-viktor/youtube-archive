<template>
  <span
    class="loader"
    :style="styles"
  />
</template>

<script>
export default {
	props: {
		size: {
			type: String,
			default: '48px',
		},

		borderWidth: {
			type: String,
			default: '5px',
		}
	},

	computed: {
		styles() {
			return `width: ${this.size}; height: ${this.size}; border-width: ${this.borderWidth}`;
		}
	}
}
</script>

<style>
.loader {
	border-color: #6610F2;
	border-style: solid;
	border-bottom-color: transparent;
	border-radius: 50%;
	display: inline-block;
	box-sizing: border-box;
	animation: rotation 1s linear infinite;
}

@keyframes rotation {
  0% {
    transform: rotate(0deg)
  }
  100% {
    transform: rotate(360deg)
  }
}
</style>