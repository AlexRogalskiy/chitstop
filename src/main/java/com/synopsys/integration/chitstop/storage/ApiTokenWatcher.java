/*
 * chitstop
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.chitstop.storage;

import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.chitstop.exception.GameOver;

public class ApiTokenWatcher implements Runnable {
    private final Logger logger = LoggerFactory.getLogger(ApiTokenWatcher.class);

    private final Path watchedPath;
    private final WatchService watchService;
    private final ApiTokenStorage apiTokenStorage;
    private final GameOver gameOver;

    public ApiTokenWatcher(Path watchedPath, WatchService watchService, ApiTokenStorage apiTokenStorage, GameOver gameOver) {
        this.watchedPath = watchedPath;
        this.watchService = watchService;
        this.apiTokenStorage = apiTokenStorage;
        this.gameOver = gameOver;
    }

    @Override
    public void run() {
        while (true) {
            WatchKey watchKey = null;
            try {
                watchKey = watchService.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                gameOver.endIt("The WatchService thread was interrupted.");
            }
            logger.info("A watchkey is now available.");
            for (WatchEvent<?> event : watchKey.pollEvents()) {
                if (StandardWatchEventKinds.ENTRY_MODIFY.equals(event.kind())) {
                    //todo try to use a type token
                    WatchEvent<Path> pathWatchEvent = (WatchEvent<Path>) event;
                    Path modifiedPath = pathWatchEvent.context();
                    if (ApiTokenStorage.TOKENS_FILENAME.equals(modifiedPath.toString())) {
                        logger.info(String.format("%s modified, loading changes.", modifiedPath.toString()));
                        apiTokenStorage.loadApiTokens();
                    }
                }
            }

            logger.info("Resetting the watchkey to receive future events.");
            watchKey.reset();
        }
    }

}
