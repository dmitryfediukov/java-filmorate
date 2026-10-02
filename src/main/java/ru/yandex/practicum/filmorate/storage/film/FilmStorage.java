package ru.yandex.practicum.filmorate.storage.film;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.Collection;
import java.util.List;

public interface FilmStorage {
    Collection<Film> findAll();

    Film findById(long id);

    Film create(Film film);

    Film update(Film film);

    void delete(long id);

    void addLike(long filmId, long userId);

    void removeLike(long filmId, long userId);

    List<Film> findPopular(int count);
}
