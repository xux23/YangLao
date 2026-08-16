<template>
  <el-card shadow="hover">
    <template #header>
      <span>操作日志</span>
    </template>

    <!-- 查询区 -->
    <el-form inline>
      <el-form-item label="操作人">
        <el-input v-model="query.username" placeholder="用户名" clearable style="width: 150px" />
      </el-form-item>
      <el-form-item label="开始时间">
        <el-date-picker
          v-model="query.startTime"
          type="datetime"
          placeholder="开始时间"
          value-format="YYYY-MM-DD HH:mm:ss"
          style="width: 190px"
        />
      </el-form-item>
      <el-form-item label="结束时间">
        <el-date-picker
          v-model="query.endTime"
          type="datetime"
          placeholder="结束时间"
          value-format="YYYY-MM-DD HH:mm:ss"
          style="width: 190px"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="loadData">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 日志表格 -->
    <el-table :data="tableData" border stripe v-loading="loading">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="username" label="操作人" width="110" />
      <el-table-column prop="operation" label="操作描述" width="140" />
      <el-table-column prop="method" label="请求接口" min-width="200" show-overflow-tooltip />
      <el-table-column prop="params" label="请求参数" min-width="180" show-overflow-tooltip />
      <el-table-column prop="ip" label="IP 地址" width="130" />
      <el-table-column prop="createTime" label="操作时间" width="170" />
    </el-table>

    <!-- 分页 -->
    <el-pagination
      v-model:current-page="query.page"
      v-model:page-size="query.size"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next, jumper"
      style="margin-top: 16px; justify-content: flex-end"
      @size-change="loadData"
      @current-change="loadData"
    />
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { getLogs } from '../../api/log'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)

const query = reactive({
  page: 1,
  size: 10,
  username: '',
  startTime: '',
  endTime: ''
})

async function loadData() {
  loading.value = true
  try {
    const res = await getLogs(query)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function handleReset() {
  query.username = ''
  query.startTime = ''
  query.endTime = ''
  loadData()
}

onMounted(loadData)
</script>