package com.nodatanolife.bifusion_be.domain.file.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * 더 이상 쓰지 않는 파일의 S3 객체를 트랜잭션 커밋 뒤에 지운다. 롤백되면 지우지 않는다.
 * 삭제 실패는 로그만 남긴다 (DB에서는 이미 사용량 집계에서 빠져 있어 요청을 실패시킬 이유가 없다).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class S3ObjectCleaner {

    private final S3Service s3Service;

    /**
     * @param s3Keys 지울 객체 key. null·중복은 건너뛴다.
     */
    public void deleteAfterCommit(Collection<String> s3Keys) {
        List<String> keys = s3Keys.stream().filter(Objects::nonNull).distinct().toList();
        if (keys.isEmpty()) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            delete(keys);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                delete(keys);
            }
        });
    }

    /**
     * 방금 올린 객체를 트랜잭션이 롤백되면 지운다 (DB에 연결되지 못한 객체가 남지 않게). 커밋되면 그대로 둔다.
     * 트랜잭션 밖에서 호출하면 아무것도 하지 않는다.
     *
     * @param s3Keys 지울 객체 key. null·중복은 건너뛴다.
     */
    public void deleteAfterRollback(Collection<String> s3Keys) {
        List<String> keys = s3Keys.stream().filter(Objects::nonNull).distinct().toList();
        if (keys.isEmpty() || !TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    delete(keys);
                }
            }
        });
    }

    private void delete(List<String> keys) {
        List<String> failed;
        try {
            failed = s3Service.deleteFiles(keys);
        } catch (RuntimeException e) {
            failed = keys;
        }
        for (String key : failed) {
            log.warn("S3 객체 삭제 실패, 객체가 남음: key={}", key);
        }
    }
}
