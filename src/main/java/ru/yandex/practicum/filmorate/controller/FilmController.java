package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/films")
public class FilmController {
    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);

    private final Map<Long, Film> films = new HashMap<>();

    @GetMapping
    public Collection<Film> findAll() {
        return films.values();
    }

    @PostMapping
    public Film create(@RequestBody Film film) {
        validateFilmForCreate(film);

        film.setId(getNextId());
        films.put(film.getId(), film);

        log.info("Создан фильм: id={}, name={}", film.getId(), film.getName());

        return film;
    }

    @PutMapping
    public Film update(@RequestBody Film newFilm) {
        if (newFilm.getId() == null) {
            log.warn("Ошибка обновления фильма: id не указан");
            throw new ValidationException("Id должен быть указан");
        }

        if (!films.containsKey(newFilm.getId())) {
            log.warn("Ошибка обновления фильма: фильм с id={} не найден", newFilm.getId());
            throw new NotFoundException("Фильм с id = " + newFilm.getId() + " не найден");
        }

        validateFilmForUpdate(newFilm);

        Film oldFilm = films.get(newFilm.getId());

        if (newFilm.getName() != null) {
            oldFilm.setName(newFilm.getName());
        }

        if (newFilm.getDescription() != null) {
            oldFilm.setDescription(newFilm.getDescription());
        }

        if (newFilm.getReleaseDate() != null) {
            oldFilm.setReleaseDate(newFilm.getReleaseDate());
        }

        if (newFilm.getDuration() > 0) {
            oldFilm.setDuration(newFilm.getDuration());
        }

        log.info("Обновлён фильм: id={}, name={}", oldFilm.getId(), oldFilm.getName());

        return oldFilm;
    }

    private void validateFilmForCreate(Film film) {
        if (film.getName() == null || film.getName().isBlank()) {
            log.warn("Ошибка валидации фильма: название пустое");
            throw new ValidationException("Название не может быть пустым");
        }

        if (film.getDescription() != null && film.getDescription().length() > 200) {
            log.warn("Ошибка валидации фильма: описание длиннее 200 символов, length={}",
                    film.getDescription().length());
            throw new ValidationException("Максимальная длина описания — 200 символов");
        }

        if (film.getReleaseDate() == null || film.getReleaseDate().isBefore(MIN_RELEASE_DATE)) {
            log.warn("Ошибка валидации фильма: некорректная дата релиза, releaseDate={}",
                    film.getReleaseDate());
            throw new ValidationException("Дата релиза не может быть раньше 28 декабря 1895 года");
        }

        if (film.getDuration() < 1) {
            log.warn("Ошибка валидации фильма: продолжительность должна быть положительной, duration={}",
                    film.getDuration());
            throw new ValidationException("Продолжительность фильма должна быть положительным числом");
        }
    }

    private void validateFilmForUpdate(Film film) {
        if (film.getName() != null && film.getName().isBlank()) {
            log.warn("Ошибка валидации фильма: название пустое");
            throw new ValidationException("Название не может быть пустым");
        }

        if (film.getDescription() != null && film.getDescription().length() > 200) {
            log.warn("Ошибка валидации фильма: описание длиннее 200 символов, length={}",
                    film.getDescription().length());
            throw new ValidationException("Максимальная длина описания — 200 символов");
        }

        if (film.getReleaseDate() != null && film.getReleaseDate().isBefore(MIN_RELEASE_DATE)) {
            log.warn("Ошибка валидации фильма: некорректная дата релиза, releaseDate={}",
                    film.getReleaseDate());
            throw new ValidationException("Дата релиза не может быть раньше 28 декабря 1895 года");
        }

        if (film.getDuration() != 0 && film.getDuration() < 1) {
            log.warn("Ошибка валидации фильма: продолжительность должна быть положительной, duration={}",
                    film.getDuration());
            throw new ValidationException("Продолжительность фильма должна быть положительным числом");
        }
    }

    // вспомогательный метод для генерации идентификатора нового фильма
    private long getNextId() {
        long currentMaxId = films.keySet()
                .stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0);
        return ++currentMaxId;
    }
}
