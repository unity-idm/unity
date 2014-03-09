/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.events;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;

import pl.edu.icm.unity.server.api.internal.LoginSession;
import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.server.events.Event;

/**
 * Java dynamic proxy decorator - fires up events infrastructure.
 * TODO should collect info about arguments marked with annotations and report exceptions with more details
 * @author K. Benedyczak
 */
public class EventDecoratingHandler implements InvocationHandler
{
	public static final String CATEGORY_INVOCATION = "methodInvocation";
	private Object wrappedObject;
	private EventProcessor processor;
	private String interfaceName;
	
	
	public EventDecoratingHandler(Object wrappedObject, EventProcessor processor, String interfaceName)
	{
		this.wrappedObject = wrappedObject;
		this.processor = processor;
		this.interfaceName = interfaceName;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
	{
		LoginSession ae = InvocationContext.getCurrent().getLoginSession();
		Long invoker = ae == null ? null : ae.getEntityId();
		try
		{
			Object ret = method.invoke(wrappedObject, args);
			Event event = new Event(CATEGORY_INVOCATION, invoker, new Date());
			event.setContents(getMethodDescription(method, null, args));
			processor.fireEvent(event);
			return ret;
		} catch (InvocationTargetException ite)
		{
			Event event = new Event(CATEGORY_INVOCATION, invoker, new Date());
			event.setContents(getMethodDescription(method, ite.getCause().toString(), args));
			processor.fireEvent(event);
			throw ite.getCause();
		}
	}
	
	private String getMethodDescription(Method method, String exception, Object[] args)
	{
		InvocationEventContents desc = new InvocationEventContents(method.getName(), interfaceName, exception);
		return desc.toJson();
	}
}
