package org.openhab.io.homekit.internal.accessories;

import java.util.concurrent.CompletableFuture;

import org.eclipse.smarthome.core.items.ItemRegistry;
import org.eclipse.smarthome.core.library.items.NumberItem;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.openhab.io.homekit.internal.HomekitAccessoryUpdater;
import org.openhab.io.homekit.internal.HomekitSettings;
import org.openhab.io.homekit.internal.HomekitTaggedItem;

import com.beowulfe.hap.HomekitCharacteristicChangeCallback;
import com.beowulfe.hap.accessories.TemperatureSensor;

/**
 * Implements a Homekit TemperatureSensor using a NumberItem
 *
 * @author Andy Lintner
 */
class HomekitTemperatureSensorImpl extends AbstractTemperatureHomekitAccessoryImpl<NumberItem>
        implements TemperatureSensor {

    public HomekitTemperatureSensorImpl(HomekitTaggedItem taggedItem, ItemRegistry itemRegistry,
            HomekitAccessoryUpdater updater, HomekitSettings settings) {
        super(taggedItem, itemRegistry, updater, settings, NumberItem.class);
    }

    @Override
    public CompletableFuture<Double> getCurrentTemperature() {
        DecimalType state = (DecimalType) getItem().getStateAs(DecimalType.class);
        if (state == null) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.completedFuture(convertToCelsius(state.doubleValue()));
    }

    @Override
    public void subscribeCurrentTemperature(HomekitCharacteristicChangeCallback callback) {
        getUpdater().subscribe(getItem(), callback);
    }

    @Override
    public void unsubscribeCurrentTemperature() {
        getUpdater().unsubscribe(getItem());
    }
}