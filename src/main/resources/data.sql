-- Test users with BCrypt encrypted passwords
-- Password for all users: Test1234!
INSERT INTO users (id, email, password, nickname, role, created_at, updated_at) 
VALUES 
(1, 'test@gmail.com', '$2a$10$YJhXn8Zl2JH3L6oG2zrUkOxF9Kq3EYwFQpU5Xq9xNnXC9FVzZGzZS', 'Test User', 'USER', NOW(), NOW()),
(2, 'admin@gmail.com', '$2a$10$YJhXn8Zl2JH3L6oG2zrUkOxF9Kq3EYwFQpU5Xq9xNnXC9FVzZGzZS', 'Admin User', 'ADMIN', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Reset sequence to avoid ID conflicts
SELECT setval('users_id_seq', (SELECT MAX(id) FROM users) + 1, false);