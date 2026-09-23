MERGE INTO mpa_ratings (id, name) KEY(id) VALUES (1, '0+');
MERGE INTO mpa_ratings (id, name) KEY(id) VALUES (2, '6+');
MERGE INTO mpa_ratings (id, name) KEY(id) VALUES (3, '12+');
MERGE INTO mpa_ratings (id, name) KEY(id) VALUES (4, '16+');
MERGE INTO mpa_ratings (id, name) KEY(id) VALUES (5, '18+');

MERGE INTO genres (id, name) KEY(id) VALUES (1, 'Комедия');
MERGE INTO genres (id, name) KEY(id) VALUES (2, 'Драма');
MERGE INTO genres (id, name) KEY(id) VALUES (3, 'Мультфильм');
MERGE INTO genres (id, name) KEY(id) VALUES (4, 'Триллер');
MERGE INTO genres (id, name) KEY(id) VALUES (5, 'Документальный');
MERGE INTO genres (id, name) KEY(id) VALUES (6, 'Боевик');
