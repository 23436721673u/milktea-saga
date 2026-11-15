package com.tea.service;

import com.tea.dao.DrinkDao;
import com.tea.entity.Drink;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/** 奶茶商品 Service：CRUD、软删除、库存预警。 */
public class DrinkService {

    public static final int STOCK_WARN_THRESHOLD = 10;
    public static final Set<String> CATEGORIES = Set.of("奶茶", "果茶", "特调");
    /** 与 DECIMAL(6,2) 对齐 */
    public static final BigDecimal MAX_PRICE = new BigDecimal("9999.99");
    public static final int MAX_STOCK = 99999;
    private static final int MAX_NAME_LEN = 64;

    private final DrinkDao drinkDao = new DrinkDao();

    public List<Drink> listAllActive() {
        return drinkDao.listAllActive();
    }

    public Drink findById(long id) {
        return drinkDao.findById(id);
    }

    /** 新增奶茶，返回带自增 ID 的对象。 */
    public Drink addDrink(String name, String category, BigDecimal price, int stock) {
        if (name == null || name.isBlank()) {
            throw new BusinessException("奶茶名不能为空");
        }
        if (name.trim().length() > MAX_NAME_LEN) {
            throw new BusinessException("奶茶名最多 " + MAX_NAME_LEN + " 个字符");
        }
        if (!CATEGORIES.contains(category)) {
            throw new BusinessException("分类只能是：奶茶 / 果茶 / 特调");
        }
        validatePriceAndStock(price, stock);
        Drink d = new Drink(name.trim(), category, price, stock);
        drinkDao.insert(d);
        return d;
    }

    public void updateDrink(long id, BigDecimal price, int stock) {
        validatePriceAndStock(price, stock);
        if (drinkDao.updatePriceAndStock(id, price, stock) != 1) {
            throw new BusinessException("奶茶不存在或已下架");
        }
    }

    public void deactivate(long id) {
        if (drinkDao.deactivate(id) != 1) {
            throw new BusinessException("奶茶不存在或已下架");
        }
    }

    public List<Drink> listLowStock() {
        return drinkDao.listLowStock(STOCK_WARN_THRESHOLD);
    }

    public static boolean isLowStock(Drink d) {
        return d.getStock() < STOCK_WARN_THRESHOLD;
    }

    private void validatePriceAndStock(BigDecimal price, int stock) {
        if (price == null || price.signum() <= 0 || price.compareTo(MAX_PRICE) > 0) {
            throw new BusinessException("单价必须在 0.01 ~ " + MAX_PRICE.toPlainString() + " 之间");
        }
        if (stock < 0 || stock > MAX_STOCK) {
            throw new BusinessException("库存必须在 0 ~ " + MAX_STOCK + " 之间");
        }
    }
}
