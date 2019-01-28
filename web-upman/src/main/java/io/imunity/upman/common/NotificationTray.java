/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.upman.common;

import com.vaadin.server.Page;
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
	
	private static void showGeneric(String caption, String description, String... styles)
	{
		createGeneric(caption, description, styles).show(Page.getCurrent());
	}
	
	private static Notification createGeneric(String caption, String description, String... styles)
	{
		Notification notification = new Notification(caption, description, Type.TRAY_NOTIFICATION);
		StringBuilder sb = new StringBuilder(notification.getStyleName());
		for (String style: styles)
			sb.append(" ").append(style);
		notification.setStyleName(sb.toString());
		notification.setDelayMsec(2000);
		return notification;
	}
}
