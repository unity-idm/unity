/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.scripts;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.event.Event;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.event.EventListener;
import pl.edu.icm.unity.engine.api.initializers.ScriptConfiguration;
import pl.edu.icm.unity.exceptions.InternalException;

/**
 * Listens to all platform {@link Event}s and triggers scripts configured for the event (if any).
 *
 * @author K. Benedyczak
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
@Component
public class ScriptTriggeringEventListener implements EventListener
{
	public static final String ID = ScriptTriggeringEventListener.class.getName();
	
	private MainGroovyExecutor groovyExecutor;

	private Map<String, List<ScriptConfiguration>> scriptsByEvent;
	
	
	@Autowired
	public ScriptTriggeringEventListener(UnityServerConfiguration config,
			MainGroovyExecutor groovyExecutor)
	{
		this.groovyExecutor = groovyExecutor;
		scriptsByEvent = config.getContentInitializersConfiguration().stream().
				collect(Collectors.groupingBy(sc -> sc.getTrigger()));
	}

	@Override
	public boolean isLightweight()
	{
		return true;
	}

	@Override
	public boolean isWanted(Event event)
	{
		return true; //optimization - we anyway need to do another map get.
	}

	@Override
	public boolean handleEvent(Event event)
	{
		List<ScriptConfiguration> list = scriptsByEvent.get(event.getTrigger());
		if (list == null)
			return true;
		run(list, event);
		return true;
	}

	@Override
	public boolean isAsync(Event event)
	{
		return false;
	}
	
	@Override
	public String getId()
	{
		return ID;
	}

	@Override
	public int getMaxFailures()
	{
		return 0;
	}
	
	private void run(List<ScriptConfiguration> list, Event event)
	{
		list.forEach(script ->
		{
			switch (script.getType())
			{
			case groovy:
				groovyExecutor.run(script, event);
				break;
			default:
				throw new InternalException("Unrecognized initalizer type: " + 
						script.getType().name());
			}
		});
	}
}
