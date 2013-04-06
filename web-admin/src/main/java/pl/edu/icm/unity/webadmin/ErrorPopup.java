/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin;

import com.vaadin.server.Page;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;


/**
 * Shows error notification. Exception stack trace is parsed to provide a meaningful information.
 * @author K. Benedyczak
 */
public class ErrorPopup
{
	public static void showError(String message, Exception e)
	{
		String description = getHumanMessage(e);
		Notification n = new Notification(message, description, Type.ERROR_MESSAGE);
		n.setDelayMsec(-1);
		n.show(Page.getCurrent());
	}
	
	public static String getHumanMessage(Throwable e)
	{
		StringBuilder sb = new StringBuilder();
		if (e.getMessage() != null)
			sb.append(e.getMessage());
		while (e.getCause() != null)
		{
			e = e.getCause();
			if (e.getMessage() == null)
				break;
			sb.append("<br>").append(e.getMessage());
		}
		return sb.toString();
	}
}
