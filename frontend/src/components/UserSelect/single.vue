<template><el-select v-model="value" placeholder="请选择用户" clearable><el-option v-for="u in users" :key="u.userId" :label="u.userName" :value="u.userId"/></el-select></template>
<script setup lang="ts">
import { ref } from 'vue'
const value = ref(null)
const users = ref([])
</script>
