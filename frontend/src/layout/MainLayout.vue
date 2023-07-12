<template>
  <div class="container">
    <header class="header">
      <router-link
        to="/video"
        active-class="active"
        class="header__link"
      >
        Видео
      </router-link>
	
      <router-link
        to="/playlist"
        active-class="active"
        class="header__link"
      >
        Плейлисты
      </router-link>
    </header>
	
    <main>
      <router-view />
    </main>
	
    <footer class="footer">
      <p>
        by <a
          href="https://true-programmers.com"
          target="_blank"
        >true-programmers.com</a>
      </p>
      <a
        href="https://github.com/kto-viktor/youtube-archive"
        target="_blank"
      >Github</a>
    </footer>
  </div>
</template>