-- Project: 공개/비공개 컬럼 추가, 기존 row는 공개로 백필
ALTER TABLE project
    ADD COLUMN is_public BOOLEAN DEFAULT TRUE;

-- ProjectMember: 마이페이지 공개 여부 컬럼 추가
ALTER TABLE project_member
    ADD COLUMN is_public BOOLEAN;

-- 기존 멤버 row 백필: 소속 프로젝트의 공개 여부를 그대로 따름
UPDATE project_member pm
SET is_public = COALESCE(
        (SELECT p.is_public FROM project p WHERE p.project_id = pm.project_id),
        TRUE
    );
