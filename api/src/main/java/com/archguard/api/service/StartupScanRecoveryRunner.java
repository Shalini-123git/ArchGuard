package com.archguard.api.service;

import com.archguard.api.persistence.ScanEntity;
import com.archguard.api.persistence.ScanStatus;
import com.archguard.api.repository.ScanJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Marks scans left active by a prior process as interrupted during startup. */
@Component
public class StartupScanRecoveryRunner implements ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(StartupScanRecoveryRunner.class);
    private static final String INTERRUPTED_MESSAGE = "Scan interrupted by server restart";
    private final ScanJpaRepository scans;

    public StartupScanRecoveryRunner(ScanJpaRepository scans) { this.scans = scans; }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        List<ScanEntity> interrupted = scans.findByStatusIn(List.of(ScanStatus.QUEUED, ScanStatus.RUNNING));
        // Failing instead of retrying avoids duplicating partially persisted analysis data.
        interrupted.forEach(scan -> scan.markFailed(INTERRUPTED_MESSAGE));
        scans.saveAll(interrupted);
        if (!interrupted.isEmpty()) LOGGER.info("event=startup_scan_recovery interruptedCount={}", interrupted.size());
    }
}