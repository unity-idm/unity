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

import com.vaadin.server.Resource;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Embedded;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;

/**
 * Show informations about all endpoints
 * 
 * @author P. Piernik
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class EndpointsStatusComponent extends VerticalLayout
{

	private UnityMessageSource msg;

	private EndpointManagement endpointMan;

	private Table endpointsTable;

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

		initEndpointsTable();
		updateContent();

		addComponent(new Label("<b>Endpoints:</b>", ContentMode.HTML));
		addComponent(endpointsTable);

	}

	private void initEndpointsTable()
	{
		endpointsTable = new Table();

		endpointsTable.addContainerProperty("name", String.class, "");
		endpointsTable.addContainerProperty("description", String.class, "");
		endpointsTable.addContainerProperty("address", String.class, "");
		endpointsTable.addContainerProperty("type", String.class, "");
		endpointsTable.addContainerProperty("bindings", String.class, null);
		endpointsTable.addContainerProperty("authenticators", String.class, null);
		endpointsTable.addContainerProperty("status", Embedded.class, null);

		endpointsTable.setColumnHeader("name", "Name");
		endpointsTable.setColumnHeader("description", "Description");
		endpointsTable.setColumnHeader("address", "Address");
		endpointsTable.setColumnHeader("type", "Type");
		endpointsTable.setColumnHeader("bindings", "Bindings");
		endpointsTable.setColumnHeader("authenticators", "Authenticators");
		endpointsTable.setColumnHeader("status", "Status");

		endpointsTable.setSizeFull();
		endpointsTable.setSelectable(true);

	}

	private void updateContent()
	{
		endpointsTable.removeAllItems();
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

			StringBuilder bindings = new StringBuilder();

			for (String s : endpointDesc.getType().getSupportedBindings())
			{
				if (bindings.length() > 0)
					// bindings.append("<br/>");
					bindings.append(" | ");
				bindings.append(s);

			}

			StringBuilder auth = new StringBuilder();

			for (AuthenticatorSet s : endpointDesc.getAuthenticatorSets())
			{
				for (String a : s.getAuthenticators())
				{
					if (auth.length() > 0)
						// auth.append("<br/>");
						auth.append(" | ");
					auth.append(a);
				}
			}

			// Label bi = new Label(bindings.toString());
			// bi.setContentMode(ContentMode.HTML);
			//
			// Label au = new Label(auth.toString());
			// au.setContentMode(ContentMode.HTML);

			Resource st = Images.ok.getResource();

			endpointsTable.addItem(
					new Object[] { endpointDesc.getId(),
							endpointDesc.getDescription(),
							endpointDesc.getContextAddress(),
							endpointDesc.getType().getName(),
							bindings.toString(), auth.toString(),
							new Embedded("", st) }, null);

		}

	}

}
