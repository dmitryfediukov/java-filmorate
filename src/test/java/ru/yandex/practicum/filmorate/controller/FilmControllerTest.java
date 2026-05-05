package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class FilmControllerTest {

    private FilmController controller;
    private Film film;

    @BeforeEach
    void setUp() {
        controller = new FilmController();

        film = new Film();
        film.setName("Avatar");
        film.setDescription("Good film");
        film.setReleaseDate(LocalDate.of(2009, 12, 10));
        film.setDuration(162);
    }

    @Test
    void shouldCreateFilm_whenValid() {
        Film created = controller.create(film);

        assertNotNull(created.getId());
        assertEquals("Avatar", created.getName());
    }

    @Test
    void shouldThrow_whenNameBlank() {
        film.setName(" ");

        assertThrows(ValidationException.class, () -> controller.create(film));
    }

    @Test
    void shouldThrow_whenDescriptionTooLong() {
        film.setDescription("a".repeat(201));

        assertThrows(ValidationException.class, () -> controller.create(film));
    }

    @Test
    void shouldThrow_whenReleaseDateBeforeMin() {
        film.setReleaseDate(LocalDate.of(1895, 12, 27));

        assertThrows(ValidationException.class, () -> controller.create(film));
    }

    @Test
    void shouldThrow_whenDurationZero() {
        film.setDuration(0);

        assertThrows(ValidationException.class, () -> controller.create(film));
    }

    // --- UPDATE (ВАЖНО: теперь частичный) ---

    @Test
    void shouldUpdateOnlyName() {
        Film created = controller.create(film);

        Film update = new Film();
        update.setId(created.getId());
        update.setName("Avatar 2");

        Film result = controller.update(update);

        assertEquals("Avatar 2", result.getName());
        assertEquals(created.getDescription(), result.getDescription());
    }

    @Test
    void shouldNotOverwriteWithNulls() {
        Film created = controller.create(film);

        Film update = new Film();
        update.setId(created.getId());
        // ничего больше не передаем

        Film result = controller.update(update);

        assertEquals(created.getName(), result.getName());
        assertEquals(created.getDescription(), result.getDescription());
    }

    @Test
    void shouldThrow_whenUpdateIdNull() {
        Film update = new Film();

        assertThrows(ValidationException.class, () -> controller.update(update));
    }

    @Test
    void shouldThrow_whenFilmNotFound() {
        Film update = new Film();
        update.setId(999L);

        assertThrows(NotFoundException.class, () -> controller.update(update));
    }

    @Test
    void shouldThrow_whenInvalidUpdateField() {
        Film created = controller.create(film);

        Film update = new Film();
        update.setId(created.getId());
        update.setName(" "); // невалидно

        assertThrows(ValidationException.class, () -> controller.update(update));
    }
}