package ru.yandex.practicum.filmorate.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.genre.GenreDbStorage;

import java.util.List;

@RestController
@RequestMapping("/genres")
public class GenreController {
    private final GenreDbStorage genreStorage;

    public GenreController(GenreDbStorage genreStorage) {
        this.genreStorage = genreStorage;
    }

    @GetMapping
    public List<Genre> findAll() {
        return genreStorage.findAll();
    }

    @GetMapping("/{id}")
    public Genre findById(@PathVariable int id) {
        return genreStorage.findById(id);
    }
}
