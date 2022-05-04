package com.godaddy.vps4.oh;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.multibindings.MapBinder;

/*
 * Each VPS4 environment (dev, test, etc.) maps to 1 or more Optimized Hosting "zones". These zones each have their own
 * base URL and auth string. This means there isn't a single ServiceProvider and request filter for each VPS4 environment
 * but rather a map of them, where the map key is the name of the optimized hosting zone.
 */

public class OhClientModule extends AbstractModule {
    @Override
    protected void configure() {
        MapBinder<String, OhApiBackupService> backupServiceBinder = MapBinder
                .newMapBinder(binder(), String.class, OhApiBackupService.class);

        String[] zones = {
                "sxb1-optimized-hosting",
                "sin2-optimized-hosting",
                "phx3-optimized-hosting"
        };

        for (String zone : zones) {
            backupServiceBinder.addBinding(zone)
                               .toProvider(new OhServiceProvider<>(zone, OhApiBackupService.class))
                               .in(Singleton.class);
        }
    }
}
