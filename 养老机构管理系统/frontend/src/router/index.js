import { createRouter, createWebHistory } from 'vue-router'

/**
 * 路由配置：meta.roles 声明可访问角色，路由守卫做登录与角色校验
 */
const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/login/Login.vue')
  },
  {
    path: '/',
    component: () => import('../views/layout/Layout.vue'),
    redirect: () => {
      // 根据角色跳转到默认首页
      const role = JSON.parse(localStorage.getItem('user') || '{}').role
      if (role === 'admin') return '/dashboard'
      if (role === 'family') return '/health-record'
      return '/elders'
    },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/dashboard/Dashboard.vue'),
        meta: { title: '首页看板', roles: ['admin'] }
      },
      {
        path: 'sys/users',
        name: 'UserManage',
        component: () => import('../views/sys/UserManage.vue'),
        meta: { title: '用户管理', roles: ['admin'] }
      },
      {
        path: 'sys/logs',
        name: 'LogManage',
        component: () => import('../views/sys/LogManage.vue'),
        meta: { title: '操作日志', roles: ['admin'] }
      },
      {
        path: 'elders',
        name: 'ElderList',
        component: () => import('../views/elder/ElderList.vue'),
        meta: { title: '老人档案', roles: ['admin', 'nurse'] }
      },
      {
        path: 'care-record',
        name: 'CareRecordList',
        component: () => import('../views/care/CareRecordList.vue'),
        meta: { title: '护理记录', roles: ['admin', 'nurse', 'family'] }
      },
      {
        path: 'health-record',
        name: 'HealthRecordList',
        component: () => import('../views/health/HealthRecordList.vue'),
        meta: { title: '体征记录', roles: ['admin', 'nurse', 'family'] }
      },
      {
        path: 'medicine-plan',
        name: 'MedicinePlan',
        component: () => import('../views/health/MedicinePlan.vue'),
        meta: { title: '用药计划', roles: ['admin', 'nurse'] }
      },
      {
        path: 'medicine-task',
        name: 'MedicineTask',
        component: () => import('../views/health/MedicineTask.vue'),
        meta: { title: '用药任务', roles: ['admin', 'nurse'] }
      },
      {
        path: 'visit',
        name: 'VisitList',
        component: () => import('../views/visit/VisitList.vue'),
        meta: { title: '探访预约', roles: ['family'] }
      },
      {
        path: 'visit-audit',
        name: 'VisitAudit',
        component: () => import('../views/visit/VisitAudit.vue'),
        meta: { title: '探访审核', roles: ['admin', 'nurse'] }
      },
      {
        path: 'message',
        name: 'MessageList',
        component: () => import('../views/message/MessageList.vue'),
        meta: { title: '留言反馈', roles: ['admin', 'nurse', 'family'] }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫：未登录跳登录页，角色不匹配跳回首页
router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  if (!token && to.path !== '/login') {
    return '/login'
  }
  if (token && to.path === '/login') {
    return '/'
  }
  const roles = to.meta.roles
  if (roles) {
    const role = JSON.parse(localStorage.getItem('user') || '{}').role
    if (!roles.includes(role)) {
      return '/'
    }
  }
})

export default router