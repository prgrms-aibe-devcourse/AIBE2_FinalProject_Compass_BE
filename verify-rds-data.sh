#!/bin/bash

# RDS 데이터 확인 스크립트

echo "=== AWS RDS Compass DB 데이터 확인 ==="
echo

# PostgreSQL 연결 정보
DB_HOST="compass-db.ch6mum0221cb.ap-northeast-2.rds.amazonaws.com"
DB_PORT="5432"
DB_NAME="compass"
DB_USER="compass"
export PGPASSWORD="compass1004!"

echo "1. ChatThread 테이블 확인..."
psql -h $DB_HOST -p $DB_PORT -d $DB_NAME -U $DB_USER -c "SELECT id, title, current_phase, created_at FROM chat_threads ORDER BY created_at DESC LIMIT 5;"

echo
echo "2. ChatMessage 테이블 확인..."
psql -h $DB_HOST -p $DB_PORT -d $DB_NAME -U $DB_USER -c "SELECT id, thread_id, role, LEFT(content, 50) as content_preview, timestamp FROM chat_messages ORDER BY timestamp DESC LIMIT 10;"

echo
echo "3. 총 레코드 수..."
psql -h $DB_HOST -p $DB_PORT -d $DB_NAME -U $DB_USER -c "SELECT 'chat_threads' as table_name, COUNT(*) as count FROM chat_threads UNION ALL SELECT 'chat_messages', COUNT(*) FROM chat_messages;"

echo
echo "4. 최근 생성된 Thread ID 확인..."
psql -h $DB_HOST -p $DB_PORT -d $DB_NAME -U $DB_USER -c "SELECT id, created_at FROM chat_threads WHERE created_at > NOW() - INTERVAL '1 hour' ORDER BY created_at DESC;"

echo
echo "=== 확인 완료 ==="