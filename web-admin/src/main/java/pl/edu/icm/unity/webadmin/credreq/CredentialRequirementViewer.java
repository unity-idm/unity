/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.credreq;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.authn.CredentialRequirements;
import pl.edu.icm.unity.webui.common.DescriptionTextArea;

/**
 * Allows to inspect a single {@link CredentialRequirements}
 * @author K. Benedyczak
 */
public class CredentialRequirementViewer extends FormLayout
{
	private UnityMessageSource msg;
	
	private Label name;
	private DescriptionTextArea description;
	private Table credentials;
	
	public CredentialRequirementViewer(UnityMessageSource msg)
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
		
		description = new DescriptionTextArea(msg.getMessage("CredentialRequirements.description"), true, "");
		addComponent(description);
		
		credentials = new Table(msg.getMessage("CredentialRequirements.credentials"));
		credentials.setHeight(12, Unit.EM);
		credentials.setWidth(90, Unit.PERCENTAGE);
		credentials.addContainerProperty(msg.getMessage("CredentialRequirements.credentialsHeader"), 
				String.class, null);
		addComponent(credentials);
		setComponentAlignment(credentials, Alignment.TOP_LEFT);
		
		setEmpty();
	}
	
	private void setEmpty()
	{
		name.setValue(msg.getMessage("CredentialRequirements.notSelected"));
		description.setValue("");
		credentials.removeAllItems();
		credentials.setVisible(false);
	}
	
	public void setInput(CredentialRequirements cr)
	{
		if (cr == null)
		{
			setEmpty();
			return;
		}
		
		name.setValue(cr.getName());
		description.setValue(cr.getDescription());
		credentials.removeAllItems();
		credentials.setVisible(true);
		for (String cred: cr.getRequiredCredentials())
			credentials.addItem(new Object[] {cred}, cred);
	}
}
