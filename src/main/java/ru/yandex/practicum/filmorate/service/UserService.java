package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.DuplicatedDataException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Slf4j
@Service
public class UserService {
    private final UserStorage userStorage;

    @Autowired
    public UserService(UserStorage userStorage) {
        this.userStorage = userStorage;
    }

    public Collection<User> findAll() {
        return userStorage.findAll();
    }

    public User findById(long id) {
        return userStorage.findById(id);
    }

    public User create(User user) {
        validateUserForCreate(user);
        validateUniqueEmail(user.getEmail(), null);
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
        User created = userStorage.create(user);
        log.info("Создан пользователь: id={}", created.getId());
        return created;
    }

    public User update(User user) {
        if (user.getId() == null) {
            throw new ValidationException("Id должен быть указан");
        }
        userStorage.findById(user.getId());
        validateUserForUpdate(user);
        validateUniqueEmail(user.getEmail(), user.getId());
        User updated = userStorage.update(user);
        log.info("Обновлён пользователь: id={}", updated.getId());
        return updated;
    }

    public void addFriend(long id, long friendId) {
        User user = userStorage.findById(id);
        User friend = userStorage.findById(friendId);
        if (id == friendId) {
            throw new ValidationException("Нельзя добавить себя в друзья");
        }
        user.getFriends().add(friendId);
        friend.getFriends().add(id);
        log.info("Добавлена дружба: userId={}, friendId={}", id, friendId);
    }

    public void removeFriend(long id, long friendId) {
        User user = userStorage.findById(id);
        User friend = userStorage.findById(friendId);
        user.getFriends().remove(friendId);
        friend.getFriends().remove(id);
        log.info("Удалена дружба: userId={}, friendId={}", id, friendId);
    }

    public List<User> findFriends(long id) {
        return userStorage.findById(id).getFriends().stream()
                .sorted()
                .map(userStorage::findById)
                .toList();
    }

    public List<User> findCommonFriends(long id, long otherId) {
        User user = userStorage.findById(id);
        User other = userStorage.findById(otherId);
        return user.getFriends().stream()
                .filter(other.getFriends()::contains)
                .sorted()
                .map(userStorage::findById)
                .toList();
    }

    private void validateUniqueEmail(String email, Long id) {
        if (email != null && userStorage.findAll().stream()
                .anyMatch(saved -> !saved.getId().equals(id) && saved.getEmail().equals(email))) {
            throw new DuplicatedDataException("Этот email уже используется");
        }
    }

    private void validateUserForCreate(User user) {
        String email = user.getEmail();
        String login = user.getLogin();
        LocalDate birthday = user.getBirthday();

        if (email == null || email.isBlank() || !email.contains("@")) {
            log.warn("Ошибка валидации пользователя: некорректный email={}", email);
            throw new ValidationException("Электронная почта не может быть пустой и должна содержать символ @");
        }

        if (login == null || login.isBlank() || login.contains(" ")) {
            log.warn("Ошибка валидации пользователя: некорректный login={}", login);
            throw new ValidationException("Логин не может быть пустым и содержать пробелы");
        }

        if (birthday == null || birthday.isAfter(LocalDate.now())) {
            log.warn("Ошибка валидации пользователя: некорректная дата рождения={}", birthday);
            throw new ValidationException("Дата рождения не может быть в будущем");
        }
    }

    private void validateUserForUpdate(User user) {
        String email = user.getEmail();
        String login = user.getLogin();
        LocalDate birthday = user.getBirthday();

        if (email != null) {
            if (email.isBlank() || !email.contains("@")) {
                log.warn("Ошибка валидации пользователя: некорректный email={}", email);
                throw new ValidationException("Некорректный email");
            }
        }

        if (login != null) {
            if (login.isBlank() || login.contains(" ")) {
                log.warn("Ошибка валидации пользователя: некорректный login={}", login);
                throw new ValidationException("Некорректный login");
            }
        }

        if (birthday != null) {
            if (birthday.isAfter(LocalDate.now())) {
                log.warn("Ошибка валидации пользователя: некорректная дата рождения={}", birthday);
                throw new ValidationException("Дата рождения не может быть в будущем");
            }
        }
    }
}
