/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.credreset;

import com.vaadin.server.VaadinSession;
import com.vaadin.server.WrappedSession;

/**
 * Additional security measure: the user's verification progress is recorded to a 
 * variable stored in session. If user is anyhow trying to perform an operation without fulfilling
 * the previous steps the application will stop him. This shouldn't be possible anyway by Vaadin's design
 * but as Unity's engine authZ is not on guard in password reset case let's go secure.
 * @author K. Benedyczak
 */
public class CredentialResetStateVariable
{
	private static final String SES_NAME = CredentialResetStateVariable.class.getName(); 
	/**
	 * @return returns a current state of the variable. In case no variable is defined 0 is returned.
	 */
	public static int get()
	{
		VaadinSession session = VaadinSession.getCurrent();
		Object var = session.getSession().getAttribute(SES_NAME);
		if (var == null)
			return 0;
		return (Integer) var;
	}
	
	/**
	 * Increases the variable value. If the variable is not set, it is set to 1.
	 */
	public static void inc()
	{
		VaadinSession vSession = VaadinSession.getCurrent();
		WrappedSession session = vSession.getSession();
		Integer var = (Integer) session.getAttribute(SES_NAME);
		if (var == null)
			var = new Integer(1);
		else
			var = new Integer(var+1);
		session.setAttribute(SES_NAME, var);
	}
	
	public static void reset()
	{
		VaadinSession vSession = VaadinSession.getCurrent();
		WrappedSession session = vSession.getSession();
		session.removeAttribute(SES_NAME);
	}
}
