package com.nodatanolife.bifusion_be.notification;

import com.nodatanolife.bifusion_be.domain.notification.dto.response.NotificationResponse;
import com.nodatanolife.bifusion_be.domain.notification.entity.NotificationType;
import com.nodatanolife.bifusion_be.domain.notification.entity.TargetType;
import com.nodatanolife.bifusion_be.domain.notification.service.NotificationService;
import com.nodatanolife.bifusion_be.domain.user.entity.User;
import com.nodatanolife.bifusion_be.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 알림창에서 지운 알림은 알림창 목록에서만 빠지고 전체목록에는 남는다.
 */
class NotificationPopupIntegrationTest extends IntegrationTestSupport {

    @Autowired
    NotificationService notificationService;

    @Test
    void 알림창에서_지운_알림은_전체목록에_남고_알림창_목록에서만_빠진다() {
        User user = user("user@test.com", 0);
        notificationService.create(user, NotificationType.QNA_ANSWERED, "첫 알림", "내용", TargetType.QNA, 1L);
        notificationService.create(user, NotificationType.QNA_ANSWERED, "둘째 알림", "내용", TargetType.QNA, 2L);
        Long deletedId = list(user, false).stream().filter(n -> n.title().equals("첫 알림")).findFirst().orElseThrow().notificationId();

        notificationService.softDeleteNotifications(user.getUserId(), deletedId);

        assertThat(list(user, false)).extracting(NotificationResponse::notificationId).contains(deletedId).hasSize(2);
        assertThat(list(user, true)).extracting(NotificationResponse::notificationId).doesNotContain(deletedId).hasSize(1);
    }

    @Test
    void 알림창_전체_삭제도_전체목록에는_남는다() {
        User user = user("user@test.com", 0);
        notificationService.create(user, NotificationType.QNA_ANSWERED, "알림", "내용", TargetType.QNA, 1L);

        notificationService.softDeleteNotifications(user.getUserId(), null);

        assertThat(list(user, true)).isEmpty();
        assertThat(list(user, false)).hasSize(1);
    }

    @Test
    void 영구_삭제한_알림은_두_목록_모두에서_빠진다() {
        User user = user("user@test.com", 0);
        notificationService.create(user, NotificationType.QNA_ANSWERED, "알림", "내용", TargetType.QNA, 1L);
        Long id = list(user, false).get(0).notificationId();

        notificationService.hardDeleteNotifications(user.getUserId(), id);

        assertThat(list(user, false)).isEmpty();
        assertThat(list(user, true)).isEmpty();
    }

    private List<NotificationResponse> list(User user, boolean popup) {
        return notificationService.getNotifications(user.getUserId(), popup, null, null, null, PageRequest.of(0, 10)).getContent();
    }
}
