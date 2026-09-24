package ru.yandex.practicum.filmorate.storage.genre;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Genre;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class GenreDbStorage {
    private static final String SELECT_ALL_GENRES = "SELECT id, name FROM genres ORDER BY id";
    private static final String SELECT_GENRE_BY_ID = "SELECT id, name FROM genres WHERE id = ?";
    private final JdbcTemplate jdbc;

    public GenreDbStorage(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Genre> findAll() {
        return jdbc.query(SELECT_ALL_GENRES, (rs, rowNum) -> mapGenre(rs));
    }

    public Genre findById(int id) {
        try {
            return jdbc.queryForObject(SELECT_GENRE_BY_ID,
                    (rs, rowNum) -> mapGenre(rs), id);
        } catch (EmptyResultDataAccessException exception) {
            throw new NotFoundException("Жанр с id = " + id + " не найден");
        }
    }

    private Genre mapGenre(ResultSet rs) throws SQLException {
        Genre genre = new Genre();
        genre.setId(rs.getInt("id"));
        genre.setName(rs.getString("name"));
        return genre;
    }
}
