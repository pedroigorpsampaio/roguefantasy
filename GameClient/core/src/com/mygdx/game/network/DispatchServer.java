package com.mygdx.game.network;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * Represents a server that fire changes to listeners
 * through Java's PropertyChange implementation
 */
public class DispatchServer {
    PropertyChangeSupport listeners; // the listeners of this server

    public DispatchServer() {
        // listeners that are going to be notified on server responses
        listeners = new PropertyChangeSupport(this);
    }

    /**
     * SERVER LISTENER METHODS
     */

    /**
     * Adds listener to the server that listen to all property changes
     * @param listener  the PCL to be added to the server listeners
     */
    public void addListener(PropertyChangeListener listener) {
        listeners.addPropertyChangeListener(listener);
    }
    /**
     * Adds a listener for a specific property.
     * @param propertyName  the property that the listener will listen
     * @param listener      the listener to be added for a specific property
     */
    public void addListener(String propertyName, PropertyChangeListener listener) {
        listeners.addPropertyChangeListener(propertyName, listener);
    }
    /**
     * Removes listener from the server listeners
     * @param listener  the PCL to be removed from the server listeners
     */
    public void removeListener(PropertyChangeListener listener) {
        listeners.removePropertyChangeListener(listener);
    }
    /**
     * Removes a listener for a specific property.
     * @param propertyName  the property that the listener listens
     * @param listener      the listener to be removed for a specific property
     */
    public void removeListener(String propertyName, PropertyChangeListener listener) {
        listeners.removePropertyChangeListener(propertyName, listener);
    }

    /**
     * Check if listener is already in server listeners list.
     * @param listener  the listener to be checked if its in the listeners list
     * @return true if it is on the list, false otherwise
     */
    public boolean isListening(PropertyChangeListener listener) {
        PropertyChangeListener[] pcls = listeners.getPropertyChangeListeners();
        for (int i = 0; i < pcls.length; i++) {
            if (pcls[i].equals(listener))
                return true;
        }
        return false;
    }
    /**
     * Check if listener is already listening to a named property.
     * @param propertyName  the property to check if listener is listening to
     * @param listener  the listener to be checked
     * @return true if it is listening to the property, false otherwise
     */
    public boolean isListening(String propertyName, PropertyChangeListener listener) {
        PropertyChangeListener[] pcls = listeners.getPropertyChangeListeners(propertyName);
        for (int i = 0; i < pcls.length; i++) {
            if (pcls[i].equals(listener))
                return true;
        }
        return false;
    }
}
