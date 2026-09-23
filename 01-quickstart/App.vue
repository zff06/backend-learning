<script setup>
import { ref } from 'vue'

// 用户信息（模拟当前登录用户）
const user = ref({
  name: '林小满',
  avatar: 'https://api.dicebear.com/9.x/adventurer/svg?seed=linxiaoman',
  role: 'organizer', // organizer / auditor / student
  skills: ['活动策划', '海报设计', 'Vue 基础', '沟通协调'],
})

// 在岗 / 请假状态
const onDuty = ref(true)

// 主题色，卡片上通过 CSS 变量传下去，默认清新蓝
const themes = [
  { name: '活力橙', color: '#e8663c' },
  { name: '清新蓝', color: '#3b82c4' },
  { name: '草木绿', color: '#4a9d6e' },
]
const activeTheme = ref(1)

// 角色对应的文字和类名
const roleMap = {
  organizer: { text: '活动组织者' },
  auditor: { text: '审核员' },
  student: { text: '学生' },
}

function toggleDuty() {
  onDuty.value = !onDuty.value
}

// 编辑资料
const editing = ref(false)
const newSkill = ref('')

function addSkill() {
  const s = newSkill.value.trim()
  if (s && !user.value.skills.includes(s)) {
    user.value.skills.push(s)
  }
  newSkill.value = ''
}

function removeSkill(index) {
  user.value.skills.splice(index, 1)
}
</script>

<template>
  <div
    class="card"
    :style="{ '--theme': themes[activeTheme].color }"
  >
    <h2 class="title">校园活动服务平台 · 个人名片</h2>

    <!-- 主题色切换 -->
    <div class="theme-bar">
      <button
        v-for="(t, i) in themes"
        :key="t.name"
        class="theme-btn"
        :class="{ picked: i === activeTheme }"
        :style="{ color: t.color }"
        @click="activeTheme = i"
      >
        <span class="dot" :style="{ background: t.color }"></span>
        {{ t.name }}
      </button>
    </div>

    <div class="main">
      <img class="avatar" :src="user.avatar" alt="头像" />

      <div class="info">
        <div class="name-row">
          <span class="name">{{ user.name }}</span>
          <span
            class="role-tag"
            :class="{
              'is-organizer': user.role === 'organizer',
              'is-auditor': user.role === 'auditor',
              'is-student': user.role === 'student',
            }"
          >
            {{ roleMap[user.role].text }}
          </span>
          <button class="edit-btn" @click="editing = !editing">
            {{ editing ? '完成' : '编辑资料' }}
          </button>
        </div>

        <p class="duty">
          当前状态：
          <span class="duty-tag" :class="onDuty ? 'duty-on' : 'duty-off'">
            {{ onDuty ? '在岗' : '请假' }}
          </span>
          <button class="duty-btn" @click="toggleDuty">
            切换为{{ onDuty ? '请假' : '在岗' }}
          </button>
        </p>

        <!-- 技能标签 -->
        <div class="skills">
          <span class="skill-title">技能标签</span>
          <ul>
            <li v-for="(s, i) in user.skills" :key="s">
              {{ s }}
              <button
                v-if="editing"
                class="skill-del"
                @click="removeSkill(i)"
              >×</button>
            </li>
          </ul>
          <div v-if="editing" class="add-skill">
            <input
              v-model="newSkill"
              placeholder="输入新技能"
              @keyup.enter="addSkill"
            />
            <button @click="addSkill">添加</button>
          </div>
        </div>

        <!-- 编辑表单 -->
        <div v-if="editing" class="edit-form">
          <label>
            姓名
            <input v-model="user.name" placeholder="请输入姓名" />
          </label>
          <label>
            头像地址
            <input v-model="user.avatar" placeholder="请输入图片链接" />
          </label>
          <div class="role-pick">
            角色
            <label v-for="(r, key) in roleMap" :key="key">
              <input type="radio" :value="key" v-model="user.role" />
              {{ r.text }}
            </label>
          </div>
          <div class="avatar-preview">
            <img :src="user.avatar" alt="头像预览" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.card {
  width: 420px;
  margin: 60px auto;
  padding: 28px 32px;
  border-radius: 14px;
  background: #fff;
  border-top: 5px solid var(--theme);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.title {
  font-size: 15px;
  color: #888;
  font-weight: normal;
  margin: 0 0 16px;
}

/* 主题色按钮 */
.theme-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 24px;
}

