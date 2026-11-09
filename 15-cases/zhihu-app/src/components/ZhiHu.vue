<template>
  <div class="container">
    <div v-for="(item, index) in list" :key="index" class="card">
      <div class="left">
        <img :src="item.banner" alt="" />
      </div>
      <div class="right">
        <h3>{{ item.title }}</h3>
        <p>
          <span class="updated">{{ item.updated }}</span>
          <span>{{ item.viewCount }}次浏览</span>
        </p>
      </div>
      <span v-if="item.isFollowing === 'TRUE'" class="follow followed">
        已关注
      </span>
      <span v-else class="follow unfollow"> 关注 </span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import axios from 'axios'

interface Special {
  id: string
  title: string
  banner: string
  description: string
  isFollowing: string
  followersCount: number
  viewCount: number
  updated: string
}

const list = ref<Special[]>([])

// 教程原代码写的是 (): void =>，但表达式体会把 Promise 返回出去，vue-tsc 报 TS2322，去掉标注即可
const fetchAll = () =>
  axios.get(`http://localhost:8080/api/v1/special/all`)
    .then((res) => {
      list.value = res.data.data as Special[]
      console.log(list.value)
    })
    .catch((err) => {
      console.log(err)
    })

fetchAll()
</script>

<style scoped>
.container {
  width: 70%;
  margin: 0 auto;
}

.card {
  display: flex;
  height: 160px;
  margin-bottom: 15px;
  border: 1px solid #ccc;
  box-shadow: 10px 5px 5px #ddd;
  border-radius: 8px;
  position: relative;
}
.left {
  flex: 1;
}

.right {
  flex: 4;
  padding: 10px;
}

.left img {
  width: 100%;
  height: 100%;
  border-top-left-radius: 8px;
  border-bottom-left-radius: 8px;
}

p span {
  font-size: 13px;
  color: #333;
}

.updated {
  margin-right: 20px;
}

.follow {
  position: absolute;
  right: 20px;
  top: 20px;
  padding: 5px 10px;
  font-size: 14px;
}

.followed {
  background-color: #f6f6f6;
  color: #716c6c;
}
.unfollow {
  background-color: #eff4fe;
  color: #3670ee;
}
</style>
