package ru.yandex.practicum.filmorate.storage.film;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.genre.GenreDbStorage;
import ru.yandex.practicum.filmorate.storage.mpa.MpaDbStorage;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Repository
public class FilmDbStorage implements FilmStorage {
    private static final String SELECT_ALL_FILMS =
            "SELECT id, name, description, release_date, duration, mpa_id FROM films ORDER BY id";
    private static final String SELECT_FILM_BY_ID =
            "SELECT id, name, description, release_date, duration, mpa_id FROM films WHERE id = ?";
    private static final String INSERT_FILM =
            "INSERT INTO films (name, description, release_date, duration, mpa_id) VALUES (?, ?, ?, ?, ?)";
    private static final String UPDATE_FILM =
            "UPDATE films SET name = ?, description = ?, release_date = ?, duration = ?, mpa_id = ? WHERE id = ?";
    private static final String DELETE_FILM = "DELETE FROM films WHERE id = ?";
    private static final String UPSERT_LIKE =
            "MERGE INTO likes (film_id, user_id) KEY(film_id, user_id) VALUES (?, ?)";
    private static final String DELETE_LIKE = "DELETE FROM likes WHERE film_id = ? AND user_id = ?";
    private static final String SELECT_POPULAR_FILMS =
            "SELECT f.id, f.name, f.description, f.release_date, f.duration, f.mpa_id"
                    + " FROM films f LEFT JOIN likes l ON l.film_id = f.id"
                    + " GROUP BY f.id, f.name, f.description, f.release_date, f.duration, f.mpa_id"
                    + " ORDER BY COUNT(l.user_id) DESC, f.id ASC LIMIT ?";
    private static final String SELECT_FILM_GENRES =
            "SELECT g.id, g.name FROM genres g JOIN film_genres fg ON fg.genre_id = g.id"
                    + " WHERE fg.film_id = ? ORDER BY g.id";
    private static final String SELECT_FILM_LIKES = "SELECT user_id FROM likes WHERE film_id = ? ORDER BY user_id";
    private static final String DELETE_FILM_GENRES = "DELETE FROM film_genres WHERE film_id = ?";
    private static final String INSERT_FILM_GENRE = "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";
    private final JdbcTemplate jdbc;
    private final GenreDbStorage genres;
    private final MpaDbStorage mpaRatings;

    public FilmDbStorage(JdbcTemplate jdbc, GenreDbStorage genres, MpaDbStorage mpaRatings) {
        this.jdbc = jdbc;
        this.genres = genres;
        this.mpaRatings = mpaRatings;
    }

    @Override
    public Collection<Film> findAll() {
        return jdbc.query(SELECT_ALL_FILMS,
                (rs, rowNum) -> mapFilm(rs));
    }

    @Override
    public Film findById(long id) {
        List<Film> found = jdbc.query(
                SELECT_FILM_BY_ID,
                (rs, rowNum) -> mapFilm(rs), id);
        if (found.isEmpty()) {
            throw new NotFoundException("Фильм с id = " + id + " не найден");
        }
        return found.getFirst();
    }

    @Override
    @Transactional
    public Film create(Film film) {
        Mpa mpa = resolveMpa(film.getMpa());
        Set<Genre> resolvedGenres = resolveGenres(film.getGenres());
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(INSERT_FILM, new String[]{"ID"});
            statement.setString(1, film.getName());
            statement.setString(2, film.getDescription());
            statement.setDate(3, Date.valueOf(film.getReleaseDate()));
            statement.setInt(4, film.getDuration());
            if (mpa == null) {
                statement.setNull(5, Types.INTEGER);
            } else {
                statement.setInt(5, mpa.getId());
            }
            return statement;
        }, keys);
        long id = keys.getKey().longValue();
        replaceGenres(id, resolvedGenres);
        return findById(id);
    }

    @Override
    @Transactional
    public Film update(Film update) {
        Film saved = findById(update.getId());
        String name = update.getName() == null ? saved.getName() : update.getName();
        String description = update.getDescription() == null ? saved.getDescription() : update.getDescription();
        LocalDate releaseDate = update.getReleaseDate() == null
                ? saved.getReleaseDate() : update.getReleaseDate();
        int duration = update.getDuration() == 0 ? saved.getDuration() : update.getDuration();
        Mpa mpa = update.getMpa() == null ? saved.getMpa() : resolveMpa(update.getMpa());
        Set<Genre> resolvedGenres = update.getGenres() == null ? null : resolveGenres(update.getGenres());
        jdbc.update(UPDATE_FILM,
                name, description, Date.valueOf(releaseDate), duration, mpa == null ? null : mpa.getId(),
                update.getId());
        if (resolvedGenres != null) {
            replaceGenres(update.getId(), resolvedGenres);
        }
        return findById(update.getId());
    }

    @Override
    public void delete(long id) {
        findById(id);
        jdbc.update(DELETE_FILM, id);
    }

    @Override
    public void addLike(long filmId, long userId) {
        jdbc.update(UPSERT_LIKE, filmId, userId);
    }

    @Override
    public void removeLike(long filmId, long userId) {
        jdbc.update(DELETE_LIKE, filmId, userId);
    }

    @Override
    public List<Film> findPopular(int count) {
        return jdbc.query(SELECT_POPULAR_FILMS,
                (rs, rowNum) -> mapFilm(rs), count);
    }

    private Film mapFilm(ResultSet rs) throws SQLException {
        Film film = new Film();
        long id = rs.getLong("id");
        film.setId(id);
        film.setName(rs.getString("name"));
        film.setDescription(rs.getString("description"));
        film.setReleaseDate(rs.getDate("release_date").toLocalDate());
        film.setDuration(rs.getInt("duration"));
        Integer mpaId = rs.getObject("mpa_id", Integer.class);
        if (mpaId != null) {
            film.setMpa(mpaRatings.findById(mpaId));
        }
        film.setGenres(new LinkedHashSet<>(jdbc.query(
                SELECT_FILM_GENRES,
                (genreRs, rowNum) -> {
                    Genre genre = new Genre();
                    genre.setId(genreRs.getInt("id"));
                    genre.setName(genreRs.getString("name"));
                    return genre;
                }, id)));
        film.getLikes().addAll(jdbc.queryForList(SELECT_FILM_LIKES, Long.class, id));
        return film;
    }

    private Mpa resolveMpa(Mpa mpa) {
        if (mpa == null) {
            return null;
        }
        if (mpa.getId() == null) {
            throw new ValidationException("У рейтинга МРА должен быть id");
        }
        return mpaRatings.findById(mpa.getId());
    }

    private Set<Genre> resolveGenres(Set<Genre> incoming) {
        Set<Genre> resolved = new LinkedHashSet<>();
        if (incoming != null) {
            for (Genre genre : incoming) {
                if (genre == null || genre.getId() == null) {
                    throw new ValidationException("У жанра должен быть id");
                }
                resolved.add(genres.findById(genre.getId()));
            }
        }
        return resolved;
    }

    private void replaceGenres(long filmId, Set<Genre> resolved) {
        jdbc.update(DELETE_FILM_GENRES, filmId);
        for (Genre genre : resolved) {
            jdbc.update(INSERT_FILM_GENRE, filmId, genre.getId());
        }
    }
}
