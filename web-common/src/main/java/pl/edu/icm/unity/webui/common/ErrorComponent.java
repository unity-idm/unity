/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import org.apache.logging.log4j.Logger;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.webui.common.safehtml.HtmlSimplifiedLabel;

/**
 * The component should be used instead of a ordinary component, when there is problem
 * retrieving the data for the ordinary component, as in the case of authorization error.
 * @author K. Benedyczak
 */
public class ErrorComponent extends FormLayout
{
	private Logger log = Log.getLogger(Log.U_SERVER_WEB, ErrorComponent.class);
	public enum Level {error, warning}
	
	public ErrorComponent()
	{
		setMargin(true);
		setSizeFull();
	}
	
	public void setError(String description, Exception error)
	{
		Label errorL = getGeneric(description + ": " + NotificationPopup.getHumanMessage(error), 
				Images.error, Styles.error); 
		if (error instanceof AuthorizationException)
			log.debug("Error component initialized with the authZ error: " + description);
		else
			log.debug("Error component initialized with the error with exception. Description: " + description, 
					error);
		addCommon(errorL);
	}

	public void setMessage(String message, Level level)
	{
		if (level == Level.error)
			setError(message);
		else if (level == Level.warning)
			setWarning(message);
	}

	public void setError(String error)
	{
		addComponent(getGeneric(error, Images.error, Styles.error));
	}
		
	public void setWarning(String warning)
	{
		addComponent(getGeneric(warning, Images.warn, Styles.emphasized));
	}
	
	private Label getGeneric(String msg, Images icon, Styles style)
	{
		HtmlSimplifiedLabel label = new HtmlSimplifiedLabel(msg);
		label.setCaptionAsHtml(true);
		label.setWidth(100, Unit.PERCENTAGE);
		label.setCaption(icon.getHtml());
		label.addStyleName(style.toString());
		label.addStyleName(Styles.largeIcon.toString());
		return label;
	}
	
	private void addCommon(Label errorL)
	{
		addComponent(errorL);
		setComponentAlignment(errorL, Alignment.MIDDLE_CENTER);
	}
}
