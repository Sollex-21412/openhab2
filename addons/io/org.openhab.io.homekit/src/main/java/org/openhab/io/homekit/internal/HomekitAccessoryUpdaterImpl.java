package org.openhab.io.homekit.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.smarthome.core.items.events.AbstractItemEventSubscriber;
import org.eclipse.smarthome.core.items.events.ItemCommandEvent;
import org.eclipse.smarthome.core.items.events.ItemStateEvent;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.Type;
import org.openhab.io.homekit.HomekitAccessoryUpdater;

import com.beowulfe.hap.HomekitCharacteristicChangeCallback;

public class HomekitAccessoryUpdaterImpl extends AbstractItemEventSubscriber implements HomekitAccessoryUpdater {

    private final Map<String, Set<Subscription>> subscriptionsByName = new HashMap<>();

    @Override
    public void receiveUpdate(ItemStateEvent event) {
        processEvent(event.getItemName(), event.getItemState());
    }

    @Override
    public void receiveCommand(ItemCommandEvent event) {
        processEvent(event.getItemName(), event.getItemCommand());
    }

    @Override
    public synchronized void subscribe(String itemName, Class<? extends State> type,
            HomekitCharacteristicChangeCallback callback) {
        if (!subscriptionsByName.containsKey(itemName)) {
            subscriptionsByName.put(itemName, new HashSet<>());
        }
        subscriptionsByName.get(itemName).add(new Subscription(type, callback));
    }

    @Override
    public void unsubscribe(String itemName, Class<? extends State> type) {
        Set<Subscription> subscriptions = subscriptionsByName.get(itemName);
        if (subscriptions != null) {
            Iterator<Subscription> i = subscriptions.iterator();
            while (i.hasNext()) {
                if (i.next().type == type) {
                    i.remove();
                }
            }
        }
    }

    private void processEvent(String itemName, Type newState) {
        Set<Subscription> subscriptions = subscriptionsByName.get(itemName);
        if (subscriptions != null) {
            for (Subscription subscription : subscriptions) {
                if (subscription.type.isAssignableFrom(newState.getClass())) {
                    subscription.callback.changed();
                }
            }
        }
    }

    private static class Subscription {
        public final Class<? extends State> type;
        public final HomekitCharacteristicChangeCallback callback;

        public Subscription(Class<? extends State> type, HomekitCharacteristicChangeCallback callback) {
            this.type = type;
            this.callback = callback;
        }
    }
}
