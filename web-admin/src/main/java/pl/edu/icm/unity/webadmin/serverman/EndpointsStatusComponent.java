/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webadmin.serverman;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.EndpointManagement;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.authn.AuthenticatorSet;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;
import pl.edu.icm.unity.webui.common.ErrorPopup;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;

import com.vaadin.server.Resource;
import com.vaadin.server.ThemeResource;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Embedded;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;

/**
 * Show information about all endpoints
 * 
 * @author P. Piernik
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class EndpointsStatusComponent extends VerticalLayout
{

	private UnityMessageSource msg;

	private EndpointManagement endpointMan;

	@Autowired
	public EndpointsStatusComponent(UnityMessageSource msg, EndpointManagement endpointMan)
	{

		this.msg = msg;
		this.endpointMan = endpointMan;
		initUI();
	}

	private void initUI()
	{
		setCaption(msg.getMessage("EndpointsStatus.caption"));
		setMargin(true);
		setSpacing(true);
		Label e = new Label("Endpoints:");
		e.addStyleName(Styles.bold.toString());
		addComponent(e);
		updateContent();

	}

	private void updateContent()
	{

		List<EndpointDescription> endpoints = null;
		try
		{
			endpoints = endpointMan.getEndpoints();
		} catch (EngineException e)
		{
			ErrorPopup.showError(msg, msg.getMessage("error"),
					msg.getMessage("EndpointsStatus.cannotLoadEndpoints"));
			return;
		}

		for (EndpointDescription endpointDesc : endpoints)
		{
			
			addComponent(new SingleEndpointComponent(endpointDesc));
			addComponent(new Label());
			Label line = new Label();
			line.addStyleName(Styles.horizontalLine.toString());
			addComponent(line);
			

		}

	}

}
