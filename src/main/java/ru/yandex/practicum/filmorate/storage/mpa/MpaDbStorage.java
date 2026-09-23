package ru.yandex.practicum.filmorate.storage.mpa;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.util.List;

@Repository
public class MpaDbStorage {
    private final JdbcTemplate jdbc;

    public MpaDbStorage(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Mpa> findAll() {
        return jdbc.query("SELECT id, name FROM mpa_ratings ORDER BY id", (rs, rowNum) -> mapMpa(rs));
    }

    public Mpa findById(int id) {
        try {
            return jdbc.queryForObject("SELECT id, name FROM mpa_ratings WHERE id = ?",
                    (rs, rowNum) -> mapMpa(rs), id);
        } catch (EmptyResultDataAccessException exception) {
            throw new NotFoundException("Рейтинг МРА с id = " + id + " не найден");
        }
    }

    private Mpa mapMpa(java.sql.ResultSet rs) throws java.sql.SQLException {
        Mpa mpa = new Mpa();
        mpa.setId(rs.getInt("id"));
        mpa.setName(rs.getString("name"));
        return mpa;
    }
}
