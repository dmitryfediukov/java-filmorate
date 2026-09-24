package ru.yandex.practicum.filmorate.storage.user;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public class UserDbStorage implements UserStorage {
    private static final String SELECT_ALL_USERS = "SELECT id, email, login, name, birthday FROM users ORDER BY id";
    private static final String SELECT_USER_BY_ID = "SELECT id, email, login, name, birthday FROM users WHERE id = ?";
    private static final String INSERT_USER = "INSERT INTO users (email, login, name, birthday) VALUES (?, ?, ?, ?)";
    private static final String UPDATE_USER =
            "UPDATE users SET email = ?, login = ?, name = ?, birthday = ? WHERE id = ?";
    private static final String DELETE_USER = "DELETE FROM users WHERE id = ?";
    private static final String SELECT_FRIENDS =
            "SELECT friend_id FROM friendships_directed WHERE user_id = ? ORDER BY friend_id";
    private static final String SELECT_REVERSE_FRIENDSHIP =
            "SELECT COUNT(*) FROM friendships_directed WHERE user_id = ? AND friend_id = ?";
    private static final String UPSERT_FRIENDSHIP =
            "MERGE INTO friendships_directed (user_id, friend_id, status) KEY(user_id, friend_id) VALUES (?, ?, ?)";
    private static final String CONFIRM_FRIENDSHIP =
            "UPDATE friendships_directed SET status = 'CONFIRMED' WHERE user_id = ? AND friend_id = ?";
    private static final String DELETE_FRIENDSHIP =
            "DELETE FROM friendships_directed WHERE user_id = ? AND friend_id = ?";
    private static final String RESET_FRIENDSHIP =
            "UPDATE friendships_directed SET status = 'PENDING' WHERE user_id = ? AND friend_id = ?";
    private final JdbcTemplate jdbc;

    public UserDbStorage(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Collection<User> findAll() {
        return jdbc.query(SELECT_ALL_USERS,
                (rs, rowNum) -> mapUser(rs));
    }

    @Override
    public User findById(long id) {
        List<User> found = jdbc.query(SELECT_USER_BY_ID,
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
            PreparedStatement statement = connection.prepareStatement(INSERT_USER, new String[]{"ID"});
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
        LocalDate birthday = update.getBirthday() == null ? saved.getBirthday() : update.getBirthday();
        jdbc.update(UPDATE_USER,
                email, login, name, Date.valueOf(birthday), update.getId());
        return findById(update.getId());
    }

    @Override
    public void delete(long id) {
        findById(id);
        jdbc.update(DELETE_USER, id);
    }

    @Override
    @Transactional
    public void addFriend(long userId, long friendId) {
        findById(userId);
        findById(friendId);
        boolean reciprocal = jdbc.queryForObject(SELECT_REVERSE_FRIENDSHIP, Integer.class, friendId, userId) > 0;
        if (reciprocal) {
            jdbc.update(CONFIRM_FRIENDSHIP, friendId, userId);
        }
        jdbc.update(UPSERT_FRIENDSHIP, userId, friendId, reciprocal ? "CONFIRMED" : "PENDING");
    }

    @Override
    @Transactional
    public void removeFriend(long userId, long friendId) {
        findById(userId);
        findById(friendId);
        if (jdbc.update(DELETE_FRIENDSHIP, userId, friendId) > 0) {
            jdbc.update(RESET_FRIENDSHIP, friendId, userId);
        }
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setEmail(rs.getString("email"));
        user.setLogin(rs.getString("login"));
        user.setName(rs.getString("name"));
        user.setBirthday(rs.getDate("birthday").toLocalDate());
        user.getFriends().addAll(jdbc.queryForList(SELECT_FRIENDS, Long.class, user.getId()));
        return user;
    }
}
