package org.openhab.io.homekit.internal;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Dictionary;

import org.osgi.framework.FrameworkUtil;

/**
 * Provides the configured and static settings for the Homekit addon
 *
 * @author Andy Lintner
 */
public class HomekitSettings {

    private final static String NAME = "OpenHAB Homekit Bridge";
    private final static String MANUFACTURER = "OpenHAB";
    private final static String SERIAL_NUMBER = "none";

    private int port;
    private String pin;
    private boolean useFahrenheitTemperature = false;
    private double minimumTemperature = -100;
    private double maximumTemperature = 66;
    private String thermostatHeatMode;
    private String thermostatCoolMode;
    private String thermostatAutoMode;
    private String thermostatOffMode;
    private InetAddress networkInterface;

    public static HomekitSettings create(Dictionary<String, ?> properties) throws UnknownHostException {
        HomekitSettings settings = new HomekitSettings();
        String portString = (String) properties.get("port");
        if (portString == null) {
            throw new RuntimeException("No homekit port found");
        }
        settings.port = Integer.parseInt(portString);
        settings.pin = (String) properties.get("pin");
        if (settings.pin == null) {
            throw new RuntimeException("No homekit pin found");
        }
        String useFahrenheitTemperature = (String) properties.get("useFahrenheitTemperature");
        settings.useFahrenheitTemperature = Boolean.valueOf(useFahrenheitTemperature);
        Object minimumTemperature = properties.get("minimumTemperature");
        if (minimumTemperature != null) {
            settings.minimumTemperature = (double) minimumTemperature;
        }
        Object maximumTemperature = properties.get("maximumTemperature");
        if (maximumTemperature != null) {
            settings.maximumTemperature = (double) maximumTemperature;
        }
        settings.thermostatHeatMode = (String) properties.get("thermostatHeatMode");
        settings.thermostatCoolMode = (String) properties.get("thermostatCoolMode");
        settings.thermostatAutoMode = (String) properties.get("thermostatAutoMode");
        settings.thermostatOffMode = (String) properties.get("thermostatOffMode");

        String networkInterface = (String) properties.get("networkInterface");
        if (networkInterface == null) {
            settings.networkInterface = InetAddress.getLocalHost();
        } else {
            settings.networkInterface = InetAddress.getByName(networkInterface);
        }
        return settings;
    }

    public String getName() {
        return NAME;
    }

    public String getManufacturer() {
        return MANUFACTURER;
    }

    public String getSerialNumber() {
        return SERIAL_NUMBER;
    }

    public String getModel() {
        return FrameworkUtil.getBundle(getClass()).getVersion().toString();
    }

    public InetAddress getNetworkInterface() {
        return networkInterface;
    }

    public int getPort() {
        return port;
    }

    public String getPin() {
        return pin;
    }

    public boolean useFahrenheitTemperature() {
        return useFahrenheitTemperature;
    }

    public double getMaximumTemperature() {
        return maximumTemperature;
    }

    public double getMinimumTemperature() {
        return minimumTemperature;
    }

    public String getThermostatHeatMode() {
        return thermostatHeatMode;
    }

    public String getThermostatCoolMode() {
        return thermostatCoolMode;
    }

    public String getThermostatAutoMode() {
        return thermostatAutoMode;
    }

    public String getThermostatOffMode() {
        return thermostatOffMode;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(maximumTemperature);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(minimumTemperature);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + ((pin == null) ? 0 : pin.hashCode());
        result = prime * result + port;
        result = prime * result + ((thermostatAutoMode == null) ? 0 : thermostatAutoMode.hashCode());
        result = prime * result + ((thermostatCoolMode == null) ? 0 : thermostatCoolMode.hashCode());
        result = prime * result + ((thermostatHeatMode == null) ? 0 : thermostatHeatMode.hashCode());
        result = prime * result + ((thermostatOffMode == null) ? 0 : thermostatOffMode.hashCode());
        result = prime * result + (useFahrenheitTemperature ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        HomekitSettings other = (HomekitSettings) obj;
        if (Double.doubleToLongBits(maximumTemperature) != Double.doubleToLongBits(other.maximumTemperature))
            return false;
        if (Double.doubleToLongBits(minimumTemperature) != Double.doubleToLongBits(other.minimumTemperature))
            return false;
        if (pin == null) {
            if (other.pin != null)
                return false;
        } else if (!pin.equals(other.pin))
            return false;
        if (port != other.port)
            return false;
        if (thermostatAutoMode == null) {
            if (other.thermostatAutoMode != null)
                return false;
        } else if (!thermostatAutoMode.equals(other.thermostatAutoMode))
            return false;
        if (thermostatCoolMode == null) {
            if (other.thermostatCoolMode != null)
                return false;
        } else if (!thermostatCoolMode.equals(other.thermostatCoolMode))
            return false;
        if (thermostatHeatMode == null) {
            if (other.thermostatHeatMode != null)
                return false;
        } else if (!thermostatHeatMode.equals(other.thermostatHeatMode))
            return false;
        if (thermostatOffMode == null) {
            if (other.thermostatOffMode != null)
                return false;
        } else if (!thermostatOffMode.equals(other.thermostatOffMode))
            return false;
        if (useFahrenheitTemperature != other.useFahrenheitTemperature)
            return false;
        return true;
    }

}
