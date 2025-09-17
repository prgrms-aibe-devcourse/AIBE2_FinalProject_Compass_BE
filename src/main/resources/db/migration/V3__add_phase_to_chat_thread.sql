-- Phase 상태 추가
ALTER TABLE chat_threads
ADD COLUMN current_phase VARCHAR(50) DEFAULT 'INITIALIZATION',
ADD COLUMN phase_updated_at TIMESTAMP;

-- 기존 데이터 업데이트
UPDATE chat_threads
SET current_phase = 'INITIALIZATION',
    phase_updated_at = NOW()
WHERE current_phase IS NULL;

-- 인덱스 추가 (Phase별 조회 최적화)
CREATE INDEX idx_chat_thread_phase ON chat_threads(current_phase);