import ElementPlus, { ElMessage } from "element-plus";

export default function installElementPlus(app) {
  app.use(ElementPlus);
  app.config.globalProperties.$message = ElMessage;
}

export { ElementPlus, ElMessage };
