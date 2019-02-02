/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.webui.common;

import com.vaadin.server.Page;
import com.vaadin.shared.Position;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Shows notifications in tray
 * @author P.Piernik
 *
 */
public class NotificationTray
{
	public static void showSuccess(String caption, String description)
	{
		showGeneric(caption, description, ValoTheme.NOTIFICATION_SUCCESS);	
	}
	
	public static void showSuccess(String caption)
	{
		showGeneric(caption, null, ValoTheme.NOTIFICATION_SUCCESS);	
	}
	
	public static void showError(String caption, String description)
	{
		showGeneric(caption, description, ValoTheme.NOTIFICATION_FAILURE);	
	}
	
	public static void showError(String caption)
	{
		showGeneric(caption, null, ValoTheme.NOTIFICATION_FAILURE);	
	}
	
	private static void showGeneric(String caption, String description, String... styles)
	{
		createGeneric(caption, description, styles).show(Page.getCurrent());
	}
	
	private static Notification createGeneric(String caption, String description, String... styles)
	{
		Notification notification = new Notification(caption, description, Type.HUMANIZED_MESSAGE);
		StringBuilder sb = new StringBuilder(notification.getStyleName());
		for (String style: styles)
			sb.append(" ").append(style);
		notification.setStyleName(sb.toString());
		notification.setDelayMsec(2000);
		notification.setPosition(Position.BOTTOM_RIGHT);
		return notification;
	}
}
