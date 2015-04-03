/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.credreq;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.authn.CredentialRequirements;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.DescriptionTextArea;
import pl.edu.icm.unity.webui.common.SmallTable;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;

/**
 * Allows to inspect a single {@link CredentialRequirements}
 * @author K. Benedyczak
 */
public class CredentialRequirementViewer extends CompactFormLayout
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
		
		credentials = new SmallTable(msg.getMessage("CredentialRequirements.credentials"));
		credentials.setHeight(12, Unit.EM);
		credentials.setWidth(90, Unit.PERCENTAGE);
		credentials.addContainerProperty(msg.getMessage("CredentialRequirements.credentialsHeader"), 
				String.class, null);
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
		credentials.removeAllItems();
		for (String cred: cr.getRequiredCredentials())
			credentials.addItem(new Object[] {cred}, cred);
	}
}
