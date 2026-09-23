package ru.yandex.practicum.filmorate.storage.user;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class InMemoryUserStorage implements UserStorage {
    private final Map<Long, User> users = new LinkedHashMap<>();
    private long nextId = 1;

    @Override
    public Collection<User> findAll() {
        return new ArrayList<>(users.values());
    }

    @Override
    public User findById(long id) {
        User user = users.get(id);
        if (user == null) {
            throw new NotFoundException("Пользователь с id = " + id + " не найден");
        }
        return user;
    }

    @Override
    public User create(User user) {
        user.setId(nextId++);
        users.put(user.getId(), user);
        return user;
    }

    @Override
    public User update(User user) {
        User saved = findById(user.getId());
        if (user.getEmail() != null) {
            saved.setEmail(user.getEmail());
        }
        if (user.getLogin() != null) {
            saved.setLogin(user.getLogin());
        }
        if (user.getBirthday() != null) {
            saved.setBirthday(user.getBirthday());
        }
        if (user.getName() != null) {
            saved.setName(user.getName().isBlank() ? saved.getLogin() : user.getName());
        }
        return saved;
    }

    @Override
    public void delete(long id) {
        findById(id);
        users.remove(id);
    }
}
