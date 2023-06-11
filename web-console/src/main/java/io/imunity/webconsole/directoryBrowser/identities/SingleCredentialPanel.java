/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.directoryBrowser.identities;

import java.util.Collection;
import java.util.Optional;

import org.apache.logging.log4j.Logger;

import com.vaadin.ui.CheckBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.authn.CredentialDefinition;
import pl.edu.icm.unity.base.authn.CredentialType;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.authn.IllegalCredentialException;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.FormValidationRTException;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditor;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorContext;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistryV8;

/**
 * Single credential editor, assuming admin context and no initial credential.
 */
class SingleCredentialPanel extends VerticalLayout
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, SingleCredentialPanel.class);
	private CredentialEditorRegistryV8 credEditorReg;
	private CredentialManagement credMan;
	private MessageSource msg;
	private CheckBox setTheCredential;
	private CheckBox invalidate;
	private CredentialEditor credEditor;
	private ComponentsContainer credEditorComp;
	private CredentialDefinition toEdit;
	
	SingleCredentialPanel(MessageSource msg, 
			CredentialManagement credMan,
			CredentialEditorRegistryV8 credEditorReg,
			CredentialDefinition toEdit)
	{
		this.msg = msg;
		this.credMan = credMan;
		this.credEditorReg = credEditorReg;
		this.toEdit = toEdit;
		init();
	}

	private void init()
	{
		credEditor = credEditorReg.getEditor(toEdit.getTypeId());

		credEditorComp = credEditor.getEditor(CredentialEditorContext.builder()
				.withConfiguration(toEdit.getConfiguration())
				.withRequired(true)
				.withAdminMode(true)
				.build());

		VerticalLayout credEditorLayout = new VerticalLayout();
		credEditorLayout.setMargin(false);
		HorizontalLayout buttonsBar = new HorizontalLayout();

		setTheCredential = new CheckBox(msg.getMessage("NewEntityCredentialsPanel.setInitialCredential"));
		setTheCredential.addValueChangeListener(e -> {
			credEditorLayout.setEnabled(setTheCredential.getValue());
			buttonsBar.setEnabled(setTheCredential.getValue());
		});
		addComponent(setTheCredential);

		buttonsBar.setSpacing(true);
		buttonsBar.setMargin(false);
		invalidate = new CheckBox(msg.getMessage("NewEntityCredentialsPanel.requireChange"));
		if (supportsInvalidation(toEdit.getTypeId()))
			buttonsBar.addComponent(invalidate);

		addComponent(credEditorLayout);
		if (!isEmptyEditor())
		{
			credEditorLayout.addComponents(credEditorComp.getComponents());
			addComponent(buttonsBar);
		}

		credEditorLayout.setEnabled(false);
		buttonsBar.setEnabled(false);		
		setSpacing(true);
		setMargin(false);
	}
	
	boolean isEmptyEditor()
	{
		return credEditorComp.getComponents().length == 0;
	}
	
	private boolean supportsInvalidation(String credType)
	{
		try
		{
			Collection<CredentialType> credentialTypes = credMan.getCredentialTypes();
			for (CredentialType type : credentialTypes)
			{
				if (type.getName().equals(credType))
				{
					return type.isSupportingInvalidation();
				}
			}
		} catch (EngineException e)
		{
			log.warn("Can not get credential type " + credType, e);
		}
		return false;
	}

	Optional<ObtainedCredential> getCredential() throws FormValidationRTException
	{
		if (!setTheCredential.getValue())
			return Optional.empty();
		String secrets;
		try
		{
			secrets = credEditor.getValue();
		} catch (IllegalCredentialException e)
		{
			throw new FormValidationRTException(e);
		}
		return Optional.of(new ObtainedCredential(invalidate.getValue(), secrets, toEdit.getName()));
	}
	
	static class ObtainedCredential
	{
		final boolean setAsInvalid;
		final String secrets;
		final String credentialId;
		
		ObtainedCredential(boolean setAsInvalid, String secrets, String credentialId)
		{
			this.setAsInvalid = setAsInvalid;
			this.secrets = secrets;
			this.credentialId = credentialId;
		}
	}
}
