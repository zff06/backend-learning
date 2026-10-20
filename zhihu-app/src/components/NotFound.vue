<template>
  <div class="not-found">
    <p class="code">404</p>
    <p class="text">页面不存在，可能地址输错了</p>
    <router-link class="back" :to="{ name: 'specialList' }">回到专题列表</router-link>
  </div>
</template>

<style scoped>
.not-found {
  width: 70%;
  margin: 80px auto;
  text-align: center;
}

.code {
  font-size: 64px;
  font-weight: 600;
  color: #d9d9d9;
  margin: 0;
}

.text {
  color: #666;
  margin: 8px 0 24px;
}

.back {
  color: #3670ee;
  text-decoration: none;
  font-size: 14px;
}
</style>
