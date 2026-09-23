package ru.yandex.practicum.filmorate.storage.genre;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Genre;

import java.util.List;

@Repository
public class GenreDbStorage {
    private final JdbcTemplate jdbc;

    public GenreDbStorage(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Genre> findAll() {
        return jdbc.query("SELECT id, name FROM genres ORDER BY id", (rs, rowNum) -> mapGenre(rs));
    }

    public Genre findById(int id) {
        try {
            return jdbc.queryForObject("SELECT id, name FROM genres WHERE id = ?",
                    (rs, rowNum) -> mapGenre(rs), id);
        } catch (EmptyResultDataAccessException exception) {
            throw new NotFoundException("Жанр с id = " + id + " не найден");
        }
    }

    private Genre mapGenre(java.sql.ResultSet rs) throws java.sql.SQLException {
        Genre genre = new Genre();
        genre.setId(rs.getInt("id"));
        genre.setName(rs.getString("name"));
        return genre;
    }
}
