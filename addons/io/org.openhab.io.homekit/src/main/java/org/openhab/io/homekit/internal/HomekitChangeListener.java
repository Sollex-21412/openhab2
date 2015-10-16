package org.openhab.io.homekit.internal;

import java.util.Collection;

import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.items.ItemRegistry;
import org.eclipse.smarthome.core.items.ItemRegistryChangeListener;
import org.openhab.io.homekit.internal.accessories.HomekitLightbulbImpl;
import org.openhab.io.homekit.internal.accessories.HomekitThermostatImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beowulfe.hap.HomekitRoot;

public class HomekitChangeListener implements ItemRegistryChangeListener {

    private ItemRegistry itemRegistry;
    private HomekitAccessoryUpdater updater = new HomekitAccessoryUpdater();
    private Logger logger = LoggerFactory.getLogger(HomekitChangeListener.class);
    private final HomekitAccessoryRegistry accessoryRegistry = new HomekitAccessoryRegistry();
    private HomekitSettings settings;

    @Override
    public synchronized void added(Item item) {
        HomekitTaggedItem taggedItem = new HomekitTaggedItem(item);
        if (taggedItem.isTagged()) {
            if (taggedItem.isRootDevice()) {
                createRootDevice(taggedItem);
            } else {
                createCharacteristic(taggedItem);
            }
        }
    }

    @Override
    public void allItemsChanged(Collection<String> oldItemNames) {
        clearAccessories();
    }

    @Override
    public synchronized void removed(Item item) {
        HomekitTaggedItem taggedItem = new HomekitTaggedItem(item);
        if (taggedItem.isTagged()) {
            accessoryRegistry.remove(taggedItem);
        }
    }

    @Override
    public void updated(Item oldElement, Item element) {
        removed(oldElement);
        added(element);
    }

    public synchronized void clearAccessories() {
        accessoryRegistry.clear();
    }

    public synchronized void setBridge(HomekitRoot bridge) {
        accessoryRegistry.setBridge(bridge);
    }

    public synchronized void setItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
        itemRegistry.addRegistryChangeListener(this);
        itemRegistry.getAll().forEach(item -> added(item));
    }

    public void setUpdater(HomekitAccessoryUpdater updater) {
        this.updater = updater;
    }

    public void setSettings(HomekitSettings settings) {
        this.settings = settings;
    }

    private void createRootDevice(HomekitTaggedItem taggedItem) {

        switch (taggedItem.getDeviceType()) {
            case LIGHTBULB:
                accessoryRegistry.addRootDevice(new HomekitLightbulbImpl(taggedItem, itemRegistry, updater));
                break;

            case THERMOSTAT:
                accessoryRegistry.addRootDevice(new HomekitThermostatImpl(taggedItem, itemRegistry, updater, settings));
                break;

            default:
                logger.error("Unknown homekit type: " + taggedItem.getDeviceType());
                return;
        }

    }

    private void createCharacteristic(HomekitTaggedItem taggedItem) {
        accessoryRegistry.addCharacteristic(taggedItem);
    }
}
