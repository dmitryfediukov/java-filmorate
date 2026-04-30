package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class FilmControllerTest {

    private final FilmController controller = new FilmController();

    @Test
    void shouldCreateFilmWhenDataIsValid() {
        Film film = new Film();
        film.setName("Avatar");
        film.setDescription("Good film");
        film.setReleaseDate(LocalDate.of(2009, 12, 10));
        film.setDuration(162);

        Film createdFilm = controller.create(film);

        assertNotNull(createdFilm.getId());
        assertEquals("Avatar", createdFilm.getName());
        assertEquals(1, controller.findAll().size());
    }

    @Test
    void shouldThrowExceptionWhenNameIsNull() {
        Film film = new Film();
        film.setName(null);
        film.setDescription("Description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(100);

        assertThrows(ValidationException.class, () -> controller.create(film));
    }

    @Test
    void shouldThrowExceptionWhenNameIsBlank() {
        Film film = new Film();
        film.setName("   ");
        film.setDescription("Description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(100);

        assertThrows(ValidationException.class, () -> controller.create(film));
    }

    @Test
    void shouldThrowExceptionWhenDescriptionIsMoreThan200Characters() {
        Film film = new Film();
        film.setName("Film");
        film.setDescription("a".repeat(201));
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(100);

        assertThrows(ValidationException.class, () -> controller.create(film));
    }

    @Test
    void shouldCreateFilmWhenDescriptionIsExactly200Characters() {
        Film film = new Film();
        film.setName("Film");
        film.setDescription("a".repeat(200));
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(100);

        Film createdFilm = controller.create(film);

        assertNotNull(createdFilm.getId());
    }

    @Test
    void shouldThrowExceptionWhenReleaseDateIsBeforeMinDate() {
        Film film = new Film();
        film.setName("Film");
        film.setDescription("Description");
        film.setReleaseDate(LocalDate.of(1895, 12, 27));
        film.setDuration(100);

        assertThrows(ValidationException.class, () -> controller.create(film));
    }

    @Test
    void shouldCreateFilmWhenReleaseDateIsMinDate() {
        Film film = new Film();
        film.setName("Film");
        film.setDescription("Description");
        film.setReleaseDate(LocalDate.of(1895, 12, 28));
        film.setDuration(100);

        Film createdFilm = controller.create(film);

        assertNotNull(createdFilm.getId());
    }

    @Test
    void shouldThrowExceptionWhenReleaseDateIsNull() {
        Film film = new Film();
        film.setName("Film");
        film.setDescription("Description");
        film.setReleaseDate(null);
        film.setDuration(100);

        assertThrows(ValidationException.class, () -> controller.create(film));
    }

    @Test
    void shouldThrowExceptionWhenDurationIsZero() {
        Film film = new Film();
        film.setName("Film");
        film.setDescription("Description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(0);

        assertThrows(ValidationException.class, () -> controller.create(film));
    }

    @Test
    void shouldThrowExceptionWhenDurationIsNegative() {
        Film film = new Film();
        film.setName("Film");
        film.setDescription("Description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(-1);

        assertThrows(ValidationException.class, () -> controller.create(film));
    }

    @Test
    void shouldCreateFilmWhenDurationIsOne() {
        Film film = new Film();
        film.setName("Film");
        film.setDescription("Description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(1);

        Film createdFilm = controller.create(film);

        assertNotNull(createdFilm.getId());
    }

    @Test
    void shouldUpdateFilmWhenDataIsValid() {
        Film film = new Film();
        film.setName("Old name");
        film.setDescription("Old description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(100);

        Film createdFilm = controller.create(film);

        Film updatedFilm = new Film();
        updatedFilm.setId(createdFilm.getId());
        updatedFilm.setName("New name");
        updatedFilm.setDescription("New description");
        updatedFilm.setReleaseDate(LocalDate.of(2001, 1, 1));
        updatedFilm.setDuration(120);

        Film result = controller.update(updatedFilm);

        assertEquals("New name", result.getName());
        assertEquals("New description", result.getDescription());
        assertEquals(120, result.getDuration());
    }

    @Test
    void shouldThrowExceptionWhenUpdateFilmIdIsNull() {
        Film film = new Film();
        film.setId(null);
        film.setName("Film");
        film.setDescription("Description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(100);

        assertThrows(ValidationException.class, () -> controller.update(film));
    }

    @Test
    void shouldThrowExceptionWhenUpdateFilmNotFound() {
        Film film = new Film();
        film.setId(999L);
        film.setName("Film");
        film.setDescription("Description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(100);

        assertThrows(NotFoundException.class, () -> controller.update(film));
    }
}