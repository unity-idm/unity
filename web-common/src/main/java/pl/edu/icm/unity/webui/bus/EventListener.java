/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.bus;

/**
 * Implemented by receivers of events.
 * @author K. Benedyczak
 * @param <T>
 */
public interface EventListener<T extends Event>
{
	public void handleEvent(T event);
}
