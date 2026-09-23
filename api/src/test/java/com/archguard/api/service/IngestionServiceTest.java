package com.archguard.api.service;

import com.archguard.api.config.ArchGuardProperties;
import com.archguard.api.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IngestionServiceTest {
    private final IngestionService service = new IngestionService(new ArchGuardProperties());

    @Test
    void rejectsNonHttpsRepositoryUrlsBeforeAnyClone() {
        assertThrows(BadRequestException.class, () -> service.validateRemoteUrl("file:///tmp/untrusted"));
        assertThrows(BadRequestException.class, () -> service.validateRemoteUrl("git@github.com:example/project.git"));
    }

    @Test
    void acceptsHttpsRepositoryUrls() {
        assertDoesNotThrow(() -> service.validateRemoteUrl("https://github.com/example/project.git"));
    }
}
