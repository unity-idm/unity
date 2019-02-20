/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webadmin.credentials;

import com.vaadin.ui.Label;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorFactory;
import pl.edu.icm.unity.webui.common.i18n.I18nLabel;
import pl.edu.icm.unity.webui.common.safehtml.SafePanel;

/**
 * Shows a single {@link CredentialDefinition}
 * @author K. Benedyczak
 */
public class CredentialDefinitionViewer extends CompactFormLayout
{
	private UnityMessageSource msg;
	
	private Label name;
	private I18nLabel displayedName;
	private I18nLabel description;
	private Label type;
	private SafePanel typeSpecific;
	
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
		displayedName = new I18nLabel(msg, msg.getMessage("displayedNameF"));
		description = new I18nLabel(msg, msg.getMessage("descriptionF"));
		type = new Label();
		type.setCaption(msg.getMessage("CredentialDefinition.type"));
		typeSpecific = new SafePanel(msg.getMessage("CredentialDefinition.typeSettings"));
		addComponents(name, displayedName, description, type, typeSpecific);
		setContentVisible(false);
	}
	
	private void setContentVisible(boolean how)
	{
		name.setVisible(how);
		displayedName.setVisible(how);
		description.setVisible(how);
		type.setVisible(how);
		typeSpecific.setVisible(how);
	}
	
	public void setInput(CredentialDefinition cd, CredentialEditorFactory cdFactory)
	{
		if (cd == null)
		{
			setContentVisible(false);
			return;
		}
		setContentVisible(true);
		
		name.setValue(cd.getName());
		displayedName.setValue(cd.getDisplayedName());
		description.setValue(cd.getDescription());
		type.setValue(cd.getTypeId());
		
		pl.edu.icm.unity.webui.common.credentials.CredentialDefinitionViewer viewer = 
				cdFactory.creteCredentialDefinitionViewer();
		typeSpecific.setContent(viewer.getViewer(cd.getConfiguration()));
	}
}
