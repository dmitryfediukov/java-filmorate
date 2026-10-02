package ru.yandex.practicum.filmorate.storage.film;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Component
@Profile("memory")
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
        if (film.getGenres() == null) {
            film.setGenres(new LinkedHashSet<>());
        }
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
        if (film.getMpa() != null) {
            saved.setMpa(film.getMpa());
        }
        if (film.getGenres() != null) {
            saved.setGenres(film.getGenres());
        }
        return saved;
    }

    @Override
    public void delete(long id) {
        findById(id);
        films.remove(id);
    }

    @Override
    public void addLike(long filmId, long userId) {
        findById(filmId).getLikes().add(userId);
    }

    @Override
    public void removeLike(long filmId, long userId) {
        findById(filmId).getLikes().remove(userId);
    }

    @Override
    public List<Film> findPopular(int count) {
        return films.values().stream()
                .sorted(Comparator.<Film>comparingInt(film -> film.getLikes().size())
                        .reversed().thenComparing(Film::getId))
                .limit(count)
                .toList();
    }
}
