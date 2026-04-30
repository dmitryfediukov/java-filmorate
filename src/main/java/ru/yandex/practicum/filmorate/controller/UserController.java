package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.DuplicatedDataException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/users")
public class UserController {
    private final Map<Long, User> users = new HashMap<>();

    @GetMapping
    public Collection<User> findAll() {
        return users.values();
    }

    @PostMapping
    public User create(@RequestBody User user) {
        validateUser(user);

        String email = user.getEmail();
        String login = user.getLogin();

        if (users.values().stream()
                .anyMatch(savedUser -> savedUser.getEmail().equals(email))) {
            log.warn("Ошибка создания пользователя: email уже используется, email={}", email);
            throw new DuplicatedDataException("Этот email уже используется");
        }

        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(login);
        }

        user.setId(getNextId());
        users.put(user.getId(), user);

        log.info("Создан пользователь: id={}, email={}, login={}",
                user.getId(), user.getEmail(), user.getLogin());

        return user;
    }

    @PutMapping
    public User update(@RequestBody User newUser) {
        Long id = newUser.getId();

        if (id == null) {
            log.warn("Ошибка обновления пользователя: id не указан");
            throw new ValidationException("Id должен быть указан");
        }

        if (!users.containsKey(id)) {
            log.warn("Ошибка обновления пользователя: пользователь с id={} не найден", id);
            throw new NotFoundException("Пользователь с id = " + id + " не найден");
        }

        validateUser(newUser);

        if (users.values().stream()
                .anyMatch(savedUser -> !savedUser.getId().equals(id)
                        && savedUser.getEmail().equals(newUser.getEmail()))) {
            log.warn("Ошибка обновления пользователя: email уже используется, email={}", newUser.getEmail());
            throw new DuplicatedDataException("Этот email уже используется");
        }

        User oldUser = users.get(id);

        oldUser.setEmail(newUser.getEmail());
        oldUser.setLogin(newUser.getLogin());
        oldUser.setBirthday(newUser.getBirthday());

        if (newUser.getName() == null || newUser.getName().isBlank()) {
            oldUser.setName(newUser.getLogin());
        } else {
            oldUser.setName(newUser.getName());
        }

        log.info("Обновлён пользователь: id={}, email={}, login={}",
                oldUser.getId(), oldUser.getEmail(), oldUser.getLogin());

        return oldUser;
    }

    private void validateUser(User user) {
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

    // вспомогательный метод для генерации идентификатора нового пользователя
    private long getNextId() {
        long currentMaxId = users.keySet()
                .stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0);
        return ++currentMaxId;
    }
}
