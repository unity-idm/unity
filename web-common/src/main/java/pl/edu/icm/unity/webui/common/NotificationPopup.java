/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import java.time.Duration;

import org.apache.logging.log4j.Logger;

import com.vaadin.server.Page;
import com.vaadin.server.Resource;
import com.vaadin.shared.Position;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.themes.ValoTheme;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AuthorizationException;
import pl.edu.icm.unity.webui.exceptions.ControllerException;


/**
 * Shows notification. Exception stack trace is parsed to provide a meaningful information in case
 * of errors. Based on Vaadin's {@link Notification}, this class ensures that it is used in the same way in the 
 * whole system. 
 * @author K. Benedyczak
 */
public class NotificationPopup
{
	private static final Duration NOTIFICATION_AUTOCLOSE_AFTER = Duration.ofSeconds(5);
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

	public static void showWarningAutoClosing(String caption, String description, Runnable afterCloseHandler)
	{
		showAutoClosing(getWarningNotificationPlain(caption, description), Position.MIDDLE_CENTER, afterCloseHandler);
	}
	
	public static void showErrorAutoClosing(String caption, String description, Runnable afterCloseHandler)
	{
		showAutoClosing(getErrorNotificationPlain(caption, description), Position.TOP_CENTER, afterCloseHandler);
	}

	public static void showAssistiveAutoClosing(String caption, String description)
	{		
		showAutoClosing(getAssistiveNotificationPlain(caption, description), Position.TOP_CENTER, () -> {});
	}
	
	public static void showErrorAutoClosing(String caption, String description)
	{
		showAutoClosing(getErrorNotificationPlain(caption, description), Position.TOP_CENTER, () -> {});
	}
	
	private static void showAutoClosing(Notification notification, Position position, Runnable afterCloseHandler)
	{
		notification.setDelayMsec((int)NOTIFICATION_AUTOCLOSE_AFTER.toMillis());
		StringBuilder sb = new StringBuilder(notification.getStyleName());
		sb.append(" ").append(Styles.veryLargeIcon.toString());
		notification.setPosition(position);
		notification.addCloseListener(e -> afterCloseHandler.run());
		notification.show(Page.getCurrent());
	}
	
	private static Notification getWarningNotificationPlain(String caption, String description)
	{
		Notification notification = new Notification(caption, description, Type.WARNING_MESSAGE);
		notification.setIcon(Images.info.getResource());
		return notification;
	}
	
	private static Notification getAssistiveNotificationPlain(String caption, String description)
	{
		Notification notification = new Notification(caption, description, Type.ASSISTIVE_NOTIFICATION);
		notification.setIcon(Images.info.getResource());
		return notification;
	}
	
	private static Notification getErrorNotificationPlain(String caption, String description)
	{
		Notification notification = new Notification(caption, description, Type.ERROR_MESSAGE);
		notification.setIcon(Images.error.getResource());
		return notification;
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
	
	public static void showError(MessageSource msg, ControllerException exception)
	{
		String description = exception.getCause() != null ? getHumanMessage(exception.getCause()) : "";

		if (exception.getDetails() != null && !exception.getDetails().isEmpty())
		{

			description = description != null && !description.trim().isEmpty()
					? exception.getDetails() + ", " + description
					: exception.getDetails();
		}

		if (description.trim().isEmpty())
		{
			description = msg.getMessage("Generic.formErrorHint");
		}

		log.warn("Error popup showed an error to the user: " + exception.getCaption());
		log.info("What's more there was an exception attached which caused an error:", exception);

		if (exception.getType() == pl.edu.icm.unity.webui.exceptions.ControllerException.Type.ERROR)
		{
			showError(exception.getCaption(), description);
		} else
		{
			showNotice(exception.getCaption(), description);
		}
	}

	public static void showFormError(MessageSource msg)
	{
		showError(msg.getMessage("Generic.formError"), msg.getMessage("Generic.formErrorHint"));
	}

	public static void showFormError(MessageSource msg, String detail)
	{
		showError(msg.getMessage("Generic.formError"), detail);
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
	
	public static void showError(MessageSource msg, String message, Exception e)
	{
		String description = getHumanMessage(e);
		if (description.trim().isEmpty())
			description = msg.getMessage("Generic.formErrorHint");

		log.warn("Error popup showed an error to the user: " + message);
		log.info("What's more there was an exception attached which caused an error:", e);
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
			if (!(e instanceof EngineException || e instanceof ConfigurationException))
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
