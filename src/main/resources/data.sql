
-- Test users with BCrypt encrypted passwords
-- Password for all users: Test1234!
INSERT INTO users (email, password, nickname, role, created_at, updated_at) 
VALUES 
('test@gmail.com', '$2a$10$YJhXn8Zl2JH3L6oG2zrUkOxF9Kq3EYwFQpU5Xq9xNnXC9FVzZGzZS', 'Test User', 'USER', NOW(), NOW()),
('admin@gmail.com', '$2a$10$YJhXn8Zl2JH3L6oG2zrUkOxF9Kq3EYwFQpU5Xq9xNnXC9FVzZGzZS', 'Admin User', 'ADMIN', NOW(), NOW())
ON CONFLICT (email) DO NOTHING;

-- Reset sequence to avoid ID conflicts
SELECT setval('users_id_seq', (SELECT MAX(id) FROM users) + 1, false);

