package com.tea.service;

import com.tea.dao.AdminDao;
import com.tea.dao.DaoException;
import com.tea.entity.Admin;
import com.tea.util.PasswordUtil;

/** 店员账号 Service：注册、登录。 */
public class AdminService {

    private static final int MIN_PASSWORD_LEN = 6;
    private static final int MAX_USERNAME_LEN = 32;

    private final AdminDao adminDao = new AdminDao();

    /**
     * 登录：成功返回 Admin，失败返回 null。
     * 用户不存在和密码错误统一返回 null，界面只提示"用户名或密码错误"，防止账号被枚举。
     */
    public Admin login(String username, String password) {
        if (isBlank(username) || isBlank(password)) {
            return null;
        }
        return adminDao.findByUsernameAndPassword(username.trim(), PasswordUtil.sha256(password));
    }

    /** 注册：校验失败抛 BusinessException。 */
    public void register(String username, String password) {
        if (isBlank(username) || isBlank(password)) {
            throw new BusinessException("用户名/密码不能为空");
        }
        String u = username.trim();
        if (u.length() > MAX_USERNAME_LEN) {
            throw new BusinessException("用户名最多 " + MAX_USERNAME_LEN + " 个字符");
        }
        if (password.length() < MIN_PASSWORD_LEN) {
            throw new BusinessException("密码长度必须 ≥ " + MIN_PASSWORD_LEN + " 位");
        }
        if (adminDao.existsByUsername(u)) {
            throw new BusinessException("用户名已被占用");
        }
        try {
            adminDao.insert(new Admin(u, PasswordUtil.sha256(password)));
        } catch (DaoException e) {
            // 并发注册同名：先查后插之间被别人抢先，由数据库唯一索引兜底
            if (e.isDuplicateKey()) {
                throw new BusinessException("用户名已被占用");
            }
            throw e;
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
