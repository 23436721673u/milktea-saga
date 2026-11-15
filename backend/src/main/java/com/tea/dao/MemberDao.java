package com.tea.dao;

import com.tea.entity.Member;
import com.tea.util.DbUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 会员 DAO。 */
public class MemberDao {

    private static final String COLUMNS = "id, name, phone, points, created_at";

    public Member findByPhone(String phone) {
        String sql = "SELECT " + COLUMNS + " FROM member WHERE phone = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, phone);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new DaoException("按手机号查会员失败", e);
        }
    }

    public Member findById(long id) {
        String sql = "SELECT " + COLUMNS + " FROM member WHERE id = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new DaoException("按 ID 查会员失败", e);
        }
    }

    public List<Member> listAll() {
        String sql = "SELECT " + COLUMNS + " FROM member ORDER BY id";
        List<Member> list = new ArrayList<>();
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new DaoException("查询会员列表失败", e);
        }
        return list;
    }

    public int insert(Member m) {
        String sql = "INSERT INTO member (name, phone, points) VALUES (?, ?, ?)";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, m.getName());
            ps.setString(2, m.getPhone());
            ps.setInt(3, m.getPoints() == null ? 0 : m.getPoints());
            int n = ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    m.setId(keys.getLong(1));
                }
            }
            return n;
        } catch (SQLException e) {
            throw new DaoException("新增会员失败", e);
        }
    }

    /** 修改姓名和积分（积分支持手动调整，用于补偿误操作）。 */
    public int updateNameAndPoints(long id, String name, int points) {
        String sql = "UPDATE member SET name = ?, points = ? WHERE id = ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setInt(2, points);
            ps.setLong(3, id);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("修改会员失败", e);
        }
    }

    /** 在外部事务里累加积分：points = points + ?，由数据库原子完成，不先查后改。 */
    public int addPointsWithConn(Connection conn, long memberId, int delta) throws SQLException {
        String sql = "UPDATE member SET points = points + ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, delta);
            ps.setLong(2, memberId);
            return ps.executeUpdate();
        }
    }

    /** 在同一事务里读回最新积分。 */
    public int findPointsWithConn(Connection conn, long memberId) throws SQLException {
        String sql = "SELECT points FROM member WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        throw new SQLException("会员不存在：id=" + memberId);
    }

    private Member map(ResultSet rs) throws SQLException {
        Member m = new Member();
        m.setId(rs.getLong("id"));
        m.setName(rs.getString("name"));
        m.setPhone(rs.getString("phone"));
        m.setPoints(rs.getInt("points"));
        // DATETIME 按原值读取，不做时区换算（JVM 与 MySQL 时区不一致时也不会偏移）
        m.setCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        return m;
    }
}
