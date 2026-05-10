-- 마이페이지 커뮤니티 활동 공개 여부 컬럼 추가
-- ddl-auto: update 가 컬럼 자체는 자동 생성하지만, 기존 row 백필과 DEFAULT 제약은 직접 처리

ALTER TABLE recipe     ADD COLUMN IF NOT EXISTS is_mypage_visible BOOLEAN;
ALTER TABLE dataset    ADD COLUMN IF NOT EXISTS is_mypage_visible BOOLEAN;
ALTER TABLE expert_qna ADD COLUMN IF NOT EXISTS is_mypage_visible BOOLEAN;
ALTER TABLE recruit    ADD COLUMN IF NOT EXISTS is_mypage_visible BOOLEAN;

-- 기존 row 백필: 콘텐츠가 공개(is_public=true)면 마이페이지에도 공개, 아니면 비공개
UPDATE recipe     SET is_mypage_visible = COALESCE(is_public, false) WHERE is_mypage_visible IS NULL;
UPDATE dataset    SET is_mypage_visible = COALESCE(is_public, false) WHERE is_mypage_visible IS NULL;
UPDATE expert_qna SET is_mypage_visible = COALESCE(is_public, false) WHERE is_mypage_visible IS NULL;
UPDATE recruit    SET is_mypage_visible = COALESCE(is_public, false) WHERE is_mypage_visible IS NULL;

-- 새로 INSERT 되는 row의 기본값
ALTER TABLE recipe     ALTER COLUMN is_mypage_visible SET DEFAULT true;
ALTER TABLE dataset    ALTER COLUMN is_mypage_visible SET DEFAULT true;
ALTER TABLE expert_qna ALTER COLUMN is_mypage_visible SET DEFAULT true;
ALTER TABLE recruit    ALTER COLUMN is_mypage_visible SET DEFAULT true;
