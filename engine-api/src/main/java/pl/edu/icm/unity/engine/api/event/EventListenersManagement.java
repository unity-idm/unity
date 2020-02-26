/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.engine.api.event;

import java.util.Set;

/**
 * Management of event listeners 
 * @author P.Piernik
 *
 */
public interface EventListenersManagement
{
	void addEventListener(EventListener eventListener);
	void removeEventListener(EventListener eventListener);
	Set<EventListener> getListeners();
} 