.theme-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  border: 1px solid #e8e8e8;
  background: #fff;
  border-radius: 18px;
  padding: 5px 14px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.theme-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 3px 8px rgba(0, 0, 0, 0.1);
}

.theme-btn .dot {
  width: 14px;
  height: 14px;
  border-radius: 50%;
}

.theme-btn.picked {
  border-color: currentColor;
  background: color-mix(in srgb, currentColor 10%, white);
  font-weight: 500;
}

.main {
  display: flex;
  gap: 20px;
}

.avatar {
  width: 84px;
  height: 84px;
  border-radius: 50%;
  border: 3px solid var(--theme);
  flex-shrink: 0;
}

.info {
  flex: 1;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.name {
  font-size: 20px;
  font-weight: bold;
}

/* 角色标签颜色 */
.role-tag {
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 4px;
  color: #fff;
}

.role-tag.is-organizer {
  background: #e8663c;
}

.role-tag.is-auditor {
  background: #8e6cc4;
}

.role-tag.is-student {
  background: #3b82c4;
}

/* 在岗 / 请假 */
.duty {
  margin: 14px 0;
  font-size: 14px;
  color: #555;
  display: flex;
  align-items: center;
  gap: 8px;
}

.duty-tag {
  padding: 2px 10px;
  border-radius: 4px;
  font-size: 12px;
  color: #fff;
}

.duty-tag.duty-on {
  background: #4a9d6e;
}

.duty-tag.duty-off {
  background: #c47f3b;
}

.duty-btn {
  border: 1px solid #ccc;
  background: #fafafa;
  border-radius: 4px;
  padding: 3px 10px;
  font-size: 12px;
  cursor: pointer;
}

.duty-btn:hover {
  border-color: var(--theme);
  color: var(--theme);
}

/* 技能标签 */
.skills {
  margin-top: 10px;
}

.skill-title {
  font-size: 13px;
  color: #999;
}

.skills ul {
  list-style: none;
  padding: 0;
  margin: 8px 0 0;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.skills li {
  font-size: 12px;
  padding: 4px 12px;
  border-radius: 14px;
  background: color-mix(in srgb, var(--theme) 12%, white);
  color: var(--theme);
  border: 1px solid color-mix(in srgb, var(--theme) 40%, white);
  display: flex;
  align-items: center;
  gap: 4px;
}

.skill-del {
  border: none;
  background: none;
  color: inherit;
  font-size: 13px;
  line-height: 1;
  cursor: pointer;
  padding: 0;
}

.add-skill {
  display: flex;
  gap: 6px;
  margin-top: 10px;
}

.add-skill input {
  flex: 1;
  min-width: 0;
  border: 1px solid #ddd;
  border-radius: 4px;
  padding: 4px 10px;
  font-size: 12px;
  outline: none;
}

.add-skill input:focus {
  border-color: var(--theme);
}

.add-skill button {
  border: 1px solid var(--theme);
  background: none;
  color: var(--theme);
  border-radius: 4px;
  padding: 4px 12px;
  font-size: 12px;
  cursor: pointer;
}

/* 编辑表单 */
.edit-form {
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px dashed #ddd;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.edit-form label {
  font-size: 12px;
  color: #888;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.edit-form input {
  border: 1px solid #ddd;
  border-radius: 4px;
  padding: 6px 10px;
  font-size: 13px;
  outline: none;
}

.edit-form input:focus {
  border-color: var(--theme);
}

/* 角色单选 */
.role-pick {
  font-size: 12px;
  color: #888;
  display: flex;
  align-items: center;
  gap: 14px;
}

.role-pick label {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 4px;
  cursor: pointer;
}

.avatar-preview img {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  border: 2px solid var(--theme);
}

.edit-btn {
  margin-left: auto;
  border: 1px solid #ccc;
  background: #fafafa;
  border-radius: 4px;
  padding: 3px 10px;
  font-size: 12px;
  cursor: pointer;
}

.edit-btn:hover {
  border-color: var(--theme);
  color: var(--theme);
}
</style>
