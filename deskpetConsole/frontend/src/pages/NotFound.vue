<template>
  <div class="not-found">
    <div class="content">
      <h1 class="code">404</h1>
      <h2 class="title">页面未找到</h2>
      <p class="description">抱歉，您访问的页面不存在或已被移除。</p>
      <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
    </div>
  </div>
</template>

<style scoped>
.not-found {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--bg-secondary, #f5f7fa);
}

.content {
  text-align: center;
}

.code {
  font-size: 72px;
  font-weight: bold;
  color: var(--text-primary, #303133);
  margin: 0;
  line-height: 1.2;
}

.title {
  font-size: 24px;
  color: var(--text-regular, #606266);
  margin: 16px 0;
}

.description {
  color: var(--text-secondary, #909399);
  margin-bottom: 32px;
}
</style>
