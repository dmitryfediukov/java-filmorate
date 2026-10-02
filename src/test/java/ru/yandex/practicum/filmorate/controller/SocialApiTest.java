package ru.yandex.practicum.filmorate.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.storage.film.InMemoryFilmStorage;
import ru.yandex.practicum.filmorate.storage.user.InMemoryUserStorage;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SocialApiTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        InMemoryUserStorage users = new InMemoryUserStorage();
        mvc = MockMvcBuilders.standaloneSetup(
                        new UserController(new UserService(users)),
                        new FilmController(new FilmService(new InMemoryFilmStorage(), users)))
                .setControllerAdvice(new ErrorHandler())
                .build();
    }

    @Test
    void friendshipIsOneSidedUniqueAndCanBeRemoved() throws Exception {
        long first = createUser("first");
        long second = createUser("second");
        mvc.perform(put("/users/{id}/friends/{friendId}", first, second)).andExpect(status().isOk());
        mvc.perform(put("/users/{id}/friends/{friendId}", first, second)).andExpect(status().isOk());
        mvc.perform(get("/users/{id}/friends", first))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(second));
        mvc.perform(get("/users/{id}/friends", second))
                .andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(delete("/users/{id}/friends/{friendId}", first, second)).andExpect(status().isOk());
        mvc.perform(delete("/users/{id}/friends/{friendId}", first, second)).andExpect(status().isOk());
        mvc.perform(get("/users/{id}/friends", first)).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/users/{id}/friends", second)).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void commonFriendsAreTheIntersection() throws Exception {
        long first = createUser("first");
        long second = createUser("second");
        long common = createUser("common");
        long extra = createUser("extra");
        mvc.perform(get("/users/{id}/friends/common/{otherId}", first, second))
                .andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(put("/users/{id}/friends/{friendId}", first, common)).andExpect(status().isOk());
        mvc.perform(put("/users/{id}/friends/{friendId}", second, common)).andExpect(status().isOk());
        mvc.perform(put("/users/{id}/friends/{friendId}", first, extra)).andExpect(status().isOk());
        mvc.perform(get("/users/{id}/friends/common/{otherId}", first, second))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(common));
    }

    @Test
    void likesAreUniqueAndDetermineRanking() throws Exception {
        long firstUser = createUser("first");
        long secondUser = createUser("second");
        long firstFilm = createFilm("first");
        long secondFilm = createFilm("second");
        mvc.perform(put("/films/{id}/like/{userId}", firstFilm, firstUser)).andExpect(status().isOk());
        mvc.perform(put("/films/{id}/like/{userId}", secondFilm, firstUser)).andExpect(status().isOk());
        mvc.perform(put("/films/{id}/like/{userId}", secondFilm, secondUser)).andExpect(status().isOk());
        mvc.perform(put("/films/{id}/like/{userId}", secondFilm, secondUser)).andExpect(status().isOk());
        mvc.perform(get("/films/{id}", secondFilm))
                .andExpect(status().isOk()).andExpect(jsonPath("$.likes", hasSize(2)));
        mvc.perform(get("/films/popular").param("count", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(secondFilm));
        mvc.perform(delete("/films/{id}/like/{userId}", secondFilm, firstUser)).andExpect(status().isOk());
        mvc.perform(delete("/films/{id}/like/{userId}", secondFilm, secondUser)).andExpect(status().isOk());
        mvc.perform(delete("/films/{id}/like/{userId}", secondFilm, secondUser)).andExpect(status().isOk());
        mvc.perform(get("/films/popular")).andExpect(jsonPath("$[0].id").value(firstFilm));
    }

    @Test
    void popularDefaultsToTenAndHandlesLimits() throws Exception {
        mvc.perform(get("/films/popular")).andExpect(jsonPath("$", hasSize(0)));
        for (int i = 0; i < 12; i++) {
            createFilm("film" + i);
        }
        mvc.perform(get("/films/popular")).andExpect(jsonPath("$", hasSize(10)));
        mvc.perform(get("/films/popular").param("count", "20")).andExpect(jsonPath("$", hasSize(12)));
        mvc.perform(get("/films/popular").param("count", "0")).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/films/popular").param("count", "-1")).andExpect(status().isBadRequest());
        mvc.perform(get("/films/popular").param("count", "abc")).andExpect(status().isBadRequest());
    }

    @Test
    void unknownObjectsReturnNotFoundWithoutChangingRelations() throws Exception {
        long user = createUser("first");
        long film = createFilm("first");
        mvc.perform(get("/users/999")).andExpect(status().isNotFound());
        mvc.perform(get("/films/999")).andExpect(status().isNotFound());
        mvc.perform(get("/users/999/friends")).andExpect(status().isNotFound());
        mvc.perform(put("/users/{id}/friends/999", user)).andExpect(status().isNotFound());
        mvc.perform(put("/users/999/friends/{id}", user)).andExpect(status().isNotFound());
        mvc.perform(delete("/users/{id}/friends/999", user)).andExpect(status().isNotFound());
        mvc.perform(get("/users/{id}/friends/common/999", user)).andExpect(status().isNotFound());
        mvc.perform(get("/users/999/friends/common/{id}", user)).andExpect(status().isNotFound());
        mvc.perform(put("/films/{id}/like/999", film)).andExpect(status().isNotFound());
        mvc.perform(put("/films/999/like/{id}", user)).andExpect(status().isNotFound());
        mvc.perform(delete("/films/{id}/like/999", film)).andExpect(status().isNotFound());
        mvc.perform(delete("/films/999/like/{id}", user)).andExpect(status().isNotFound());
        mvc.perform(get("/users/{id}/friends", user)).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/films/{id}", film)).andExpect(jsonPath("$.likes", hasSize(0)));
    }

    @Test
    void updatesPreserveFriendsAndLikes() throws Exception {
        long first = createUser("first");
        long second = createUser("second");
        long film = createFilm("film");
        mvc.perform(put("/users/{id}/friends/{friendId}", first, second)).andExpect(status().isOk());
        mvc.perform(put("/films/{id}/like/{userId}", film, first)).andExpect(status().isOk());
        mvc.perform(put("/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":" + first + ",\"name\":\"changed\",\"friends\":[]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.friends", hasSize(1)));
        mvc.perform(put("/films").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":" + film + ",\"name\":\"changed\",\"likes\":[]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.likes", hasSize(1)));
        mvc.perform(get("/users/{id}", first))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("changed"));
    }

    @Test
    void cannotInjectFriendsOrLikesThroughJson() throws Exception {
        mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@mail.ru\",\"login\":\"test\","
                                + "\"birthday\":\"2000-01-01\",\"friends\":[999]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.friends", hasSize(0)));
        mvc.perform(post("/films").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"test\",\"releaseDate\":\"2000-01-01\","
                                + "\"duration\":100,\"likes\":[999]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.likes", hasSize(0)));
    }

    @Test
    void invalidRequestsReturnBadRequest() throws Exception {
        long user = createUser("first");
        mvc.perform(put("/users/{id}/friends/{id}", user, user)).andExpect(status().isBadRequest());
        mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").exists());
        mvc.perform(post("/films").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/films").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/users/abc")).andExpect(status().isBadRequest());
        mvc.perform(put("/users").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/films").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/users").contentType(MediaType.APPLICATION_JSON).content("{\"id\":999}"))
                .andExpect(status().isNotFound());
        mvc.perform(put("/films").contentType(MediaType.APPLICATION_JSON).content("{\"id\":999}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void unexpectedFailureReturns500WithoutInternalDetails() throws Exception {
        UserService service = mock(UserService.class);
        when(service.findById(1L)).thenThrow(new IllegalStateException("internal detail"));
        MockMvc failingMvc = MockMvcBuilders.standaloneSetup(new UserController(service))
                .setControllerAdvice(new ErrorHandler()).build();
        String body = failingMvc.perform(get("/users/1"))
                .andExpect(status().isInternalServerError())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertEquals("Внутренняя ошибка сервера", mapper.readTree(body).get("error").asText());
    }

    private long createUser(String login) throws Exception {
        String body = mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + login + "@mail.ru\",\"login\":\"" + login
                                + "\",\"birthday\":\"2000-01-01\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(body).get("id").asLong();
    }

    private long createFilm(String name) throws Exception {
        String body = mvc.perform(post("/films").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"releaseDate\":\"2000-01-01\",\"duration\":100}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(body).get("id").asLong();
    }
}
