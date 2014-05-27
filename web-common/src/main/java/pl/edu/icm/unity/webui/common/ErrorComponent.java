/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.server.utils.Log;

import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;

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
		Label errorL = new Label();
		errorL.setStyleName(Styles.error.toString());
		errorL.setIcon(Images.error32.getResource());
		errorL.setValue(description + ": " + ErrorPopup.getHumanMessage(error));
		errorL.setContentMode(ContentMode.HTML);
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
		Label errorL = new Label();
		errorL.setStyleName(Styles.error.toString());
		errorL.setIcon(Images.error32.getResource());
		errorL.setValue(error);
		addCommon(errorL);
	}
		
	public void setWarning(String warning)
	{
		Label errorL = new Label();
		errorL.setStyleName(Styles.italic.toString());
		errorL.setContentMode(ContentMode.HTML);
		errorL.setIcon(Images.warn32.getResource());
		
		errorL.setValue(warning);
		addCommon(errorL);
	}

	private void addCommon(Label errorL)
	{
		addComponent(errorL);
		setComponentAlignment(errorL, Alignment.MIDDLE_CENTER);
	}
}
