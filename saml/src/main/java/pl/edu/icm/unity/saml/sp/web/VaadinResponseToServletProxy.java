/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp.web;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.servlet.http.HttpServletResponse;

import com.vaadin.server.VaadinServletResponse;

public class VaadinResponseToServletProxy implements InvocationHandler
{
	private VaadinServletResponse response;
	
	private VaadinResponseToServletProxy(VaadinServletResponse response)
	{
		this.response = response;
	}

	public static HttpServletResponse getProxiedResponse(VaadinServletResponse response)
	{
		return (HttpServletResponse) Proxy.newProxyInstance(
				HttpServletResponse.class.getClassLoader(), 
				new Class<?>[] {HttpServletResponse.class}, 
				new VaadinResponseToServletProxy(response));
	}
	
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
	{
		Method m = findMethod(method);
		return m.invoke(response, args);
	}

	private Method findMethod(Method method) throws Throwable
	{
		try
		{
			return VaadinServletResponse.class.getMethod(method.getName(),
					method.getParameterTypes());
		} catch (NoSuchMethodException e)
		{
			throw new IllegalStateException("Trying to invoke method " + method  
					+ " on VaadinServletResponse, which can not be found", e);
		}
	}
}