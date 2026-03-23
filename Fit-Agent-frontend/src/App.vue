<template>
  <LoginPage v-if="!hasSession" @login-success="handleLoginSuccess" />
  <SpringAiPage
    v-else
    :key="chatPageKey"
    @logout-success="handleLogoutSuccess"
  />
</template>

<script>
import LoginPage from "./pages/login/LoginPage.vue";
import SpringAiPage from "./pages/spring-ai/SpringAiPage.vue";
import { getToken, getUserInfo } from "./services/http";

const THEME_STORAGE_KEY = "geogeo-theme";

export default {
  name: "SpringAiViteApp",
  components: {
    LoginPage,
    SpringAiPage,
  },
  data() {
    return {
      hasSession: false,
      chatPageKey: 0,
    };
  },
  created() {
    this.syncInitialTheme();
    this.syncSessionState();
  },
  methods: {
    syncInitialTheme() {
      if (typeof window === "undefined" || typeof document === "undefined") {
        return;
      }

      var savedTheme = window.localStorage.getItem(THEME_STORAGE_KEY);
      var theme =
        savedTheme === "light" ||
        savedTheme === "dark" ||
        savedTheme === "system"
          ? savedTheme
          : "system";
      var root = document.documentElement;

      root.removeAttribute("data-theme");

      if (theme === "light" || theme === "dark") {
        root.setAttribute("data-theme", theme);
        return;
      }

      var prefersDark =
        window.matchMedia &&
        window.matchMedia("(prefers-color-scheme: dark)").matches;
      root.setAttribute("data-theme", prefersDark ? "dark" : "light");
    },
    syncSessionState() {
      this.hasSession = !!(getToken() && getUserInfo());
    },
    handleLoginSuccess() {
      this.syncSessionState();
      this.chatPageKey += 1;
    },
    handleLogoutSuccess() {
      this.syncSessionState();
      this.chatPageKey += 1;
    },
  },
};
</script>
