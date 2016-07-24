/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.hdpowerview.discovery;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.hdpowerview.config.HDPowerViewSceneConfiguration;
import org.openhab.binding.hdpowerview.handler.HDPowerViewHubHandler;
import org.openhab.binding.hdpowerview.internal.HDPowerViewWebTargets;
import org.openhab.binding.hdpowerview.internal.api.responses.Scenes;
import org.openhab.binding.hdpowerview.internal.api.responses.Scenes.Scene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Discovers a HD Power View Scene from an existing Hub
 *
 * @author Andy Lintner
 */
public class HDPowerViewSceneDiscoveryService extends AbstractDiscoveryService {

    private final Logger logger = LoggerFactory.getLogger(HDPowerViewSceneDiscoveryService.class);
    private final HDPowerViewHubHandler hub;
    private final Runnable scanner;
    private ScheduledFuture<?> backgroundFuture;

    public HDPowerViewSceneDiscoveryService(HDPowerViewHubHandler hub) {
        super(Collections.emptySet(), 600, true);
        this.hub = hub;

        scanner = createScanner();
    }

    @Override
    protected void startScan() {
        scheduler.execute(scanner);
    }

    @Override
    protected void startBackgroundDiscovery() {
        if (backgroundFuture != null && !backgroundFuture.isDone()) {
            backgroundFuture.cancel(true);
            backgroundFuture = null;
        }
        backgroundFuture = scheduler.scheduleAtFixedRate(scanner, 0, 60, TimeUnit.SECONDS);
    }

    @Override
    protected void stopBackgroundDiscovery() {
        if (backgroundFuture != null && !backgroundFuture.isDone()) {
            backgroundFuture.cancel(true);
            backgroundFuture = null;
        }
        super.stopBackgroundDiscovery();
    }

    private Runnable createScanner() {
        return () -> {
            HDPowerViewWebTargets targets = hub.getWebTargets();
            Scenes scenes;
            try {
                scenes = targets.getScenes();
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
                return;
            }
            if (scenes != null) {
                for (Scene scene : scenes.sceneData) {
                    ThingUID thingUID = new ThingUID("SKIP", Integer.toString(scene.id));
                    DiscoveryResult result = DiscoveryResultBuilder.create(thingUID)
                            .withProperty(HDPowerViewSceneConfiguration.ID, scene.id).withLabel(scene.getName())
                            .withBridge(hub.getThing().getUID()).build();
                    thingDiscovered(result);
                }
            }
        };
    }

}
