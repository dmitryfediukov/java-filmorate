package ru.yandex.practicum.filmorate.storage.mpa;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class MpaDbStorage {
    private static final String SELECT_ALL_MPA = "SELECT id, name FROM mpa_ratings ORDER BY id";
    private static final String SELECT_MPA_BY_ID = "SELECT id, name FROM mpa_ratings WHERE id = ?";
    private final JdbcTemplate jdbc;

    public MpaDbStorage(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Mpa> findAll() {
        return jdbc.query(SELECT_ALL_MPA, (rs, rowNum) -> mapMpa(rs));
    }

    public Mpa findById(int id) {
        try {
            return jdbc.queryForObject(SELECT_MPA_BY_ID,
                    (rs, rowNum) -> mapMpa(rs), id);
        } catch (EmptyResultDataAccessException exception) {
            throw new NotFoundException("Рейтинг МРА с id = " + id + " не найден");
        }
    }

    private Mpa mapMpa(ResultSet rs) throws SQLException {
        Mpa mpa = new Mpa();
        mpa.setId(rs.getInt("id"));
        mpa.setName(rs.getString("name"));
        return mpa;
    }
}
