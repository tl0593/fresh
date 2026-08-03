package com.fresh.goods.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fresh.common.constant.OrderConstant;
import com.fresh.common.base.PageVO;
import com.fresh.common.base.Result;
import com.fresh.common.exception.BusinessException;
import com.fresh.common.exception.ErrorCodeEnum;
import com.fresh.common.util.ContextUtil;
import com.fresh.common.util.JsonUtils;
import com.fresh.common.util.RedisUtils;
import com.fresh.goods.config.GoodsProperties;
import com.fresh.goods.constant.GoodsRedisKeyConstant;
import com.fresh.goods.dto.GoodsSaveWithSpecsDTO;
import com.fresh.goods.dto.GroupTextRequestDTO;
import com.fresh.goods.dto.GoodsPriceQueryDTO;
import com.fresh.goods.dto.StockChangeDTO;
import com.fresh.goods.dto.StockRestockDTO;
import com.fresh.goods.entity.*;
import com.fresh.goods.feign.AiFeignClient;
import com.fresh.goods.mapper.primary.*;
import com.fresh.goods.vo.CategoryTreeVO;
import com.fresh.goods.vo.CommentRateVO;
import com.fresh.goods.vo.GoodsAdminDetailVO;
import com.fresh.goods.vo.GoodsDetailVO;
import com.fresh.goods.vo.GoodsPriceVO;
import com.fresh.goods.vo.StockAlertVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GoodsService {

    private final GoodsMapper goodsMapper;
    private final GoodsCategoryMapper categoryMapper;
    private final GoodsSpecMapper specMapper;
    private final GoodsImageMapper imageMapper;
    private final GroupActivityMapper groupActivityMapper;
    private final SeckillActivityMapper seckillActivityMapper;
    private final RedisUtils redisUtils;
    private final GoodsProperties properties;
    private final GoodsRedisService goodsRedisService;
    private final GoodsMqProducer mqProducer;
    private final AiFeignClient aiFeignClient;
    private final CommentService commentService;

    public List<CategoryTreeVO> categoryTree() {
        String cache = redisUtils.get(GoodsRedisKeyConstant.GOODS_CATEGORY_TREE);
        if (cache != null) {
            return com.alibaba.fastjson2.JSON.parseArray(cache, CategoryTreeVO.class);
        }
        // 先按 sort 升序、id 升序取出，再用 LinkedHashMap 保持顺序
        List<GoodsCategory> all = categoryMapper.selectList(new LambdaQueryWrapper<GoodsCategory>()
                .eq(GoodsCategory::getDelFlag, 0)
                .eq(GoodsCategory::getStatus, 1)
                .orderByAsc(GoodsCategory::getSort)
                .orderByAsc(GoodsCategory::getId));
        Map<Long, CategoryTreeVO> map = new java.util.LinkedHashMap<>();
        for (GoodsCategory c : all) {
            CategoryTreeVO vo = new CategoryTreeVO();
            vo.setId(c.getId());
            vo.setParentId(c.getParentId());
            vo.setCatName(c.getCatName());
            vo.setIcon(c.getIcon());
            vo.setSort(c.getSort());
            map.put(c.getId(), vo);
        }
        List<CategoryTreeVO> roots = new ArrayList<>();
        for (CategoryTreeVO vo : map.values()) {
            if (vo.getParentId() == null || vo.getParentId() == 0) {
                roots.add(vo);
            } else {
                CategoryTreeVO parent = map.get(vo.getParentId());
                if (parent != null) {
                    parent.getChildren().add(vo);
                } else {
                    // 父节点缺失时降级为根，避免子分类丢失
                    roots.add(vo);
                }
            }
        }
        sortCategoryTree(roots);
        redisUtils.set(GoodsRedisKeyConstant.GOODS_CATEGORY_TREE, JsonUtils.toJson(roots));
        return roots;
    }

    /** 递归按 sort、id 排序，保证各级菜单顺序稳定 */
    private void sortCategoryTree(List<CategoryTreeVO> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }
        nodes.sort(Comparator
                .comparing((CategoryTreeVO n) -> n.getSort() == null ? Integer.MAX_VALUE : n.getSort())
                .thenComparing(n -> n.getId() == null ? 0L : n.getId()));
        for (CategoryTreeVO node : nodes) {
            sortCategoryTree(node.getChildren());
        }
    }

    public List<Goods> hotList() {
        String cache = redisUtils.get(GoodsRedisKeyConstant.GOODS_HOT_LIST);
        if (cache != null) {
            return com.alibaba.fastjson2.JSON.parseArray(cache, Goods.class);
        }
        List<Goods> list = goodsMapper.selectList(new LambdaQueryWrapper<Goods>()
                .eq(Goods::getStatus, 1)
                .eq(Goods::getDelFlag, 0)
                .orderByDesc(Goods::getSaleCount)
                .last("LIMIT 20"));
        redisUtils.set(GoodsRedisKeyConstant.GOODS_HOT_LIST, JsonUtils.toJson(list), 600, TimeUnit.SECONDS);
        return list;
    }

    /**
     * C 端分类选购商品分页
     * sortType: sale | price；sortOrder: asc | desc
     * catId 会包含其全部子孙分类
     */
    public PageVO<Goods> pageGoods(Long catId, String keyword, String sortType,
                                   String sortOrder, Integer pageNum, Integer pageSize) {
        int pn = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int ps = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 50);
        LambdaQueryWrapper<Goods> qw = new LambdaQueryWrapper<Goods>()
                .eq(Goods::getStatus, 1)
                .eq(Goods::getDelFlag, 0);
        if (catId != null && catId > 0) {
            List<Long> catIds = collectDescendantCatIds(catId);
            if (catIds.isEmpty()) {
                return PageVO.of(0L, List.of());
            }
            qw.in(Goods::getCatId, catIds);
        }
        if (StringUtils.hasText(keyword)) {
            qw.like(Goods::getGoodsName, keyword.trim());
        }
        boolean asc = "asc".equalsIgnoreCase(sortOrder);
        if ("price".equalsIgnoreCase(sortType)) {
            qw.orderBy(true, asc, Goods::getSalePrice).orderByDesc(Goods::getId);
        } else {
            // 默认按销量
            qw.orderBy(true, asc, Goods::getSaleCount).orderByDesc(Goods::getId);
        }
        Page<Goods> page = goodsMapper.selectPage(new Page<>(pn, ps), qw);
        return PageVO.of(page.getTotal(), page.getRecords());
    }

    /** 自身 + 子孙分类 id */
    private List<Long> collectDescendantCatIds(Long catId) {
        List<GoodsCategory> all = categoryMapper.selectList(new LambdaQueryWrapper<GoodsCategory>()
                .eq(GoodsCategory::getDelFlag, 0)
                .eq(GoodsCategory::getStatus, 1));
        Map<Long, List<Long>> childrenMap = new java.util.HashMap<>();
        for (GoodsCategory c : all) {
            Long pid = c.getParentId() == null ? 0L : c.getParentId();
            childrenMap.computeIfAbsent(pid, k -> new ArrayList<>()).add(c.getId());
        }
        Set<Long> result = new HashSet<>();
        java.util.ArrayDeque<Long> queue = new java.util.ArrayDeque<>();
        queue.add(catId);
        while (!queue.isEmpty()) {
            Long id = queue.poll();
            if (id == null || !result.add(id)) {
                continue;
            }
            List<Long> children = childrenMap.get(id);
            if (children != null) {
                queue.addAll(children);
            }
        }
        return new ArrayList<>(result);
    }

    public void refreshHotListCache() {
        redisUtils.delete(GoodsRedisKeyConstant.GOODS_HOT_LIST);
        hotList();
    }

    public GoodsDetailVO detail(Long goodsId) {
        String cacheKey = GoodsRedisKeyConstant.GOODS_DETAIL + goodsId;
        String cache = redisUtils.get(cacheKey);
        if (cache != null) {
            GoodsDetailVO cached = JsonUtils.fromJson(cache, GoodsDetailVO.class);
            // 旧缓存可能无规格，导致小程序无法加购，强制重建
            if (cached != null && cached.getSpecs() != null && !cached.getSpecs().isEmpty()) {
                // 评价数会随提交变化，缓存详情时单独刷新评分统计
                cached.setCommentRate(commentService.getCommentRate(goodsId));
                return cached;
            }
        }
        Goods goods = goodsMapper.selectById(goodsId);
        if (goods == null || goods.getDelFlag() == 1) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "商品不存在");
        }
        GoodsDetailVO vo = new GoodsDetailVO();
        BeanUtils.copyProperties(goods, vo);
        List<GoodsSpec> specs = specMapper.selectList(new LambdaQueryWrapper<GoodsSpec>()
                .eq(GoodsSpec::getGoodsId, goodsId)
                .eq(GoodsSpec::getDelFlag, 0));
        if (specs == null || specs.isEmpty()) {
            specs = ensureDefaultSpec(goods);
        }
        vo.setSpecs(specs);
        vo.setImages(imageMapper.selectList(new LambdaQueryWrapper<GoodsImage>()
                .eq(GoodsImage::getGoodsId, goodsId)
                .orderByAsc(GoodsImage::getSort)));
        vo.setCommentRate(commentService.getCommentRate(goodsId));
        redisUtils.set(cacheKey, JsonUtils.toJson(vo), properties.getGoodsCacheTtl(), TimeUnit.SECONDS);
        return vo;
    }

    /**
     * 后台录商品未配规格时，自动补一条标准规格。
     * 规格名表示份量/型号（如 500g），计量单位单独存 goods.unit（斤/份/盒）。
     */
    private List<GoodsSpec> ensureDefaultSpec(Goods goods) {
        GoodsSpec spec = new GoodsSpec();
        spec.setGoodsId(goods.getId());
        spec.setSpecName("标准");
        spec.setSpecPrice(goods.getSalePrice() != null ? goods.getSalePrice() : BigDecimal.ZERO);
        int stock = goods.getTotalStock() != null ? goods.getTotalStock() : 0;
        spec.setStock(Math.max(stock, 0));
        spec.setIsDefault(1);
        spec.setDelFlag(0);
        specMapper.insert(spec);
        List<GoodsSpec> list = new ArrayList<>();
        list.add(spec);
        return list;
    }

    private void normalizeGoodsUnit(Goods goods) {
        if (goods == null) {
            return;
        }
        if (!StringUtils.hasText(goods.getUnit())) {
            goods.setUnit("份");
            return;
        }
        goods.setUnit(goods.getUnit().trim());
    }

    /** 商品售价变更时，同步到默认规格；仅一个规格时也可直接改其库存 */
    private void syncDefaultSpecFromGoods(Goods goods) {
        List<GoodsSpec> specs = listSpecsByGoodsId(goods.getId());
        if (specs.isEmpty()) {
            ensureDefaultSpec(goods);
            return;
        }
        GoodsSpec target = specs.stream()
                .filter(s -> s.getIsDefault() != null && s.getIsDefault() == 1)
                .findFirst()
                .orElse(specs.get(0));
        boolean changed = false;
        if (goods.getSalePrice() != null
                && (target.getSpecPrice() == null || target.getSpecPrice().compareTo(goods.getSalePrice()) != 0)) {
            target.setSpecPrice(goods.getSalePrice());
            changed = true;
        }
        // 仅单规格时，商品表库存字段作为快捷改库存入口
        if (specs.size() == 1 && goods.getTotalStock() != null
                && !goods.getTotalStock().equals(target.getStock())) {
            target.setStock(Math.max(0, goods.getTotalStock()));
            changed = true;
        }
        if (changed) {
            specMapper.updateById(target);
        }
    }

    private void refreshGoodsTotalStock(Long goodsId) {
        if (goodsId == null) {
            return;
        }
        List<GoodsSpec> specs = listSpecsByGoodsId(goodsId);
        int sum = specs.stream().mapToInt(s -> s.getStock() == null ? 0 : Math.max(0, s.getStock())).sum();
        Goods goods = goodsMapper.selectById(goodsId);
        if (goods == null) {
            return;
        }
        goods.setTotalStock(sum);
        // 默认规格价回写商品售价，保持列表价一致
        specs.stream()
                .filter(s -> s.getIsDefault() != null && s.getIsDefault() == 1)
                .findFirst()
                .ifPresent(def -> {
                    if (def.getSpecPrice() != null) {
                        goods.setSalePrice(def.getSpecPrice());
                    }
                });
        goodsMapper.updateById(goods);
    }

    private void ensureSingleDefaultSpec(Long goodsId, Long preferSpecId) {
        List<GoodsSpec> specs = listSpecsByGoodsId(goodsId);
        if (specs.isEmpty()) {
            return;
        }
        Long keepId = preferSpecId;
        if (keepId == null) {
            keepId = specs.stream()
                    .filter(s -> s.getIsDefault() != null && s.getIsDefault() == 1)
                    .map(GoodsSpec::getId)
                    .findFirst()
                    .orElse(specs.get(0).getId());
        }
        for (GoodsSpec s : specs) {
            int want = s.getId().equals(keepId) ? 1 : 0;
            int now = s.getIsDefault() == null ? 0 : s.getIsDefault();
            if (now != want) {
                s.setIsDefault(want);
                specMapper.updateById(s);
            }
        }
    }

    public List<GroupActivity> groupList() {
        String cache = redisUtils.get(GoodsRedisKeyConstant.GROUP_HOT_LIST);
        List<GroupActivity> list;
        if (cache != null) {
            list = com.alibaba.fastjson2.JSON.parseArray(cache, GroupActivity.class);
        } else {
            list = groupActivityMapper.selectList(new LambdaQueryWrapper<GroupActivity>()
                    .eq(GroupActivity::getStatus, 1)
                    .eq(GroupActivity::getDelFlag, 0)
                    .le(GroupActivity::getStartTime, LocalDateTime.now())
                    .ge(GroupActivity::getEndTime, LocalDateTime.now()));
            redisUtils.set(GoodsRedisKeyConstant.GROUP_HOT_LIST, JsonUtils.toJson(list), 600, TimeUnit.SECONDS);
        }
        enrichGroupGoods(list);
        return list;
    }

    /** 回填团购关联的商品名称、主图、原价 */
    private void enrichGroupGoods(List<GroupActivity> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Set<Long> goodsIds = list.stream()
                .map(GroupActivity::getGoodsId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        if (goodsIds.isEmpty()) {
            return;
        }
        List<Goods> goodsList = goodsMapper.selectList(new LambdaQueryWrapper<Goods>()
                .in(Goods::getId, goodsIds)
                .eq(Goods::getDelFlag, 0));
        Map<Long, Goods> goodsMap = goodsList.stream()
                .collect(Collectors.toMap(Goods::getId, g -> g, (a, b) -> a));
        for (GroupActivity act : list) {
            Goods goods = goodsMap.get(act.getGoodsId());
            if (goods == null) {
                continue;
            }
            act.setGoodsName(goods.getGoodsName());
            act.setGoodsImg(goods.getGoodsImg());
            act.setSalePrice(goods.getSalePrice());
            act.setOriginPrice(goods.getOriginPrice() != null ? goods.getOriginPrice() : goods.getSalePrice());
        }
    }

    public List<SeckillActivity> seckillList() {
        List<SeckillActivity> list = seckillActivityMapper.selectList(new LambdaQueryWrapper<SeckillActivity>()
                .eq(SeckillActivity::getStatus, 1)
                .eq(SeckillActivity::getDelFlag, 0)
                .le(SeckillActivity::getStartTime, LocalDateTime.now())
                .ge(SeckillActivity::getEndTime, LocalDateTime.now()));
        enrichSeckillGoods(list);
        return list;
    }

    /** 回填秒杀关联的商品名称、主图、原价 */
    private void enrichSeckillGoods(List<SeckillActivity> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Set<Long> goodsIds = list.stream()
                .map(SeckillActivity::getGoodsId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        if (goodsIds.isEmpty()) {
            return;
        }
        List<Goods> goodsList = goodsMapper.selectList(new LambdaQueryWrapper<Goods>()
                .in(Goods::getId, goodsIds)
                .eq(Goods::getDelFlag, 0));
        Map<Long, Goods> goodsMap = goodsList.stream()
                .collect(Collectors.toMap(Goods::getId, g -> g, (a, b) -> a));
        for (SeckillActivity act : list) {
            Goods goods = goodsMap.get(act.getGoodsId());
            if (goods == null) {
                continue;
            }
            act.setGoodsName(goods.getGoodsName());
            act.setGoodsImg(goods.getGoodsImg());
            act.setSalePrice(goods.getSalePrice());
            act.setOriginPrice(goods.getOriginPrice() != null ? goods.getOriginPrice() : goods.getSalePrice());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void deductStock(StockChangeDTO dto) {
        if (dto.getActivityType() != null && dto.getActivityType() == OrderConstant.ACTIVITY_SECKILL) {
            deductSeckillStock(dto);
            return;
        }
        GoodsSpec spec = specMapper.selectById(dto.getSpecId());
        if (spec == null || spec.getStock() < dto.getNum()) {
            throw new BusinessException(ErrorCodeEnum.STOCK_NOT_ENOUGH);
        }
        spec.setStock(spec.getStock() - dto.getNum());
        specMapper.updateById(spec);
        syncGoodsStock(dto, false);
        mqProducer.sendStockChange(dto);
        clearGoodsCache(dto.getGoodsId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void restoreStock(StockChangeDTO dto) {
        GoodsSpec spec = specMapper.selectById(dto.getSpecId());
        if (spec != null) {
            spec.setStock(spec.getStock() + dto.getNum());
            specMapper.updateById(spec);
        }
        syncGoodsStock(dto, true);
        clearGoodsCache(dto.getGoodsId());
    }

    private void deductSeckillStock(StockChangeDTO dto) {
        String lockKey = GoodsRedisKeyConstant.LOCK_SECKILL + dto.getActivityId();
        if (!goodsRedisService.tryLock(lockKey, properties.getLockWaitTime(), properties.getLockHoldTime())) {
            throw new BusinessException(ErrorCodeEnum.INTERNAL_ERROR.getCode(), "秒杀锁获取失败");
        }
        try {
            String stockKey = GoodsRedisKeyConstant.GOODS_SECKILL_STOCK + dto.getActivityId();
            Integer stock = goodsRedisService.getStock(stockKey);
            if (stock == null) {
                SeckillActivity act = seckillActivityMapper.selectById(dto.getActivityId());
                stock = act == null ? 0 : act.getStock();
                goodsRedisService.setStock(stockKey, stock);
            }
            if (stock < dto.getNum()) {
                throw new BusinessException(ErrorCodeEnum.STOCK_NOT_ENOUGH);
            }
            goodsRedisService.decrementStock(stockKey);
            mqProducer.sendStockChange(dto);
        } finally {
            goodsRedisService.unlock(lockKey);
        }
    }

    private void syncGoodsStock(StockChangeDTO dto, boolean restore) {
        Goods goods = goodsMapper.selectById(dto.getGoodsId());
        if (goods == null) {
            return;
        }
        if (restore) {
            goods.setTotalStock(goods.getTotalStock() + dto.getNum());
            goods.setSaleCount(Math.max(0, goods.getSaleCount() - dto.getNum()));
        } else {
            goods.setTotalStock(Math.max(0, goods.getTotalStock() - dto.getNum()));
            goods.setSaleCount(goods.getSaleCount() + dto.getNum());
        }
        goodsMapper.updateById(goods);
    }

    public void handleGroupExpire(Map<String, Object> payload) {
        Object idObj = payload.get("groupActivityId");
        if (idObj == null) {
            idObj = payload.get("activityId");
        }
        if (idObj == null) {
            return;
        }
        Long activityId = Long.valueOf(idObj.toString());
        GroupActivity activity = groupActivityMapper.selectById(activityId);
        if (activity != null) {
            activity.setStatus(2);
            groupActivityMapper.updateById(activity);
        }
        redisUtils.delete(GoodsRedisKeyConstant.GROUP_HOT_LIST);
    }

    public void saveGoods(Goods goods) {
        normalizeGoodsUnit(goods);
        if (goods.getId() == null) {
            goods.setDelFlag(0);
            if (goods.getTotalStock() == null) {
                goods.setTotalStock(0);
            }
            goodsMapper.insert(goods);
            // 新建商品若未配规格，自动补默认规格，避免小程序无法加购
            List<GoodsSpec> specs = specMapper.selectList(new LambdaQueryWrapper<GoodsSpec>()
                    .eq(GoodsSpec::getGoodsId, goods.getId())
                    .eq(GoodsSpec::getDelFlag, 0));
            if (specs == null || specs.isEmpty()) {
                ensureDefaultSpec(goods);
            }
        } else {
            Goods exists = goodsMapper.selectById(goods.getId());
            if (exists == null || (exists.getDelFlag() != null && exists.getDelFlag() == 1)) {
                throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "商品不存在");
            }
            goodsMapper.updateById(goods);
            // 编辑商品时同步默认规格价格；库存以规格为准，回写总库存
            syncDefaultSpecFromGoods(goods);
            refreshGoodsTotalStock(goods.getId());
            clearGoodsCache(goods.getId());
        }
    }

    /** 管理端商品详情（含规格列表） */
    public GoodsAdminDetailVO adminGoodsDetail(Long goodsId) {
        if (goodsId == null) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "商品ID不能为空");
        }
        Goods goods = goodsMapper.selectById(goodsId);
        if (goods == null || (goods.getDelFlag() != null && goods.getDelFlag() == 1)) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "商品不存在");
        }
        List<GoodsSpec> specs = listSpecsByGoodsId(goodsId);
        if (specs.isEmpty()) {
            specs = ensureDefaultSpec(goods);
            refreshGoodsTotalStock(goodsId);
            goods = goodsMapper.selectById(goodsId);
            specs = listSpecsByGoodsId(goodsId);
        }
        GoodsAdminDetailVO vo = new GoodsAdminDetailVO();
        vo.setGoods(goods);
        vo.setSpecs(specs);
        return vo;
    }

    /**
     * 一次保存商品 + 规格（主流平台 SPU+SKU 编辑模型）。
     * 价格/库存以规格为准，商品售价取默认规格价，总库存为规格库存之和。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long saveGoodsWithSpecs(GoodsSaveWithSpecsDTO dto) {
        if (dto == null || dto.getGoods() == null) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "商品信息不能为空");
        }
        List<GoodsSpec> inputSpecs = dto.getSpecs() == null ? List.of() : dto.getSpecs();
        validateSpecsForSave(inputSpecs);

        Goods goods = dto.getGoods();
        normalizeGoodsUnit(goods);
        if (!StringUtils.hasText(goods.getGoodsName())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "商品名称不能为空");
        }
        if (goods.getCatId() == null) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "请选择分类");
        }

        GoodsSpec defaultSpec = inputSpecs.stream()
                .filter(s -> s.getIsDefault() != null && s.getIsDefault() == 1)
                .findFirst()
                .orElse(inputSpecs.get(0));
        goods.setSalePrice(defaultSpec.getSpecPrice() != null ? defaultSpec.getSpecPrice() : BigDecimal.ZERO);
        if (goods.getOriginPrice() == null) {
            goods.setOriginPrice(goods.getSalePrice());
        }
        int totalStock = inputSpecs.stream()
                .mapToInt(s -> s.getStock() == null ? 0 : Math.max(0, s.getStock()))
                .sum();
        goods.setTotalStock(totalStock);

        if (goods.getId() == null) {
            goods.setDelFlag(0);
            if (goods.getStatus() == null) {
                goods.setStatus(0);
            }
            if (goods.getSaleCount() == null) {
                goods.setSaleCount(0);
            }
            if (goods.getIsGroup() == null) {
                goods.setIsGroup(0);
            }
            if (goods.getIsSeckill() == null) {
                goods.setIsSeckill(0);
            }
            goodsMapper.insert(goods);
        } else {
            Goods exists = goodsMapper.selectById(goods.getId());
            if (exists == null || (exists.getDelFlag() != null && exists.getDelFlag() == 1)) {
                throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "商品不存在");
            }
            goodsMapper.updateById(goods);
        }

        syncSpecsForGoods(goods.getId(), inputSpecs);
        ensureSingleDefaultSpec(goods.getId(), defaultSpec.getId());
        refreshGoodsTotalStock(goods.getId());
        clearGoodsCache(goods.getId());
        return goods.getId();
    }

    private void validateSpecsForSave(List<GoodsSpec> specs) {
        if (specs == null || specs.isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "至少需要一个销售规格");
        }
        Set<String> names = new HashSet<>();
        int defaultCount = 0;
        for (GoodsSpec spec : specs) {
            if (spec == null || !StringUtils.hasText(spec.getSpecName())) {
                throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "规格名称不能为空");
            }
            String name = spec.getSpecName().trim();
            spec.setSpecName(name);
            String key = name.toLowerCase();
            if (!names.add(key)) {
                throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "规格名称不能重复：" + name);
            }
            if (spec.getSpecPrice() == null || spec.getSpecPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "规格价格不能为空或为负");
            }
            if (spec.getStock() == null || spec.getStock() < 0) {
                spec.setStock(0);
            }
            if (spec.getIsDefault() != null && spec.getIsDefault() == 1) {
                defaultCount++;
            }
        }
        if (defaultCount == 0) {
            specs.get(0).setIsDefault(1);
        } else if (defaultCount > 1) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "只能有一个默认规格");
        }
    }

    /** 按提交列表 upsert 规格，未出现的旧规格软删 */
    private void syncSpecsForGoods(Long goodsId, List<GoodsSpec> inputSpecs) {
        List<GoodsSpec> oldSpecs = listSpecsByGoodsId(goodsId);
        Map<Long, GoodsSpec> oldMap = oldSpecs.stream()
                .collect(Collectors.toMap(GoodsSpec::getId, s -> s, (a, b) -> a));
        Set<Long> keepIds = new HashSet<>();

        for (GoodsSpec input : inputSpecs) {
            input.setGoodsId(goodsId);
            input.setDelFlag(0);
            if (input.getIsDefault() == null) {
                input.setIsDefault(0);
            }
            if (input.getId() != null && oldMap.containsKey(input.getId())) {
                GoodsSpec db = oldMap.get(input.getId());
                if (!db.getGoodsId().equals(goodsId)) {
                    throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "规格不属于该商品");
                }
                specMapper.updateById(input);
                keepIds.add(input.getId());
            } else {
                input.setId(null);
                specMapper.insert(input);
                keepIds.add(input.getId());
            }
        }
        for (GoodsSpec old : oldSpecs) {
            if (!keepIds.contains(old.getId())) {
                old.setDelFlag(1);
                specMapper.updateById(old);
            }
        }
    }

    public void deleteGoods(Long id) {
        Goods goods = goodsMapper.selectById(id);
        if (goods != null) {
            goods.setDelFlag(1);
            goodsMapper.updateById(goods);
            clearGoodsCache(id);
        }
    }

    public List<Goods> listAllGoods() {
        return goodsMapper.selectList(new LambdaQueryWrapper<Goods>()
                .eq(Goods::getDelFlag, 0)
                .orderByDesc(Goods::getId));
    }

    public void saveCategory(GoodsCategory category) {
        if (category.getId() == null) {
            category.setDelFlag(0);
            categoryMapper.insert(category);
        } else {
            categoryMapper.updateById(category);
        }
        redisUtils.delete(GoodsRedisKeyConstant.GOODS_CATEGORY_TREE);
    }

    public void deleteCategory(Long id) {
        GoodsCategory cat = categoryMapper.selectById(id);
        if (cat != null) {
            cat.setDelFlag(1);
            categoryMapper.updateById(cat);
        }
        redisUtils.delete(GoodsRedisKeyConstant.GOODS_CATEGORY_TREE);
    }

    public List<GoodsCategory> listAllCategories() {
        return categoryMapper.selectList(new LambdaQueryWrapper<GoodsCategory>().eq(GoodsCategory::getDelFlag, 0));
    }

    public void saveGroupActivity(GroupActivity activity) {
        validateActivityGoodsFlag(activity.getGoodsId(), true, activity.getStatus());
        if (activity.getId() == null) {
            activity.setDelFlag(0);
            if (activity.getGroupDesc() == null || activity.getGroupDesc().isBlank()) {
                activity.setGroupDesc(generateGroupDesc(activity));
            }
            groupActivityMapper.insert(activity);
        } else {
            groupActivityMapper.updateById(activity);
        }
        redisUtils.delete(GoodsRedisKeyConstant.GROUP_HOT_LIST);
    }

    public void deleteGroupActivity(Long id) {
        GroupActivity activity = groupActivityMapper.selectById(id);
        if (activity != null) {
            activity.setDelFlag(1);
            groupActivityMapper.updateById(activity);
        }
        redisUtils.delete(GoodsRedisKeyConstant.GROUP_HOT_LIST);
    }

    public List<GroupActivity> listAllGroupActivities() {
        return groupActivityMapper.selectList(new LambdaQueryWrapper<GroupActivity>().eq(GroupActivity::getDelFlag, 0));
    }

    public void saveSeckillActivity(SeckillActivity activity) {
        validateActivityGoodsFlag(activity.getGoodsId(), false, activity.getStatus());
        if (activity.getId() == null) {
            activity.setDelFlag(0);
            seckillActivityMapper.insert(activity);
            goodsRedisService.setStock(GoodsRedisKeyConstant.GOODS_SECKILL_STOCK + activity.getId(), activity.getStock());
        } else {
            seckillActivityMapper.updateById(activity);
            goodsRedisService.setStock(GoodsRedisKeyConstant.GOODS_SECKILL_STOCK + activity.getId(), activity.getStock());
        }
    }

    public void deleteSeckillActivity(Long id) {
        SeckillActivity activity = seckillActivityMapper.selectById(id);
        if (activity != null) {
            activity.setDelFlag(1);
            seckillActivityMapper.updateById(activity);
        }
        redisUtils.delete(GoodsRedisKeyConstant.GOODS_SECKILL_STOCK + id);
    }

    public List<SeckillActivity> listAllSeckillActivities() {
        return seckillActivityMapper.selectList(new LambdaQueryWrapper<SeckillActivity>().eq(SeckillActivity::getDelFlag, 0));
    }

    /**
     * 下单计价：按规格价；若指定有效团购/秒杀活动则用活动价。
     */
    public GoodsPriceVO resolvePrice(GoodsPriceQueryDTO query) {
        if (query == null || query.getGoodsId() == null || query.getSpecId() == null) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "商品或规格不能为空");
        }
        Goods goods = goodsMapper.selectById(query.getGoodsId());
        if (goods == null || (goods.getDelFlag() != null && goods.getDelFlag() == 1)) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "商品不存在");
        }
        if (goods.getStatus() != null && goods.getStatus() != 1) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "商品已下架");
        }
        GoodsSpec spec = specMapper.selectById(query.getSpecId());
        if (spec == null || (spec.getDelFlag() != null && spec.getDelFlag() == 1)
                || !query.getGoodsId().equals(spec.getGoodsId())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "商品规格不存在");
        }

        BigDecimal price = spec.getSpecPrice() != null ? spec.getSpecPrice() : goods.getSalePrice();
        if (price == null) {
            price = BigDecimal.ZERO;
        }
        Integer activityType = query.getActivityType() == null ? OrderConstant.ACTIVITY_NORMAL : query.getActivityType();
        Long activityId = query.getActivityId();

        if (activityType == OrderConstant.ACTIVITY_GROUP && activityId != null) {
            GroupActivity act = groupActivityMapper.selectById(activityId);
            if (act == null || (act.getDelFlag() != null && act.getDelFlag() == 1)) {
                throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "团购活动不存在");
            }
            validateActiveActivity(act.getStatus(), act.getStartTime(), act.getEndTime(), "团购");
            if (!query.getGoodsId().equals(act.getGoodsId()) || !query.getSpecId().equals(act.getSpecId())) {
                throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "团购活动与商品规格不匹配");
            }
            price = act.getGroupPrice();
        } else if (activityType == OrderConstant.ACTIVITY_SECKILL && activityId != null) {
            SeckillActivity act = seckillActivityMapper.selectById(activityId);
            if (act == null || (act.getDelFlag() != null && act.getDelFlag() == 1)) {
                throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "秒杀活动不存在");
            }
            validateActiveActivity(act.getStatus(), act.getStartTime(), act.getEndTime(), "秒杀");
            if (!query.getGoodsId().equals(act.getGoodsId()) || !query.getSpecId().equals(act.getSpecId())) {
                throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "秒杀活动与商品规格不匹配");
            }
            price = act.getSeckillPrice();
        } else {
            activityType = OrderConstant.ACTIVITY_NORMAL;
            activityId = null;
        }

        GoodsPriceVO vo = new GoodsPriceVO();
        vo.setGoodsId(goods.getId());
        vo.setSpecId(spec.getId());
        vo.setGoodsName(goods.getGoodsName());
        vo.setGoodsImg(goods.getGoodsImg());
        vo.setPrice(price);
        vo.setActivityType(activityType);
        vo.setActivityId(activityId);
        return vo;
    }

    private void validateActiveActivity(Integer status, LocalDateTime start, LocalDateTime end, String label) {
        if (status == null || status != 1) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), label + "活动未开启");
        }
        LocalDateTime now = LocalDateTime.now();
        if (start != null && now.isBefore(start)) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), label + "活动未开始");
        }
        if (end != null && now.isAfter(end)) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), label + "活动已结束");
        }
    }

    /**
     * 开启团购/秒杀活动时，商品必须已在商品表标记可参与（is_group / is_seckill = 1）。
     * status=0（关闭）时不校验标记，便于下架活动。
     */
    private void validateActivityGoodsFlag(Long goodsId, boolean groupActivity, Integer status) {
        if (status != null && status == 0) {
            return;
        }
        if (goodsId == null) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "请选择商品");
        }
        Goods goods = goodsMapper.selectById(goodsId);
        if (goods == null || (goods.getDelFlag() != null && goods.getDelFlag() == 1)) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "商品不存在");
        }
        if (groupActivity) {
            if (goods.getIsGroup() == null || goods.getIsGroup() != 1) {
                throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(),
                        "该商品未标记「可参与团购」，请先在商品管理中开启团购标记");
            }
        } else {
            if (goods.getIsSeckill() == null || goods.getIsSeckill() != 1) {
                throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(),
                        "该商品未标记「可参与秒杀」，请先在商品管理中开启秒杀标记");
            }
        }
    }

    public void saveSpec(GoodsSpec spec) {
        if (spec == null || spec.getGoodsId() == null) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "商品ID不能为空");
        }
        Goods goods = goodsMapper.selectById(spec.getGoodsId());
        if (goods == null || (goods.getDelFlag() != null && goods.getDelFlag() == 1)) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "商品不存在");
        }
        if (!StringUtils.hasText(spec.getSpecName())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "规格名称不能为空");
        }
        spec.setSpecName(spec.getSpecName().trim());
        if (spec.getStock() == null || spec.getStock() < 0) {
            spec.setStock(0);
        }
        if (spec.getSpecPrice() == null) {
            spec.setSpecPrice(goods.getSalePrice() != null ? goods.getSalePrice() : BigDecimal.ZERO);
        }
        boolean asDefault = spec.getIsDefault() != null && spec.getIsDefault() == 1;
        if (spec.getId() == null) {
            spec.setDelFlag(0);
            if (spec.getIsDefault() == null) {
                List<GoodsSpec> exists = listSpecsByGoodsId(spec.getGoodsId());
                spec.setIsDefault(exists.isEmpty() ? 1 : 0);
                asDefault = spec.getIsDefault() == 1;
            }
            specMapper.insert(spec);
        } else {
            GoodsSpec db = specMapper.selectById(spec.getId());
            if (db == null || (db.getDelFlag() != null && db.getDelFlag() == 1)) {
                throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "规格不存在");
            }
            if (!db.getGoodsId().equals(spec.getGoodsId())) {
                throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "规格不属于该商品");
            }
            specMapper.updateById(spec);
        }
        if (asDefault) {
            ensureSingleDefaultSpec(spec.getGoodsId(), spec.getId());
        } else {
            // 取消默认后保证仍有一个默认规格
            ensureSingleDefaultSpec(spec.getGoodsId(), null);
        }
        refreshGoodsTotalStock(spec.getGoodsId());
        clearGoodsCache(spec.getGoodsId());
    }

    public void deleteSpec(Long specId) {
        if (specId == null) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "规格ID不能为空");
        }
        GoodsSpec spec = specMapper.selectById(specId);
        if (spec == null || (spec.getDelFlag() != null && spec.getDelFlag() == 1)) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "规格不存在");
        }
        List<GoodsSpec> specs = listSpecsByGoodsId(spec.getGoodsId());
        if (specs.size() <= 1) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "至少保留一个规格，可直接编辑该规格");
        }
        spec.setDelFlag(1);
        specMapper.updateById(spec);
        ensureSingleDefaultSpec(spec.getGoodsId(), null);
        refreshGoodsTotalStock(spec.getGoodsId());
        clearGoodsCache(spec.getGoodsId());
    }

    public List<GoodsSpec> listSpecsByGoodsId(Long goodsId) {
        if (goodsId == null) {
            return List.of();
        }
        return specMapper.selectList(new LambdaQueryWrapper<GoodsSpec>()
                .eq(GoodsSpec::getGoodsId, goodsId)
                .eq(GoodsSpec::getDelFlag, 0)
                .orderByDesc(GoodsSpec::getIsDefault)
                .orderByAsc(GoodsSpec::getId));
    }

    /** 缺货/低库存提醒列表（按规格库存） */
    public List<StockAlertVO> listStockAlerts(Integer threshold, Boolean onlyOnSale) {
        int th = resolveThreshold(threshold);
        boolean onSaleOnly = onlyOnSale == null || onlyOnSale;

        List<GoodsSpec> lowSpecs = specMapper.selectList(new LambdaQueryWrapper<GoodsSpec>()
                .eq(GoodsSpec::getDelFlag, 0)
                .le(GoodsSpec::getStock, th)
                .orderByAsc(GoodsSpec::getStock)
                .orderByAsc(GoodsSpec::getGoodsId));
        if (lowSpecs.isEmpty()) {
            return List.of();
        }

        Set<Long> goodsIds = lowSpecs.stream().map(GoodsSpec::getGoodsId).collect(Collectors.toSet());
        Map<Long, Goods> goodsMap = goodsMapper.selectList(new LambdaQueryWrapper<Goods>()
                        .in(Goods::getId, goodsIds)
                        .eq(Goods::getDelFlag, 0))
                .stream()
                .collect(Collectors.toMap(Goods::getId, g -> g, (a, b) -> a));

        List<StockAlertVO> result = new ArrayList<>();
        for (GoodsSpec spec : lowSpecs) {
            Goods goods = goodsMap.get(spec.getGoodsId());
            if (goods == null) {
                continue;
            }
            if (onSaleOnly && (goods.getStatus() == null || goods.getStatus() != 1)) {
                continue;
            }
            result.add(toStockAlert(goods, spec, th));
        }
        result.sort(Comparator
                .comparing((StockAlertVO v) -> "OUT".equals(v.getLevel()) ? 0 : 1)
                .thenComparing(v -> v.getStock() == null ? 0 : v.getStock())
                .thenComparing(StockAlertVO::getGoodsId));
        return result;
    }

    public long countStockAlerts(Integer threshold, Boolean onlyOnSale) {
        return listStockAlerts(threshold, onlyOnSale).size();
    }

    /** 管理端补货：增加规格库存，并同步商品总库存 */
    @Transactional(rollbackFor = Exception.class)
    public void restock(StockRestockDTO dto) {
        if (dto == null || dto.getAddNum() == null || dto.getAddNum() <= 0) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "补货数量必须大于 0");
        }
        GoodsSpec spec = resolveRestockSpec(dto);
        int before = spec.getStock() == null ? 0 : spec.getStock();
        spec.setStock(before + dto.getAddNum());
        specMapper.updateById(spec);

        Goods goods = goodsMapper.selectById(spec.getGoodsId());
        if (goods == null || (goods.getDelFlag() != null && goods.getDelFlag() == 1)) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "商品不存在");
        }
        int total = goods.getTotalStock() == null ? 0 : goods.getTotalStock();
        goods.setTotalStock(total + dto.getAddNum());
        goodsMapper.updateById(goods);
        clearGoodsCache(goods.getId());
    }

    private GoodsSpec resolveRestockSpec(StockRestockDTO dto) {
        if (dto.getSpecId() != null) {
            GoodsSpec spec = specMapper.selectById(dto.getSpecId());
            if (spec == null || (spec.getDelFlag() != null && spec.getDelFlag() == 1)) {
                throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "规格不存在");
            }
            return spec;
        }
        if (dto.getGoodsId() == null) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "请指定规格或商品");
        }
        List<GoodsSpec> specs = listSpecsByGoodsId(dto.getGoodsId());
        if (specs.isEmpty()) {
            Goods goods = goodsMapper.selectById(dto.getGoodsId());
            if (goods == null || (goods.getDelFlag() != null && goods.getDelFlag() == 1)) {
                throw new BusinessException(ErrorCodeEnum.BAD_REQUEST.getCode(), "商品不存在");
            }
            specs = ensureDefaultSpec(goods);
        }
        return specs.get(0);
    }

    private StockAlertVO toStockAlert(Goods goods, GoodsSpec spec, int threshold) {
        StockAlertVO vo = new StockAlertVO();
        vo.setGoodsId(goods.getId());
        vo.setGoodsName(goods.getGoodsName());
        vo.setGoodsImg(goods.getGoodsImg());
        vo.setGoodsStatus(goods.getStatus());
        vo.setUnit(goods.getUnit());
        vo.setTotalStock(goods.getTotalStock());
        vo.setSpecId(spec.getId());
        vo.setSpecName(spec.getSpecName());
        vo.setStock(spec.getStock() == null ? 0 : spec.getStock());
        vo.setLevel(vo.getStock() <= 0 ? "OUT" : "LOW");
        vo.setThreshold(threshold);
        return vo;
    }

    private int resolveThreshold(Integer threshold) {
        if (threshold != null && threshold >= 0) {
            return threshold;
        }
        return Math.max(0, properties.getStockWarnThreshold());
    }

    private String generateGroupDesc(GroupActivity activity) {
        try {
            Goods goods = goodsMapper.selectById(activity.getGoodsId());
            GroupTextRequestDTO req = new GroupTextRequestDTO();
            req.setGoodsId(activity.getGoodsId());
            req.setGoodsName(goods == null ? "生鲜商品" : goods.getGoodsName());
            req.setSpec(String.valueOf(activity.getGroupPrice()));
            Result<String> result = aiFeignClient.generateGroupText(req);
            if (result != null && result.getData() != null) {
                return result.getData();
            }
        } catch (Exception ignored) {
            // AI 不可用时使用默认文案
        }
        return "限时团购，新鲜直达，拼团更优惠！";
    }

    private void clearGoodsCache(Long goodsId) {
        redisUtils.delete(GoodsRedisKeyConstant.GOODS_DETAIL + goodsId);
        redisUtils.delete(GoodsRedisKeyConstant.GOODS_HOT_LIST);
    }
}
