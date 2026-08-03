import TabMenu from './data';
import { fetchCartGroupData } from '../services/cart/cart';
import { config } from '../config/index';

Component({
  data: {
    active: 0,
    list: TabMenu,
    cartNum: 0,
    cartBump: false,
  },

  methods: {
    onChange(event) {
      const index = Number(event.currentTarget.dataset.index);
      if (Number.isNaN(index) || index < 0) return;
      this.setData({ active: index });
      const item = this.data.list[index];
      if (!item) return;
      wx.switchTab({
        url: item.url.startsWith('/') ? item.url : `/${item.url}`,
      });
    },

    init() {
      const page = getCurrentPages().pop();
      const route = page ? page.route.split('?')[0] : '';
      const active = this.data.list.findIndex(
        (item) => (item.url.startsWith('/') ? item.url.substr(1) : item.url) === `${route}`,
      );
      this.setData({ active: active >= 0 ? active : 0 });
      this.refreshCartNum();
    },

    async refreshCartNum() {
      if (config.useMock) return;
      try {
        const res = await fetchCartGroupData();
        const stores = (res && res.data && res.data.storeGoods) || [];
        let count = 0;
        stores.forEach((store) => {
          (store.promotionGoodsList || []).forEach((act) => {
            (act.goodsPromotionList || []).forEach((g) => {
              count += Number(g.quantity) || 0;
            });
          });
        });
        this.setData({ cartNum: count });
      } catch (e) {
        /* 未登录等忽略 */
      }
    },

    bumpCart(delta = 1) {
      const cartNum = Math.max(0, (this.data.cartNum || 0) + Number(delta || 0));
      this.setData({ cartNum, cartBump: true });
      clearTimeout(this._bumpTimer);
      this._bumpTimer = setTimeout(() => {
        this.setData({ cartBump: false });
      }, 480);
    },
  },
});
