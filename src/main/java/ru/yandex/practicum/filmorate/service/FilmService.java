package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.LinkedHashSet;

@Slf4j
@Service
public class FilmService {
    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;

    @Autowired
    public FilmService(@Qualifier("filmDbStorage") FilmStorage filmStorage,
                       @Qualifier("userDbStorage") UserStorage userStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
    }

    public Collection<Film> findAll() {
        return filmStorage.findAll();
    }

    public Film findById(long id) {
        return filmStorage.findById(id);
    }

    public Film create(Film film) {
        validateFilm(film, true);
        if (film.getGenres() == null) {
            film.setGenres(new LinkedHashSet<>());
        }
        Film created = filmStorage.create(film);
        log.info("Создан фильм: id={}", created.getId());
        return created;
    }

    public Film update(Film film) {
        if (film.getId() == null) {
            throw new ValidationException("Id должен быть указан");
        }
        filmStorage.findById(film.getId());
        validateFilm(film, false);
        Film updated = filmStorage.update(film);
        log.info("Обновлён фильм: id={}", updated.getId());
        return updated;
    }

    public void addLike(long id, long userId) {
        filmStorage.findById(id);
        userStorage.findById(userId);
        filmStorage.addLike(id, userId);
        log.info("Добавлен лайк: filmId={}, userId={}", id, userId);
    }

    public void removeLike(long id, long userId) {
        filmStorage.findById(id);
        userStorage.findById(userId);
        filmStorage.removeLike(id, userId);
        log.info("Удалён лайк: filmId={}, userId={}", id, userId);
    }

    public List<Film> findPopular(int count) {
        if (count < 0) {
            throw new ValidationException("Количество фильмов не может быть отрицательным");
        }
        return filmStorage.findPopular(count);
    }

    private void validateFilm(Film film, boolean isCreate) {
        if ((isCreate && film.getName() == null)
                || (film.getName() != null && film.getName().isBlank())) {
            log.warn("Ошибка валидации фильма: название пустое");
            throw new ValidationException("Название не может быть пустым");
        }

        if (film.getDescription() != null && film.getDescription().length() > 200) {
            log.warn("Ошибка валидации фильма: описание длиннее 200 символов, length={}",
                    film.getDescription().length());
            throw new ValidationException("Максимальная длина описания — 200 символов");
        }

        if ((isCreate && film.getReleaseDate() == null)
                || (film.getReleaseDate() != null && film.getReleaseDate().isBefore(MIN_RELEASE_DATE))) {
            log.warn("Ошибка валидации фильма: некорректная дата релиза, releaseDate={}",
                    film.getReleaseDate());
            throw new ValidationException("Дата релиза не может быть раньше 28 декабря 1895 года");
        }

        if (isCreate ? film.getDuration() < 1 : film.getDuration() < 0) {
            log.warn("Ошибка валидации фильма: продолжительность должна быть положительной, duration={}",
                    film.getDuration());
            throw new ValidationException("Продолжительность фильма должна быть положительным числом");
        }
    }
}
