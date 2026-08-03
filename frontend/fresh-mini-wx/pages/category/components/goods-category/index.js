Component({
  externalClasses: ['custom-class'],

  properties: {
    category: {
      type: Array,
      value: [],
      observer() {
        this.buildSideList();
      },
    },
    initActive: {
      type: Array,
      value: [],
    },
    isSlotRight: {
      type: Boolean,
      value: false,
    },
    level: {
      type: Number,
      value: 2,
      observer() {
        this.buildSideList();
      },
    },
  },

  data: {
    activeKey: 0,
    subActiveKey: 0,
    sideList: [],
    activeSideIndex: 0,
  },

  attached() {
    this.buildSideList();
    if (this.properties.initActive && this.properties.initActive.length > 0) {
      this.setData({
        activeKey: this.properties.initActive[0],
        subActiveKey: this.properties.initActive[1] || 0,
      });
    }
  },

  methods: {
    /**
     * 左侧：父级 + 其子级（缩进挂在父级下面）
     */
    buildSideList() {
      const category = this.properties.category || [];
      const sideList = [];
      category.forEach((parent, parentIndex) => {
        sideList.push({
          type: 'parent',
          parentIndex,
          childIndex: -1,
          name: parent.name || '',
          disabled: !!parent.disabled,
        });
        (parent.children || []).forEach((child, childIndex) => {
          sideList.push({
            type: 'child',
            parentIndex,
            childIndex,
            name: child.name || '',
            disabled: !!child.disabled,
          });
        });
      });

      const { activeKey, subActiveKey } = this.data;
      let activeSideIndex = sideList.findIndex((s) => s.parentIndex === activeKey && s.type === 'parent');
      if (subActiveKey >= 0 && sideList.some((s) => s.parentIndex === activeKey && s.type === 'child')) {
        const childIdx = sideList.findIndex(
          (s) => s.parentIndex === activeKey && s.type === 'child' && s.childIndex === subActiveKey,
        );
        // 仅当当前就是点选子级时保持；初始化默认高亮父级
        if (this._lastSideType === 'child' && childIdx >= 0) {
          activeSideIndex = childIdx;
        }
      }
      if (activeSideIndex < 0) activeSideIndex = 0;

      this.setData({ sideList, activeSideIndex });
    },

    onSideTap(event) {
      const { index } = event.currentTarget.dataset;
      const item = this.data.sideList[index];
      if (!item || item.disabled) return;

      this._lastSideType = item.type;
      const subKey = item.type === 'parent' ? 0 : item.childIndex;

      this.setData(
        {
          activeKey: item.parentIndex,
          subActiveKey: subKey,
          activeSideIndex: index,
        },
        () => {
          this.triggerEvent('change', [this.data.activeKey, this.data.subActiveKey]);
        },
      );
    },

    changCategory(event) {
      const { item } = event.currentTarget.dataset;
      this.triggerEvent('changeCategory', { item });
    },
  },
});
