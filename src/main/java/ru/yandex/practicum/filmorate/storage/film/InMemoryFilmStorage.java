package ru.yandex.practicum.filmorate.storage.film;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class InMemoryFilmStorage implements FilmStorage {
    private final Map<Long, Film> films = new LinkedHashMap<>();
    private long nextId = 1;

    @Override
    public Collection<Film> findAll() {
        return new ArrayList<>(films.values());
    }

    @Override
    public Film findById(long id) {
        Film film = films.get(id);
        if (film == null) {
            throw new NotFoundException("Фильм с id = " + id + " не найден");
        }
        return film;
    }

    @Override
    public Film create(Film film) {
        film.setId(nextId++);
        films.put(film.getId(), film);
        return film;
    }

    @Override
    public Film update(Film film) {
        Film saved = findById(film.getId());
        if (film.getName() != null) {
            saved.setName(film.getName());
        }
        if (film.getDescription() != null) {
            saved.setDescription(film.getDescription());
        }
        if (film.getReleaseDate() != null) {
            saved.setReleaseDate(film.getReleaseDate());
        }
        if (film.getDuration() > 0) {
            saved.setDuration(film.getDuration());
        }
        return saved;
    }

    @Override
    public void delete(long id) {
        findById(id);
        films.remove(id);
    }
}
