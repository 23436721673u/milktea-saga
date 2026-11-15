package com.tea.service;

import com.tea.dao.DaoException;
import com.tea.dao.MemberDao;
import com.tea.dao.TeaOrderDao;
import com.tea.entity.Member;
import com.tea.entity.TeaOrder;

import java.util.List;
import java.util.regex.Pattern;

/** 会员 Service。积分累加放在 OrderService 的下单事务里完成。 */
public class MemberService {

    private static final Pattern PHONE = Pattern.compile("^1\\d{10}$");
    private static final int MAX_NAME_LEN = 32;

    private final MemberDao memberDao = new MemberDao();
    private final TeaOrderDao teaOrderDao = new TeaOrderDao();

    public static boolean isValidPhone(String phone) {
        return phone != null && PHONE.matcher(phone).matches();
    }

    public Member findByPhone(String phone) {
        if (!isValidPhone(phone)) {
            return null;
        }
        return memberDao.findByPhone(phone);
    }

    public Member findById(long id) {
        return memberDao.findById(id);
    }

    public List<Member> listAll() {
        return memberDao.listAll();
    }

    public Member addMember(String name, String phone) {
        validateName(name);
        if (!isValidPhone(phone)) {
            throw new BusinessException("手机号必须是 1 开头的 11 位数字");
        }
        if (memberDao.findByPhone(phone) != null) {
            throw new BusinessException("该手机号已注册");
        }
        Member m = new Member(name.trim(), phone);
        try {
            memberDao.insert(m);
        } catch (DaoException e) {
            if (e.isDuplicateKey()) {
                throw new BusinessException("该手机号已注册");
            }
            throw e;
        }
        return m;
    }

    public void updateMember(long id, String name, int points) {
        validateName(name);
        if (points < 0) {
            throw new BusinessException("积分不能为负数");
        }
        if (memberDao.updateNameAndPoints(id, name.trim(), points) != 1) {
            throw new BusinessException("会员不存在");
        }
    }

    public List<TeaOrder> listOrders(long memberId) {
        return teaOrderDao.listByMemberId(memberId);
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException("姓名不能为空");
        }
        if (name.trim().length() > MAX_NAME_LEN) {
            throw new BusinessException("姓名最多 " + MAX_NAME_LEN + " 个字符");
        }
    }
}
