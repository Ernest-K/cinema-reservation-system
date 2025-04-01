CREATE SCHEMA IF NOT EXISTS user_service;
CREATE SCHEMA IF NOT EXISTS reservation_service;
CREATE SCHEMA IF NOT EXISTS payment_service;
CREATE SCHEMA IF NOT EXISTS notification_service;

-- Przejście do schematu user_service i utworzenie tabel
SET search_path TO user_service;

CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100),
    email VARCHAR(100) UNIQUE
);

INSERT INTO users (name, email) VALUES
('Jan Kowalski', 'jan.kowalski@example.com'),
('Anna Nowak', 'anna.nowak@example.com');

-- Przejście do schematu reservation_service i utworzenie tabel
SET search_path TO reservation_service;

CREATE TABLE IF NOT EXISTS reservations (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL,
    movie VARCHAR(255) NOT NULL,
    seat VARCHAR(10) NOT NULL,
    reservation_date TIMESTAMP DEFAULT NOW(),
    FOREIGN KEY (user_id) REFERENCES user_service.users(id) ON DELETE CASCADE
);

INSERT INTO reservations (user_id, movie, seat) VALUES
(1, 'Incepcja', 'A1'),
(2, 'Matrix', 'B3');