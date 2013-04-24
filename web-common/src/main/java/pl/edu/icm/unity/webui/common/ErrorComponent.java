/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import pl.edu.icm.unity.exceptions.AuthorizationException;

import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

/**
 * The component should be used instead of a ordinary component, when there is problem
 * retrieving the data for the ordinary component, as in the case of authorization error.
 * @author K. Benedyczak
 */
public class ErrorComponent extends VerticalLayout
{
	public ErrorComponent(Exception error)
	{
		if (error instanceof AuthorizationException)
		{
			Label authzError = new Label();
			authzError.setValue(error.getMessage());
			authzError.setIcon(Images.noAuthzGrp.getResource());
		} else
		{
			Label otherError = new Label();
			otherError.setValue(ErrorPopup.getHumanMessage(error));
			otherError.setContentMode(ContentMode.HTML);
			otherError.setIcon(Images.noAuthzGrp.getResource());
		}
	}
}
