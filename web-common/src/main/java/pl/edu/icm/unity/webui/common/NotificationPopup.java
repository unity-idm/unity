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
import com.vaadin.server.Resource;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.themes.ValoTheme;


/**
 * Shows notification. Exception stack trace is parsed to provide a meaningful information in case
 * of errors. Based on Vaadin's {@link Notification}, this class ensures that it is used in the same way in the 
 * whole system. 
 * @author K. Benedyczak
 */
public class NotificationPopup
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, NotificationPopup.class);
	
	public static void showSuccess(UnityMessageSource msg, String caption, String description)
	{
		showGeneric(caption, description, Type.HUMANIZED_MESSAGE, Images.stdinfo64.getResource(), 
				ValoTheme.NOTIFICATION_SUCCESS, 
				ValoTheme.NOTIFICATION_CLOSABLE);
	}

	public static void showNotice(UnityMessageSource msg, String caption, String description)
	{
		showGeneric(caption, description, Type.WARNING_MESSAGE, Images.stdwarn64.getResource(),
				ValoTheme.NOTIFICATION_CLOSABLE);
	}

	public static void showError(UnityMessageSource msg, String caption, String description)
	{
		showGeneric(caption, description, Type.ERROR_MESSAGE, Images.stderror64.getResource(),
				ValoTheme.NOTIFICATION_CLOSABLE);
	}

	public static void showFormError(UnityMessageSource msg)
	{
		showError(msg, msg.getMessage("Generic.formError"), msg.getMessage("Generic.formErrorHint"));
	}

	public static void showError(UnityMessageSource msg, String message, Exception e)
	{
		String description = getHumanMessage(e);
		if (description.trim().isEmpty())
			description = msg.getMessage("Generic.formErrorHint");

		if (log.isDebugEnabled())
		{
			log.debug("Error popup showed an error to the user: " + message);
			log.debug("What's more there was an exception attached which caused an error:", e);
		}
		showError(msg, message, description);
	}

	public static String getHumanMessage(Throwable e)
	{
		return getHumanMessage(e, "; ");
	}
	
	public static String getHumanMessage(Throwable e, String separator)
	{
		StringBuilder sb = new StringBuilder();
		if (e instanceof AuthorizationException)
			return e.getMessage();
		String lastMessage = "";
		if (e.getMessage() != null)
		{
			lastMessage = e.getMessage();
			sb.append(lastMessage);
		}
		while (e.getCause() != null)
		{
			e = e.getCause();
			if (e.getMessage() == null)
				break;
			if (e.getMessage().equals(lastMessage))
				continue;
			lastMessage = e.getMessage();
			sb.append(separator).append(lastMessage);
		}
		return sb.toString();
	}
	
	
	private static void showGeneric(String caption, String description, Type type, 
			Resource icon, String... styles)
	{
		Notification notification = new Notification(caption, description, type);
		StringBuilder sb = new StringBuilder(notification.getStyleName());
		for (String style: styles)
			sb.append(" ").append(style);
		
		notification.setStyleName(sb.toString());
		notification.setIcon(icon);
		notification.setDelayMsec(-1);
		notification.show(Page.getCurrent());
	}
}
