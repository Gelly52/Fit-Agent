import { createApp } from "vue";
import App from "./App.vue";
import "./styles/base.css";
import "./styles/spring-ai-vite-layout.css";
import "./styles/console-theme.css";
import "./config/runtime";
import "./services/http";
import "./services/doctorApi";
import "./services/sseService";
import installElementPlus from "./plugins/element-ui";

const app = createApp(App);

installElementPlus(app);

app.mount("#app");
