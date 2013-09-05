/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;

import com.vaadin.server.Page;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;


/**
 * Shows error notification. Exception stack trace is parsed to provide a meaningful information.
 * @author K. Benedyczak
 */
public class ErrorPopup
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, ErrorPopup.class);
	
	public static void showNotice(String caption, String description)
	{
		Notification n = new Notification(caption, description, Type.HUMANIZED_MESSAGE);
		n.setDelayMsec(-1);
		n.show(Page.getCurrent());
	}

	public static void showError(String caption, String description)
	{
		Notification n = new Notification(caption, description, Type.ERROR_MESSAGE);
		n.setDelayMsec(-1);
		n.show(Page.getCurrent());
	}

	public static void showFormError(UnityMessageSource msg)
	{
		Notification n = new Notification(msg.getMessage("Generic.formError"), 
				msg.getMessage("Generic.formErrorHint"), Type.ERROR_MESSAGE);
		n.setDelayMsec(-1);
		n.show(Page.getCurrent());
	}

	public static void showError(String message, Exception e)
	{
		String description = getHumanMessage(e);
		if (log.isDebugEnabled())
			Log.logException("Exception in error popup: ", e , log);
		showError(message, description);
	}
	
	public static String getHumanMessage(Throwable e)
	{
		StringBuilder sb = new StringBuilder();
		if (e instanceof AuthorizationException)
			return e.getMessage();
		if (e.getMessage() != null)
			sb.append(e.getMessage());
		while (e.getCause() != null)
		{
			e = e.getCause();
			if (e.getMessage() == null)
				break;
			sb.append("; ").append(e.getMessage());
		}
		return sb.toString();
	}
}
