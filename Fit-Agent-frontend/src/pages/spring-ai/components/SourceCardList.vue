<template>
  <div
    class="message-sources"
    v-if="normalizedSources.length"
    role="list"
    aria-label="消息来源列表"
  >
    <div
      class="source-card"
      v-for="(source, index) in normalizedSources"
      :key="source.id || index"
      role="listitem"
    >
      <div class="source-card-header">
        <span class="source-card-index">来源 {{ index + 1 }}</span>
        <a
          v-if="source.url"
          class="source-card-link"
          :href="source.url"
          target="_blank"
          rel="noopener noreferrer"
        >
          打开
        </a>
      </div>
      <p class="source-card-title">{{ source.title }}</p>
      <p class="source-card-snippet" v-if="source.snippet">{{ source.snippet }}</p>
      <p class="source-card-extra" v-if="source.extra">{{ source.extra }}</p>
    </div>
  </div>
</template>

<script>
import { normalizeSources } from '../utils/sourceNormalizer';

export default {
  name: 'SourceCardList',
  props: {
    sources: {
      type: [Array, Object, String],
      default: function () {
        return [];
      },
    },
  },
  computed: {
    normalizedSources() {
      return normalizeSources(this.sources);
    },
  },
};
</script>
