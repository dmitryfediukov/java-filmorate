package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.DuplicatedDataException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class UserControllerTest {

    private UserController controller;
    private User user;

    @BeforeEach
    void setUp() {
        controller = new UserController();

        user = new User();
        user.setEmail("user@mail.ru");
        user.setLogin("login");
        user.setName("Name");
        user.setBirthday(LocalDate.of(2000, 1, 1));
    }

    @Test
    void shouldCreateUser_whenValid() {
        User created = controller.create(user);

        assertNotNull(created.getId());
        assertEquals("user@mail.ru", created.getEmail());
    }

    @Test
    void shouldThrow_whenEmailInvalid() {
        user.setEmail("wrong");

        assertThrows(ValidationException.class, () -> controller.create(user));
    }

    @Test
    void shouldThrow_whenLoginBlank() {
        user.setLogin(" ");

        assertThrows(ValidationException.class, () -> controller.create(user));
    }

    @Test
    void shouldThrow_whenBirthdayInFuture() {
        user.setBirthday(LocalDate.now().plusDays(1));

        assertThrows(ValidationException.class, () -> controller.create(user));
    }

    @Test
    void shouldSetLoginAsName_whenNameNull() {
        user.setName(null);

        User created = controller.create(user);

        assertEquals("login", created.getName());
    }

    // --- UPDATE (частичный) ---

    @Test
    void shouldUpdateOnlyEmail() {
        User created = controller.create(user);

        User update = new User();
        update.setId(created.getId());
        update.setEmail("new@mail.ru");

        User result = controller.update(update);

        assertEquals("new@mail.ru", result.getEmail());
        assertEquals(created.getLogin(), result.getLogin());
    }

    @Test
    void shouldNotOverwriteWithNulls() {
        User created = controller.create(user);

        User update = new User();
        update.setId(created.getId());

        User result = controller.update(update);

        assertEquals(created.getEmail(), result.getEmail());
        assertEquals(created.getLogin(), result.getLogin());
    }

    @Test
    void shouldThrow_whenDuplicateEmailOnUpdate() {
        User u1 = controller.create(user);

        User u2 = new User();
        u2.setEmail("second@mail.ru");
        u2.setLogin("login2");
        u2.setBirthday(LocalDate.of(2000, 1, 1));
        controller.create(u2);

        User update = new User();
        update.setId(u2.getId());
        update.setEmail(u1.getEmail());

        assertThrows(DuplicatedDataException.class, () -> controller.update(update));
    }

    @Test
    void shouldThrow_whenUpdateIdNull() {
        User update = new User();

        assertThrows(ValidationException.class, () -> controller.update(update));
    }

    @Test
    void shouldThrow_whenUserNotFound() {
        User update = new User();
        update.setId(999L);

        assertThrows(NotFoundException.class, () -> controller.update(update));
    }
}