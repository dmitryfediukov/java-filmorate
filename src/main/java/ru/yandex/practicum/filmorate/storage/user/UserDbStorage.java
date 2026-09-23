package ru.yandex.practicum.filmorate.storage.user;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;

import java.sql.Date;
import java.util.Collection;
import java.util.List;

@Repository
public class UserDbStorage implements UserStorage {
    private final JdbcTemplate jdbc;

    public UserDbStorage(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Collection<User> findAll() {
        return jdbc.query("SELECT id, email, login, name, birthday FROM users ORDER BY id",
                (rs, rowNum) -> mapUser(rs));
    }

    @Override
    public User findById(long id) {
        List<User> found = jdbc.query("SELECT id, email, login, name, birthday FROM users WHERE id = ?",
                (rs, rowNum) -> mapUser(rs), id);
        if (found.isEmpty()) {
            throw new NotFoundException("Пользователь с id = " + id + " не найден");
        }
        return found.getFirst();
    }

    @Override
    public User create(User user) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            java.sql.PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO users (email, login, name, birthday) VALUES (?, ?, ?, ?)", new String[]{"ID"});
            statement.setString(1, user.getEmail());
            statement.setString(2, user.getLogin());
            statement.setString(3, user.getName());
            statement.setDate(4, Date.valueOf(user.getBirthday()));
            return statement;
        }, keys);
        user.setId(keys.getKey().longValue());
        return findById(user.getId());
    }

    @Override
    public User update(User update) {
        User saved = findById(update.getId());
        String email = update.getEmail() == null ? saved.getEmail() : update.getEmail();
        String login = update.getLogin() == null ? saved.getLogin() : update.getLogin();
        String name = update.getName() == null ? saved.getName() : update.getName();
        if (name.isBlank()) {
            name = login;
        }
        java.time.LocalDate birthday = update.getBirthday() == null ? saved.getBirthday() : update.getBirthday();
        jdbc.update("UPDATE users SET email = ?, login = ?, name = ?, birthday = ? WHERE id = ?",
                email, login, name, Date.valueOf(birthday), update.getId());
        return findById(update.getId());
    }

    @Override
    public void delete(long id) {
        findById(id);
        jdbc.update("DELETE FROM users WHERE id = ?", id);
    }

    @Override
    @Transactional
    public void addFriend(long userId, long friendId) {
        findById(userId);
        findById(friendId);
        long low = Math.min(userId, friendId);
        long high = Math.max(userId, friendId);
        List<Friendship> rows = jdbc.query(
                "SELECT requested_by_user_id, status FROM friendships WHERE user_low_id = ? AND user_high_id = ?",
                (rs, rowNum) -> new Friendship(rs.getLong("requested_by_user_id"), rs.getString("status")),
                low, high);
        if (rows.isEmpty()) {
            jdbc.update("INSERT INTO friendships (user_low_id, user_high_id, requested_by_user_id, status)"
                    + " VALUES (?, ?, ?, 'PENDING')", low, high, userId);
        } else if (rows.getFirst().requester() != userId && rows.getFirst().status().equals("PENDING")) {
            jdbc.update("UPDATE friendships SET status = 'CONFIRMED' WHERE user_low_id = ? AND user_high_id = ?",
                    low, high);
        }
    }

    @Override
    @Transactional
    public void removeFriend(long userId, long friendId) {
        findById(userId);
        findById(friendId);
        long low = Math.min(userId, friendId);
        long high = Math.max(userId, friendId);
        List<Friendship> rows = jdbc.query(
                "SELECT requested_by_user_id, status FROM friendships WHERE user_low_id = ? AND user_high_id = ?",
                (rs, rowNum) -> new Friendship(rs.getLong("requested_by_user_id"), rs.getString("status")),
                low, high);
        if (rows.isEmpty()) {
            return;
        }
        Friendship friendship = rows.getFirst();
        if (friendship.status().equals("CONFIRMED")) {
            jdbc.update("UPDATE friendships SET status = 'PENDING', requested_by_user_id = ?"
                    + " WHERE user_low_id = ? AND user_high_id = ?", friendId, low, high);
        } else if (friendship.requester() == userId) {
            jdbc.update("DELETE FROM friendships WHERE user_low_id = ? AND user_high_id = ?", low, high);
        }
    }

    private User mapUser(java.sql.ResultSet rs) throws java.sql.SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setEmail(rs.getString("email"));
        user.setLogin(rs.getString("login"));
        user.setName(rs.getString("name"));
        user.setBirthday(rs.getDate("birthday").toLocalDate());
        user.getFriends().addAll(jdbc.queryForList(
                "SELECT CASE WHEN user_low_id = ? THEN user_high_id ELSE user_low_id END"
                        + " FROM friendships WHERE (user_low_id = ? OR user_high_id = ?)"
                        + " AND (status = 'CONFIRMED' OR requested_by_user_id = ?)"
                        + " ORDER BY 1",
                Long.class, user.getId(), user.getId(), user.getId(), user.getId()));
        return user;
    }

    private record Friendship(long requester, String status) {
    }
}
