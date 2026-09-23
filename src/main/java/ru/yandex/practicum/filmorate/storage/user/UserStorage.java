package ru.yandex.practicum.filmorate.storage.user;

import ru.yandex.practicum.filmorate.model.User;

import java.util.Collection;

public interface UserStorage {
    Collection<User> findAll();

    User findById(long id);

    User create(User user);

    User update(User user);

    void delete(long id);

    void addFriend(long userId, long friendId);

    void removeFriend(long userId, long friendId);
}
