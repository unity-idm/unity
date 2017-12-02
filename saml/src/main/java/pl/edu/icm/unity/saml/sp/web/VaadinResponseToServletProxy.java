/*
 * Copyright (c) 2017 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp.web;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.servlet.http.HttpServletResponse;

import com.vaadin.server.VaadinResponse;

public class VaadinResponseToServletProxy implements InvocationHandler
{
	private VaadinResponse response;
	
	private VaadinResponseToServletProxy(VaadinResponse response)
	{
		this.response = response;
	}

	public static HttpServletResponse getProxiedResponse(VaadinResponse response)
	{
		return (HttpServletResponse) Proxy.newProxyInstance(
				HttpServletResponse.class.getClassLoader(), 
				new Class<?>[] {HttpServletResponse.class}, 
				new VaadinResponseToServletProxy(response));
	}
	
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
	{
		Method m = findMethod(response.getClass(), method);
		return m != null ? m.invoke(response, args) : null;
	}

	private Method findMethod(Class<?> clazz, Method method) throws Throwable
	{
		try
		{
			return clazz.getDeclaredMethod(method.getName(),
					method.getParameterTypes());
		} catch (NoSuchMethodException e)
		{
			return null;
		}
	}
}