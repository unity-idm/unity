/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.sessionscope;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;

import com.vaadin.server.VaadinSession;

import pl.edu.icm.unity.base.utils.Log;

public class WebSessionScope implements Scope
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, WebSessionScope.class);
	static final String NAME = "webSession";
	
	@Override
	public Object get(String name, ObjectFactory<?> objectFactory)
	{
		return getOrCreateObjectsMap().beans.computeIfAbsent(name, k -> objectFactory.getObject());
	}

	@Override
	public Object remove(String name)
	{
		return getOrCreateObjectsMap().beans.remove(name);
	}

	@Override
	public void registerDestructionCallback(String name, Runnable callback)
	{
		log.warn("Registering destruction callback tried for bean " + name + " what is unsupported");
	}

	@Override
	public Object resolveContextualObject(String key)
	{
		return null;
	}

	@Override
	public String getConversationId()
	{
		return getVaadinSession().getSession().getId();
	}
	
	private VaadinSession getVaadinSession()
	{
		VaadinSession current = VaadinSession.getCurrent();
		if (current == null)
			throw new NoWebSessionActiveForThreadException();
		return current;
	}

	private synchronized ObjectsMap getOrCreateObjectsMap()
	{
		VaadinSession vaadinSession = getVaadinSession();
		ObjectsMap objectsMap = vaadinSession.getAttribute(ObjectsMap.class);
		if (objectsMap == null)
		{
			objectsMap = new ObjectsMap();
			vaadinSession.setAttribute(ObjectsMap.class, objectsMap);
		}
		return objectsMap;
	}
	
	private static class ObjectsMap
	{
		private final Map<String, Object> beans = new ConcurrentHashMap<>();
	}
	
	private static class NoWebSessionActiveForThreadException extends IllegalStateException
	{
	}
}
