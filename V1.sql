-- invitee_id nullable 변경
ALTER TABLE project_invitation
    ALTER COLUMN invitee_id DROP NOT NULL;

-- invitee_email 추가
ALTER TABLE project_invitation
    ADD COLUMN invitee_email VARCHAR(255);

-- last_sent_at 추가
ALTER TABLE project_invitation
    ADD COLUMN last_sent_at TIMESTAMP;

-- CHECK 제약
ALTER TABLE project_invitation
    ADD CONSTRAINT project_invitation_invitee_check
        CHECK (invitee_id IS NOT NULL OR invitee_email IS NOT NULL);

-- 동일 프로젝트 + 동일 이메일에 대해 PENDING 초대는 1건만 허용 (동시성 가드)
CREATE UNIQUE INDEX uq_project_invitation_pending
    ON project_invitation (project_id, invitee_email)
    WHERE status = 'PENDING';