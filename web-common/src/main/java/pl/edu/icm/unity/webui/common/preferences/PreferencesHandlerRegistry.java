/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.preferences;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Gives access to web preferences handlers for given preference ids.
 * 
 * @author K. Benedyczak
 */
@Component
public class PreferencesHandlerRegistry
{
	private Map<String, PreferencesHandler> handlersByType = new HashMap<String, PreferencesHandler>();
	
	@Autowired
	public PreferencesHandlerRegistry(List<PreferencesHandler> handlers)
	{
		for (PreferencesHandler handler: handlers)
			handlersByType.put(handler.getPreferenceId(), handler);
	}
	
	public PreferencesHandler getHandler(String typeId)
	{
		PreferencesHandler handler = handlersByType.get(typeId);
		if (handler == null)
			throw new IllegalArgumentException("Preference type " + typeId + " has no handler registered");
		return handler;
	}
	
	public Set<String> getSupportedPreferenceTypes()
	{
		return handlersByType.keySet();
	}
}



