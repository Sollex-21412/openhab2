/**
 * Copyright (c) 2014 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zigbee.handler;

import static org.openhab.binding.zigbee.ZigBeeBindingConstants.PARAMETER_CHANNEL;
import static org.openhab.binding.zigbee.ZigBeeBindingConstants.PARAMETER_PANID;

import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;

import org.bubblecloud.zigbee.ZigBeeApi;
import org.bubblecloud.zigbee.api.Device;
import org.bubblecloud.zigbee.api.DeviceListener;
import org.bubblecloud.zigbee.api.ZigBeeDeviceException;
import org.bubblecloud.zigbee.api.cluster.Cluster;
import org.bubblecloud.zigbee.api.cluster.general.ColorControl;
import org.bubblecloud.zigbee.api.cluster.general.LevelControl;
import org.bubblecloud.zigbee.api.cluster.general.OnOff;
import org.bubblecloud.zigbee.api.cluster.impl.api.core.Attribute;
import org.bubblecloud.zigbee.api.cluster.impl.api.core.ZigBeeClusterException;
import org.bubblecloud.zigbee.util.Cie;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.library.types.HSBType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.zigbee.discovery.ZigBeeDiscoveryService;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ZigBeeCoordinatorHandler} is responsible for handling commands,
 * which are sent to one of the channels.
 * 
 * @author Chris Jackson - Initial contribution
 */
