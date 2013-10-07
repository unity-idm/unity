/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.generic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;

/**
 * Notifications hub for dependencies. Each {@link GenericEntityHandler} registers listener for 
 * its dependences. Implementation of DB code will fire the events for each type of its object.
 * <p>
 * This class is thread safe.
 * @author K. Benedyczak
 */
@Component
public class DependencyNotificationManager
{
	private Map<String, List<DependencyChangeListener<?>>> listenersByType = 
			new HashMap<String, List<DependencyChangeListener<?>>>();
	
	
	public synchronized void addListener(DependencyChangeListener<?> listener)
	{
		String key = listener.getDependencyObjectType();
		List<DependencyChangeListener<?>> listeners = listenersByType.get(key);
		if (listeners == null)
		{
			listeners = new ArrayList<>();
			listenersByType.put(key, listeners);
		}
		listeners.add(listener);
	}

	public synchronized boolean hasListeners(String type)
	{
		return listenersByType.containsKey(type);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void firePreAddEvent(String type, Object added, SqlSession sql) throws EngineException
	{
		List<DependencyChangeListener<?>> listeners = listenersByType.get(type);
		if (listeners == null)
			return;
		for (DependencyChangeListener listener: listeners)
		{
			listener.preAdd(added, sql);
		}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void firePreUpdateEvent(String type, Object old, Object updated, SqlSession sql) throws EngineException
	{
		List<DependencyChangeListener<?>> listeners = listenersByType.get(type);
		if (listeners == null)
			return;
		for (DependencyChangeListener listener: listeners)
		{
			listener.preUpdate(old, updated, sql);
		}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void firePreRemoveEvent(String type, Object removed, SqlSession sql) throws EngineException
	{
		List<DependencyChangeListener<?>> listeners = listenersByType.get(type);
		if (listeners == null)
			return;
		for (DependencyChangeListener listener: listeners)
		{
			listener.preRemove(removed, sql);
		}
	}
}
