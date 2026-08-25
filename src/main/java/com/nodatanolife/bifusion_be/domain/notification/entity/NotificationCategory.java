package com.nodatanolife.bifusion_be.domain.notification.entity;

import java.util.List;

public enum NotificationCategory {

    EXPERT_INSPECTION(List.of(
            NotificationType.EXPERT_APPROVED,
            NotificationType.EXPERT_REJECTED,
            NotificationType.RECIPE_REVIEW_REQUESTED,
            NotificationType.RECIPE_REVIEW_ACCEPTED,
            NotificationType.RECIPE_APPROVED,
            NotificationType.RECIPE_REJECTED,
            NotificationType.DATASET_VERIFIED,
            NotificationType.DATASET_VERIFY_REJECTED,
            NotificationType.AUGMENTATION_INSPECTION_APPROVED,
            NotificationType.AUGMENTATION_INSPECTION_REJECTED
    )),
    AUGMENTATION(List.of(
            NotificationType.AUGMENTATION_SUCCESS,
            NotificationType.AUGMENTATION_FAILED
    )),
    PROJECT(List.of(
            NotificationType.PROJECT_INVITATION,
            NotificationType.INVITATION_ACCEPTED,
            NotificationType.INVITATION_REJECTED
    )),
    COMMUNITY(List.of(
            NotificationType.QNA_ANSWERED,
            NotificationType.RECIPE_REVIEW,
            NotificationType.RECRUITMENT_APPLY,
            NotificationType.RECRUITMENT_ACCEPTED,
            NotificationType.RECRUITMENT_REJECTED
    ));

    private final List<NotificationType> types;

    NotificationCategory(List<NotificationType> types) {
        this.types = types;
    }

    public List<NotificationType> getTypes() {
        return types;
    }
}