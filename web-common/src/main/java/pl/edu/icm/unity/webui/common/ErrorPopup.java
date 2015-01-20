/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.common.safehtml.HtmlTag;

import com.vaadin.event.MouseEvents.ClickEvent;
import com.vaadin.event.MouseEvents.ClickListener;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.VerticalLayout;


/**
 * Shows error notification. Exception stack trace is parsed to provide a meaningful information.
 * @author K. Benedyczak
 */
public class ErrorPopup
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, ErrorPopup.class);
	
	public static void showNotice(UnityMessageSource msg, String caption, String description)
	{
		CustomNotificationDialog dialog = new CustomNotificationDialog(msg, msg.getMessage("notice"), 
				caption, description, Type.HUMANIZED_MESSAGE);
		dialog.show();
	}

	public static void showError(UnityMessageSource msg, String caption, String description)
	{
		CustomNotificationDialog dialog = new CustomNotificationDialog(msg, msg.getMessage("error"), 
				caption, description, Type.ERROR_MESSAGE);
		dialog.show();
	}

	public static void showFormError(UnityMessageSource msg)
	{
		showError(msg, msg.getMessage("Generic.formError"), msg.getMessage("Generic.formErrorHint"));
	}

	public static void showError(UnityMessageSource msg, String message, Exception e)
	{
		String description = getHumanMessage(e);
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
		if (e.getMessage() != null)
			sb.append(e.getMessage());
		while (e.getCause() != null)
		{
			e = e.getCause();
			if (e.getMessage() == null)
				break;
			sb.append(separator).append(e.getMessage());
		}
		return sb.toString();
	}
	
	
	public static class CustomNotificationDialog extends AbstractDialog
	{
		private String header;
		private String message;
		private Notification.Type type;
		
		public CustomNotificationDialog(UnityMessageSource msg, String caption, String header, String message,
				Notification.Type type)
		{
			super(msg, caption, msg.getMessage("close"));
			this.header = header;
			this.message = message;
			this.type = type;
			this.lightweightWrapperPanel = true;
		}

		@Override
		protected Component getContents() throws Exception
		{
			HorizontalLayout main = new HorizontalLayout();
			main.setSpacing(true);
			addClickListener(new ClickListener()
			{
				@Override
				public void click(ClickEvent event)
				{
					close();
				}
			});
			Image img = new Image();
			if (type == Type.ERROR_MESSAGE)
			{
				img.setSource(Images.stderror64.getResource());
			} else if (type == Type.WARNING_MESSAGE)
			{
				img.setSource(Images.stdwarn64.getResource());
			} else
			{
				img.setSource(Images.stdinfo64.getResource());
			}
			main.addComponent(img);
			main.setComponentAlignment(img, Alignment.MIDDLE_CENTER);
			
			main.addComponent(HtmlTag.hspaceEm(4));
			
			VerticalLayout right = new VerticalLayout();
			right.setSpacing(true);
			if (header != null)
			{
				Label headerL = new Label(header);
				headerL.addStyleName(Styles.textXLarge.toString());
				headerL.addStyleName(Styles.bold.toString());
				headerL.setWidth(20, Unit.EM);
				if (type == Type.ERROR_MESSAGE)
					headerL.addStyleName(Styles.error.toString());
				right.addComponent(headerL);
				right.addComponent(new Label(""));
				right.setComponentAlignment(headerL, Alignment.MIDDLE_CENTER);
			}
			
			if (message != null)
			{
				Label msgL = new Label(message);
				msgL.addStyleName(Styles.textLarge.toString());
				msgL.setWidth(30, Unit.EM);
				right.addComponent(msgL);
			}
			main.addComponent(right);
			
			return main;
		}

		@Override
		protected void onConfirm()
		{
			close();
		}
	}
}
