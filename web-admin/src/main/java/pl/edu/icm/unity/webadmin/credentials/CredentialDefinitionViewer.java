/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.credentials;

import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.webui.common.DescriptionTextArea;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorFactory;

/**
 * Shows a single {@link CredentialDefinition}
 * @author K. Benedyczak
 */
public class CredentialDefinitionViewer extends FormLayout
{
	private UnityMessageSource msg;
	
	private Label name;
	private DescriptionTextArea description;
	private Label type;
	private Panel typeSpecific;
	
	public CredentialDefinitionViewer(UnityMessageSource msg)
	{
		super();
		this.msg = msg;
		initUI();
	}
	
	private void initUI()
	{
		name = new Label();
		name.setCaption(msg.getMessage("CredentialDefinition.name"));
		description = new DescriptionTextArea(msg.getMessage("CredentialDefinition.description"), true, "");
		type = new Label();
		type.setCaption(msg.getMessage("CredentialDefinition.type"));
		typeSpecific = new Panel(msg.getMessage("CredentialDefinition.typeSettings"));
		addComponents(name, description, type, typeSpecific);
		setEmpty();
	}
	
	private void setEmpty()
	{
		name.setValue(msg.getMessage("CredentialDefinition.notSelected"));
		description.setValue("");
		type.setValue("");
		typeSpecific.setContent(new Label());
		typeSpecific.setVisible(false);
	}
	
	public void setInput(CredentialDefinition cd, CredentialEditorFactory cdFactory)
	{
		if (cd == null)
		{
			setEmpty();
			return;
		}
		
		name.setValue(cd.getName());
		description.setValue(cd.getDescription());
		type.setValue(cd.getTypeId());
		typeSpecific.setVisible(true);
		
		pl.edu.icm.unity.webui.common.credentials.CredentialDefinitionViewer viewer = 
				cdFactory.creteCredentialDefinitionViewer();
		typeSpecific.setContent(viewer.getViewer(cd.getJsonConfiguration()));
	}
}
