package ru.yandex.practicum.filmorate.exception;

public class DublicatedDataException extends RuntimeException {
    public DublicatedDataException(String message) {
        super(message);
    }
}
