/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import org.apache.logging.log4j.Logger;

import com.vaadin.server.Page;
import com.vaadin.server.Resource;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.themes.ValoTheme;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.webui.exceptions.ControllerException;


/**
 * Shows notification. Exception stack trace is parsed to provide a meaningful information in case
 * of errors. Based on Vaadin's {@link Notification}, this class ensures that it is used in the same way in the 
 * whole system. 
 * @author K. Benedyczak
 */
public class NotificationPopup
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, NotificationPopup.class);
	
	
	public static void showSuccess(String caption, String description)
	{
		showGeneric(caption, description, Type.HUMANIZED_MESSAGE, Images.info.getResource(), 
				ValoTheme.NOTIFICATION_SUCCESS, 
				ValoTheme.NOTIFICATION_CLOSABLE);
	}

	public static void showNotice(String caption, String description)
	{
		showGeneric(caption, description, Type.WARNING_MESSAGE, Images.warn.getResource(),
				ValoTheme.NOTIFICATION_CLOSABLE);
	}

	public static void showError(String caption, String description)
	{
		showGeneric(caption, description, Type.ERROR_MESSAGE, Images.error.getResource(),
				ValoTheme.NOTIFICATION_CLOSABLE);
	}
	
	public static void showError(ControllerException exception)
	{
		if (exception.getType() == pl.edu.icm.unity.webui.exceptions.ControllerException.Type.ERROR)
		{
			showError(exception.getCaption(), exception.getDetails());
		}else
		{
			showNotice(exception.getCaption(), exception.getDetails());
		}
	}

	public static void showFormError(UnityMessageSource msg)
	{
		showError(msg.getMessage("Generic.formError"), msg.getMessage("Generic.formErrorHint"));
	}

	public static Notification getNoticeNotification(String caption, String description)
	{
		return createGeneric(caption, description, Type.WARNING_MESSAGE, Images.warn.getResource(),
				ValoTheme.NOTIFICATION_CLOSABLE);
	}
	
	public static Notification getErrorNotification(String caption, String description)
	{
		return createGeneric(caption, description, Type.ERROR_MESSAGE, Images.error.getResource(),
				ValoTheme.NOTIFICATION_CLOSABLE);
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
		showError(message, description);
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
			if (!(e instanceof EngineException))
				break;
			lastMessage = e.getMessage();
			sb.append(separator).append(lastMessage);
		}
		return sb.toString();
	}
	
	private static Notification createGeneric(String caption, String description, Type type, 
			Resource icon, String... styles)
	{
		Notification notification = new Notification(caption, description, type);
		StringBuilder sb = new StringBuilder(notification.getStyleName());
		for (String style: styles)
			sb.append(" ").append(style);
		sb.append(" ").append(Styles.veryLargeIcon.toString());
		notification.setStyleName(sb.toString());
		notification.setIcon(icon);
		notification.setDelayMsec(-1);
		return notification;
	}
	
	private static void showGeneric(String caption, String description, Type type, 
			Resource icon, String... styles)
	{
		createGeneric(caption, description, type, icon, styles).show(Page.getCurrent());
	}
}
