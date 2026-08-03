import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/Login.vue'),
    meta: { public: true }
  },
  {
    path: '/',
    component: () => import('@/layout/AdminLayout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/dashboard/Index.vue'), meta: { title: '数据看板' } },
      { path: 'order', name: 'Order', component: () => import('@/views/order/Index.vue'), meta: { title: '订单管理' } },
      { path: 'after-sale', name: 'AfterSale', component: () => import('@/views/afterSale/Index.vue'), meta: { title: '售后管理' } },
      { path: 'category', name: 'Category', component: () => import('@/views/category/Index.vue'), meta: { title: '分类管理' } },
      { path: 'goods', name: 'Goods', component: () => import('@/views/goods/Index.vue'), meta: { title: '商品管理' } },
      { path: 'stock-alert', name: 'StockAlert', component: () => import('@/views/stock/Index.vue'), meta: { title: '补货提醒' } },
      { path: 'group', name: 'Group', component: () => import('@/views/group/Index.vue'), meta: { title: '团购活动' } },
      { path: 'seckill', name: 'Seckill', component: () => import('@/views/seckill/Index.vue'), meta: { title: '秒杀活动' } },
      { path: 'comment', name: 'Comment', component: () => import('@/views/comment/Index.vue'), meta: { title: '评价管理' } },
      { path: 'coupon', name: 'Coupon', component: () => import('@/views/marketing/Coupon.vue'), meta: { title: '优惠券模板' } },
      { path: 'fullreduce', name: 'FullReduce', component: () => import('@/views/marketing/FullReduce.vue'), meta: { title: '满减活动' } },
      { path: 'integral-coupon', name: 'IntegralCoupon', component: () => import('@/views/marketing/IntegralCoupon.vue'), meta: { title: '积分兑券' } },
      { path: 'seckill-coupon', name: 'SeckillCoupon', component: () => import('@/views/marketing/SeckillCoupon.vue'), meta: { title: '整点抢券' } },
      { path: 'lottery', name: 'Lottery', component: () => import('@/views/marketing/Lottery.vue'), meta: { title: '抽奖奖品' } },
      { path: 'coupon-log', name: 'CouponLog', component: () => import('@/views/marketing/CouponLog.vue'), meta: { title: '用券记录' } },
      { path: 'ai/knowledge', name: 'Knowledge', component: () => import('@/views/ai/Knowledge.vue'), meta: { title: 'AI 知识库' } },
      { path: 'ai/chat-log', name: 'ChatLog', component: () => import('@/views/ai/ChatLog.vue'), meta: { title: '对话日志' } },
      { path: 'ai/image-log', name: 'ImageLog', component: () => import('@/views/ai/ImageLog.vue'), meta: { title: '识图日志' } },
      { path: 'ai/group-text-log', name: 'GroupTextLog', component: () => import('@/views/ai/GroupTextLog.vue'), meta: { title: '文案日志' } },
      { path: 'message/template', name: 'MsgTemplate', component: () => import('@/views/message/Template.vue'), meta: { title: '消息模板' } }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  if (!to.meta.public && !userStore.token) {
    next('/login')
  } else if (to.path === '/login' && userStore.token) {
    next('/')
  } else {
    next()
  }
})

export default router
