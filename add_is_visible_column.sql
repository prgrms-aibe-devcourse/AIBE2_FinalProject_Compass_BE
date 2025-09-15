-- Migration script to add is_visible column to chat_threads table
-- This script handles existing data by setting default values

-- Step 1: Add the column as nullable first
ALTER TABLE chat_threads 
ADD COLUMN IF NOT EXISTS is_visible BOOLEAN;

-- Step 2: Update all existing rows to have is_visible = true
UPDATE chat_threads 
SET is_visible = true 
WHERE is_visible IS NULL;

-- Step 3: Now we can make it NOT NULL if needed (optional)
-- ALTER TABLE chat_threads 
-- ALTER COLUMN is_visible SET NOT NULL;

-- Verify the change
SELECT id, user_id, title, is_visible, created_at 
FROM chat_threads 
LIMIT 5;