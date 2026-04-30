package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.DuplicatedDataException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class UserControllerTest {

    private final UserController controller = new UserController();

    @Test
    void shouldCreateUserWhenDataIsValid() {
        User user = new User();
        user.setEmail("user@mail.ru");
        user.setLogin("login");
        user.setName("Name");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        User createdUser = controller.create(user);

        assertNotNull(createdUser.getId());
        assertEquals("user@mail.ru", createdUser.getEmail());
        assertEquals(1, controller.findAll().size());
    }

    @Test
    void shouldThrowExceptionWhenEmailIsNull() {
        User user = new User();
        user.setEmail(null);
        user.setLogin("login");
        user.setName("Name");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        assertThrows(ValidationException.class, () -> controller.create(user));
    }

    @Test
    void shouldThrowExceptionWhenEmailIsBlank() {
        User user = new User();
        user.setEmail("   ");
        user.setLogin("login");
        user.setName("Name");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        assertThrows(ValidationException.class, () -> controller.create(user));
    }

    @Test
    void shouldThrowExceptionWhenEmailDoesNotContainAt() {
        User user = new User();
        user.setEmail("wrong-email");
        user.setLogin("login");
        user.setName("Name");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        assertThrows(ValidationException.class, () -> controller.create(user));
    }

    @Test
    void shouldThrowExceptionWhenEmailIsDuplicated() {
        User firstUser = new User();
        firstUser.setEmail("user@mail.ru");
        firstUser.setLogin("login1");
        firstUser.setName("Name 1");
        firstUser.setBirthday(LocalDate.of(2000, 1, 1));

        controller.create(firstUser);

        User secondUser = new User();
        secondUser.setEmail("user@mail.ru");
        secondUser.setLogin("login2");
        secondUser.setName("Name 2");
        secondUser.setBirthday(LocalDate.of(2001, 1, 1));

        assertThrows(DuplicatedDataException.class, () -> controller.create(secondUser));
    }

    @Test
    void shouldThrowExceptionWhenLoginIsNull() {
        User user = new User();
        user.setEmail("user@mail.ru");
        user.setLogin(null);
        user.setName("Name");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        assertThrows(ValidationException.class, () -> controller.create(user));
    }

    @Test
    void shouldThrowExceptionWhenLoginIsBlank() {
        User user = new User();
        user.setEmail("user@mail.ru");
        user.setLogin("   ");
        user.setName("Name");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        assertThrows(ValidationException.class, () -> controller.create(user));
    }

    @Test
    void shouldThrowExceptionWhenLoginContainsSpace() {
        User user = new User();
        user.setEmail("user@mail.ru");
        user.setLogin("my login");
        user.setName("Name");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        assertThrows(ValidationException.class, () -> controller.create(user));
    }

    @Test
    void shouldSetLoginAsNameWhenNameIsNull() {
        User user = new User();
        user.setEmail("user@mail.ru");
        user.setLogin("login");
        user.setName(null);
        user.setBirthday(LocalDate.of(2000, 1, 1));

        User createdUser = controller.create(user);

        assertEquals("login", createdUser.getName());
    }

    @Test
    void shouldSetLoginAsNameWhenNameIsBlank() {
        User user = new User();
        user.setEmail("user@mail.ru");
        user.setLogin("login");
        user.setName("   ");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        User createdUser = controller.create(user);

        assertEquals("login", createdUser.getName());
    }

    @Test
    void shouldThrowExceptionWhenBirthdayIsInFuture() {
        User user = new User();
        user.setEmail("user@mail.ru");
        user.setLogin("login");
        user.setName("Name");
        user.setBirthday(LocalDate.now().plusDays(1));

        assertThrows(ValidationException.class, () -> controller.create(user));
    }

    @Test
    void shouldCreateUserWhenBirthdayIsToday() {
        User user = new User();
        user.setEmail("user@mail.ru");
        user.setLogin("login");
        user.setName("Name");
        user.setBirthday(LocalDate.now());

        User createdUser = controller.create(user);

        assertNotNull(createdUser.getId());
    }

    @Test
    void shouldThrowExceptionWhenBirthdayIsNull() {
        User user = new User();
        user.setEmail("user@mail.ru");
        user.setLogin("login");
        user.setName("Name");
        user.setBirthday(null);

        assertThrows(ValidationException.class, () -> controller.create(user));
    }

    @Test
    void shouldUpdateUserWhenDataIsValid() {
        User user = new User();
        user.setEmail("old@mail.ru");
        user.setLogin("oldlogin");
        user.setName("Old Name");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        User createdUser = controller.create(user);

        User updatedUser = new User();
        updatedUser.setId(createdUser.getId());
        updatedUser.setEmail("new@mail.ru");
        updatedUser.setLogin("newlogin");
        updatedUser.setName("New Name");
        updatedUser.setBirthday(LocalDate.of(2001, 1, 1));

        User result = controller.update(updatedUser);

        assertEquals("new@mail.ru", result.getEmail());
        assertEquals("newlogin", result.getLogin());
        assertEquals("New Name", result.getName());
    }

    @Test
    void shouldThrowExceptionWhenUpdateUserIdIsNull() {
        User user = new User();
        user.setId(null);
        user.setEmail("user@mail.ru");
        user.setLogin("login");
        user.setName("Name");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        assertThrows(ValidationException.class, () -> controller.update(user));
    }

    @Test
    void shouldThrowExceptionWhenUpdateUserNotFound() {
        User user = new User();
        user.setId(999L);
        user.setEmail("user@mail.ru");
        user.setLogin("login");
        user.setName("Name");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        assertThrows(NotFoundException.class, () -> controller.update(user));
    }

    @Test
    void shouldUpdateUserWithSameEmail() {
        User user = new User();
        user.setEmail("user@mail.ru");
        user.setLogin("login");
        user.setName("Name");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        User createdUser = controller.create(user);

        User updatedUser = new User();
        updatedUser.setId(createdUser.getId());
        updatedUser.setEmail("user@mail.ru");
        updatedUser.setLogin("newlogin");
        updatedUser.setName("New Name");
        updatedUser.setBirthday(LocalDate.of(2000, 1, 1));

        User result = controller.update(updatedUser);

        assertEquals("user@mail.ru", result.getEmail());
        assertEquals("newlogin", result.getLogin());
    }
}