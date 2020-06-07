/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.credreq;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.Label;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.types.authn.CredentialRequirements;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.SmallGrid;

/**
 * Allows to inspect a single {@link CredentialRequirements}
 * @author K. Benedyczak
 */
public class CredentialRequirementViewer extends CompactFormLayout
{
	private MessageSource msg;
	
	private Label name;
	private Label description;
	private Grid<String> credentials;
	
	public CredentialRequirementViewer(MessageSource msg)
	{
		super();
		this.msg = msg;
		
		initUI();
	}
	
	private void initUI()
	{
		name = new Label();
		name.setCaption(msg.getMessage("CredentialRequirements.name"));
		addComponent(name);
		
		description = new Label();
		description.setCaption(msg.getMessage("descriptionF"));
		addComponent(description);
		
		credentials = new SmallGrid<>(msg.getMessage("CredentialRequirements.credentials"));
		credentials.setHeight(12, Unit.EM);
		credentials.setWidth(90, Unit.PERCENTAGE);
		credentials.addColumn(a -> a)
			.setCaption(msg.getMessage("CredentialRequirements.credentialsHeader"));
		credentials.setSelectionMode(SelectionMode.NONE);
		addComponent(credentials);
		setComponentAlignment(credentials, Alignment.TOP_LEFT);
		
		setContentVisible(false);
	}
	
	private void setContentVisible(boolean how)
	{
		name.setVisible(how);
		description.setVisible(how);
		credentials.setVisible(how);
	}
	
	public void setInput(CredentialRequirements cr)
	{
		if (cr == null)
		{
			setContentVisible(false);
			return;
		}
		
		setContentVisible(true);
		name.setValue(cr.getName());
		description.setValue(cr.getDescription());
		credentials.setItems(cr.getRequiredCredentials());
	}
}