public abstract class ZigBeeCoordinatorHandler extends BaseBridgeHandler
		implements DeviceListener {
	protected int panId;
	protected int channelId;

	protected ZigBeeApi zigbeeApi;
	private ScheduledFuture<?> pollingJob;
	
	private ZigBeeDiscoveryService discoveryService;

	private ConcurrentMap<String, ZigBeeEventListener> eventListeners = new ConcurrentHashMap();

	private Logger logger = LoggerFactory
			.getLogger(ZigBeeCoordinatorHandler.class);

	public ZigBeeCoordinatorHandler(Bridge coordinator) {
		super(coordinator);
	}

	protected void subscribeEvents(String macAddress,
			ZigBeeEventListener handler) {
		eventListeners.put(macAddress, handler);
	}

	@Override
	public void initialize() {
		logger.debug("Initializing ZigBee coordinator.");

		// panId = ((BigDecimal)getConfig().get(PARAMETER_PANID)).intValue();
		panId = Integer.parseInt((String) getConfig().get(PARAMETER_PANID));
		channelId = Integer.parseInt((String) getConfig()
				.get(PARAMETER_CHANNEL));

		super.initialize();
	}

	@Override
	public void dispose() {
		// Remove the discovery service
		discoveryService.deactivate();

		// Shut down the ZigBee library
		zigbeeApi.shutdown();
		logger.debug("ZigBee network closed.");
	}

	@Override
	protected void updateStatus(ThingStatus status) {
		super.updateStatus(status);
		for (Thing child : getThing().getThings()) {
			child.setStatus(status);
		}
	}

	/**
	 * Called after initial browsing is complete. At this point we're good to go
	 */
	protected void browsingComplete() {
		logger.debug("ZigBee network READY. Found "
				+ zigbeeApi.getDevices().size() + " nodes.");

		updateStatus(ThingStatus.ONLINE);

		final List<Device> devices = zigbeeApi.getDevices();
		for (int i = 0; i < devices.size(); i++) {
			final Device device = devices.get(i);
			logger.debug("ZigBee '{}' device at address {}",
					device.getDeviceType(), device.getEndpointId());

			addNewDevice(device);

			// Signal to the handlers that they are known...
			if (eventListeners.get(device.getEndpointId()) != null) {
				eventListeners.get(device.getEndpointId())
						.onEndpointStateChange();
			}
		}

		// Add a listener for any new devices
		zigbeeApi.addDeviceListener(this);
	}

	/**
	 * Wait for the network initialisation to complete.
	 */
	protected void waitForNetwork() {
		// Start the discovery service
        discoveryService = new ZigBeeDiscoveryService(this);
        discoveryService.activate();

        // And register it as an OSGi service
        bundleContext.registerService(DiscoveryService.class.getName(), discoveryService, new Hashtable<String, Object>());

		logger.debug("Browsing ZigBee network ...");
		Thread thread = new Thread() {
			public void run() {
				while (!zigbeeApi.isInitialBrowsingComplete()) {
					try {
						Thread.sleep(250);
					} catch (InterruptedException e) {
						break;
					}
				}

				browsingComplete();
			}
		};

		// Kick off the discovery
		thread.start();
	}

	private Device getDeviceByIndexOrEndpointId(ZigBeeApi zigbeeApi,
			String deviceIdentifier) {
		Device device;
		device = zigbeeApi.getDevice(deviceIdentifier);
		if (device == null) {
			logger.debug("Error finding ZigBee device with address {}",
					deviceIdentifier);
		}
		return device;
	}

	public boolean LightPower(String lightAddress, OnOffType state) {
		final Device device = getDeviceByIndexOrEndpointId(zigbeeApi,
				lightAddress);
		if (device == null) {
			return false;
		}
		final OnOff onOff = device.getCluster(OnOff.class);
		try {
			if (state == OnOffType.ON) {
				onOff.on();
			} else {
				onOff.off();
			}
		} catch (ZigBeeDeviceException e) {
			e.printStackTrace();
		}

		return true;
	}

	public boolean LightBrightness(String lightAddress, int state) {
		final Device device = getDeviceByIndexOrEndpointId(zigbeeApi,
				lightAddress);
		if (device == null) {
			return false;
		}

		try {
			state = state * 256 / 100;
			if (state > 254) {
				state = 254;
			}
			if (state < 0) {
				state = 0;
			}

			final LevelControl levelControl = device
					.getCluster(LevelControl.class);
			levelControl.moveToLevel((short) state, 10);
		} catch (ZigBeeDeviceException e) {
			e.printStackTrace();
		}

		return true;
	}

	public boolean LightColor(String lightAddress, HSBType state) {
		final Device device = getDeviceByIndexOrEndpointId(zigbeeApi,
				lightAddress);
		if (device == null) {
			return false;
		}

		final ColorControl colorControl = device.getCluster(ColorControl.class);
		if (colorControl == null) {
			logger.debug("Device {} does not support color control.",
					lightAddress);
			return false;
		}

		try {
			int red = state.getRed().intValue();
			int green = state.getGreen().intValue();
			int blue = state.getBlue().intValue();

			Cie cie = Cie.rgb2cie(red, green, blue);
			int x = (int) (cie.x * 65536);
			int y = (int) (cie.y * 65536);
			if (x > 65279) {
				x = 65279;
			}
			if (y > 65279) {
				y = 65279;
			}
			colorControl.moveToColor(x, y, 10);
		} catch (ZigBeeDeviceException e) {
			e.printStackTrace();
		}

		return true;
	}

	public Object attributeRead(String zigbeeAddress, int clusterId, int attributeIndex) {
		final Device device = getDeviceByIndexOrEndpointId(zigbeeApi,
				zigbeeAddress);
		if (device == null) {
			return null;
		}
		
		return readAttribute(device, clusterId, attributeIndex);
	}
	
	
	public Object readAttribute(Device device, int clusterId, int attributeIndex) {
		final Cluster cluster = device.getCluster(clusterId);
		if (cluster == null) {
			logger.debug("Cluster not found.");
			return null;
		}

		final Attribute attribute = cluster.getAttributes()[attributeIndex];
		if (attribute == null) {
			logger.debug("Attribute not found.");
			return null;
		}

		try {
			return attribute.getValue();
		} catch (ZigBeeClusterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Returns a list of all known devices
	 * @return list of devices
	 */
	public List<Device> getDeviceList() {
		return zigbeeApi.getDevices();
	}

	public void startDeviceDiscovery() {
		final List<Device> devices = zigbeeApi.getDevices();
		for (int i = 0; i < devices.size(); i++) {
			final Device device = devices.get(i);
			logger.debug("ZigBee '{}' device at address {}",
					device.getDeviceType(), device.getEndpointId());
			addNewDevice(device);
		}

		
//		ZigBeeDiscoveryManager discoveryManager = zigbeeApi.getZigBeeDiscoveryManager();
//		discoveryManager.
	}
	
	/**
	 * Adds a device listener to receive updates on device status
	 * @param listener
	 */
	public void addDeviceListener(DeviceListener listener) {
		zigbeeApi.addDeviceListener(listener);
	}

	/**
	 * Removes a device listener to receive updates on device status
	 * @param listener
	 */
	public void removeDeviceListener(DeviceListener listener) {
		zigbeeApi.removeDeviceListener(listener);
	}

	@Override
	public void handleCommand(ChannelUID channelUID, Command command) {
		// TODO Auto-generated method stub

	}

	@Override
	public void deviceAdded(Device device) {
		// TODO Auto-generated method stub
		logger.debug("Device ADDED: {} {} {}", device.getIEEEAddress(),
				device.getDeviceType(), device.getProfileId());
		
		addNewDevice(device);
	}

	@Override
	public void deviceUpdated(Device device) {
		// TODO Auto-generated method stub
		logger.debug("Device UPDATED: {} {} {}", device.getIEEEAddress(),
				device.getDeviceType(), device.getProfileId());
	}

	@Override
	public void deviceRemoved(Device device) {
		// TODO Auto-generated method stub
		logger.debug("Device REMOVED: {} {} {}", device.getIEEEAddress(),
				device.getDeviceType(), device.getProfileId());
	}
	
	private class DiscoveryThread extends Thread {
		public void run(Device device) {
			logger.debug("Device Discovery: {} {} {}", device.getIEEEAddress(),
					device.getDeviceType(), device.getProfileId());
			
			String description = null;
			Object manufacturer = readAttribute(device, 0, 4);		// Manufacturer
			if(manufacturer != null) {
				description = manufacturer.toString();
				Object model = readAttribute(device, 0, 5);			// Model
				if(model != null) {
					description = manufacturer.toString() + ":" + model.toString();
				}
			}

			discoveryService.deviceAdded(device, description);
		}
	}

	private void addNewDevice(Device device) {
		DiscoveryThread discover = new DiscoveryThread();
		discover.run(device);
	}
}
