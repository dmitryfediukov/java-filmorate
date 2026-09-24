package ru.yandex.practicum.filmorate.storage;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.genre.GenreDbStorage;
import ru.yandex.practicum.filmorate.storage.mpa.MpaDbStorage;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JdbcTest
@AutoConfigureTestDatabase
@Import({UserDbStorage.class, FilmDbStorage.class, GenreDbStorage.class, MpaDbStorage.class})
class StorageJdbcTest {
    @Autowired
    private UserDbStorage users;
    @Autowired
    private FilmDbStorage films;
    @Autowired
    private GenreDbStorage genres;
    @Autowired
    private MpaDbStorage mpaRatings;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void userCrudPersistsFieldsAndRejectsMissingIds() {
        assertThat(users.findAll()).isEmpty();
        User created = users.create(user("one"));
        assertThat(created.getId()).isPositive();
        assertThat(users.findById(created.getId()).getEmail()).isEqualTo("one@mail.ru");

        User patch = new User();
        patch.setId(created.getId());
        patch.setEmail("updated@mail.ru");
        User updated = users.update(patch);
        assertThat(updated.getEmail()).isEqualTo("updated@mail.ru");
        assertThat(updated.getLogin()).isEqualTo("one");
        assertThat(users.findAll()).hasSize(1);

        users.delete(created.getId());
        assertThat(users.findAll()).isEmpty();
        assertThatThrownBy(() -> users.findById(created.getId())).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> users.delete(created.getId())).isInstanceOf(NotFoundException.class);
    }

    @Test
    void friendshipIsDirectedUntilReciprocalRequestAndCanBeRemovedOneWay() {
        long first = users.create(user("first")).getId();
        long second = users.create(user("second")).getId();
        users.addFriend(first, second);
        users.addFriend(first, second);
        assertThat(users.findById(first).getFriends()).containsExactly(second);
        assertThat(users.findById(second).getFriends()).isEmpty();
        assertThat(jdbc.queryForObject("SELECT status FROM friendships_directed", String.class)).isEqualTo("PENDING");

        users.addFriend(second, first);
        assertThat(users.findById(second).getFriends()).containsExactly(first);
        assertThat(jdbc.queryForList("SELECT status FROM friendships_directed ORDER BY user_id", String.class))
                .containsExactly("CONFIRMED", "CONFIRMED");

        users.removeFriend(first, second);
        assertThat(users.findById(first).getFriends()).isEmpty();
        assertThat(users.findById(second).getFriends()).containsExactly(first);
        assertThat(jdbc.queryForObject("SELECT status FROM friendships_directed", String.class)).isEqualTo("PENDING");
        users.removeFriend(second, first);
        users.removeFriend(second, first);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM friendships_directed", Integer.class)).isZero();
    }

    @Test
    void filmCrudPersistsMpaGenresAndPartialUpdates() {
        assertThat(films.findAll()).isEmpty();
        Film incoming = film("First", 1);
        incoming.setGenres(new LinkedHashSet<>(Set.of(genre(1), genre(2))));
        Film created = films.create(incoming);
        assertThat(created.getMpa().getName()).isEqualTo("G");
        assertThat(created.getGenres()).extracting(Genre::getName)
                .containsExactly("Комедия", "Драма");
        assertThat(films.findById(created.getId()).getGenres()).hasSize(2);

        Film patch = new Film();
        patch.setId(created.getId());
        patch.setName("Renamed");
        Film updated = films.update(patch);
        assertThat(updated.getName()).isEqualTo("Renamed");
        assertThat(updated.getMpa().getId()).isEqualTo(1);
        assertThat(updated.getGenres()).hasSize(2);

        patch = new Film();
        patch.setId(created.getId());
        patch.setGenres(new LinkedHashSet<>());
        assertThat(films.update(patch).getGenres()).isEmpty();
        assertThat(films.findAll()).hasSize(1);
        films.delete(created.getId());
        assertThat(films.findAll()).isEmpty();
        assertThatThrownBy(() -> films.findById(created.getId())).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> films.delete(created.getId())).isInstanceOf(NotFoundException.class);
    }

    @Test
    void likesAreUniqueAndPopularityIncludesUnlikedFilms() {
        long first = films.create(film("First", 1)).getId();
        long second = films.create(film("Second", 2)).getId();
        long user = users.create(user("viewer")).getId();
        films.addLike(second, user);
        films.addLike(second, user);
        assertThat(films.findById(second).getLikes()).containsExactly(user);
        assertThat(films.findPopular(1)).extracting(Film::getId).containsExactly(second);
        assertThat(films.findPopular(2)).extracting(Film::getId).containsExactly(second, first);
        films.removeLike(second, user);
        films.removeLike(second, user);
        assertThat(films.findPopular(2)).extracting(Film::getId).containsExactly(first, second);
    }

    @Test
    void catalogsHaveAllExpectedRowsAndMissingIdsReturn404Exceptions() {
        assertThat(genres.findAll()).hasSize(6);
        assertThat(genres.findById(1).getName()).isEqualTo("Комедия");
        assertThat(mpaRatings.findAll()).extracting(Mpa::getName)
                .containsExactly("G", "PG", "PG-13", "R", "NC-17");
        assertThat(mpaRatings.findById(5).getName()).isEqualTo("NC-17");
        assertThatThrownBy(() -> genres.findById(999)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> mpaRatings.findById(999)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void invalidCatalogReferencesDoNotCreateFilms() {
        Film badMpa = film("Bad MPA", 999);
        assertThatThrownBy(() -> films.create(badMpa)).isInstanceOf(NotFoundException.class);
        Film badGenre = film("Bad genre", 1);
        badGenre.setGenres(Set.of(genre(999)));
        assertThatThrownBy(() -> films.create(badGenre)).isInstanceOf(NotFoundException.class);
        assertThat(films.findAll()).isEmpty();
    }

    private User user(String login) {
        User user = new User();
        user.setEmail(login + "@mail.ru");
        user.setLogin(login);
        user.setName(login);
        user.setBirthday(LocalDate.of(2000, 1, 1));
        return user;
    }

    private Film film(String name, int mpaId) {
        Film film = new Film();
        film.setName(name);
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(100);
        Mpa mpa = new Mpa();
        mpa.setId(mpaId);
        film.setMpa(mpa);
        return film;
    }

    private Genre genre(int id) {
        Genre genre = new Genre();
        genre.setId(id);
        return genre;
    }
}
