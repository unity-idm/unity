/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.identities;

import java.util.Collection;
import java.util.Optional;

import org.apache.logging.log4j.Logger;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import io.imunity.vaadin.endpoint.common.plugins.ComponentsContainer;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditor;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditorContext;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditorRegistry;
import pl.edu.icm.unity.base.authn.CredentialDefinition;
import pl.edu.icm.unity.base.authn.CredentialType;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.authn.IllegalCredentialException;


class SingleCredentialPanel extends VerticalLayout
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, SingleCredentialPanel.class);
	private final CredentialEditorRegistry credEditorReg;
	private final CredentialManagement credMan;
	private final MessageSource msg;
	private final CredentialDefinition toEdit;
	private Checkbox setTheCredential;
	private Checkbox invalidate;
	private CredentialEditor credEditor;
	private ComponentsContainer credEditorComp;

	SingleCredentialPanel(MessageSource msg, 
			CredentialManagement credMan,
			CredentialEditorRegistry credEditorReg,
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

		setTheCredential = new Checkbox(msg.getMessage("NewEntityCredentialsPanel.setInitialCredential"));
		setTheCredential.addValueChangeListener(e -> {
			credEditorLayout.setEnabled(setTheCredential.getValue());
			buttonsBar.setEnabled(setTheCredential.getValue());
		});
		add(setTheCredential);

		buttonsBar.setSpacing(true);
		buttonsBar.setMargin(false);
		invalidate = new Checkbox(msg.getMessage("NewEntityCredentialsPanel.requireChange"));
		if (supportsInvalidation(toEdit.getTypeId()))
			buttonsBar.add(invalidate);

		add(credEditorLayout);
		if (isNotEmptyEditor())
		{
			credEditorLayout.add(credEditorComp.getComponents());
			add(buttonsBar);
		}

		credEditorLayout.setEnabled(false);
		buttonsBar.setEnabled(false);		
		setSpacing(true);
		setMargin(false);
	}
	
	boolean isNotEmptyEditor()
	{
		return credEditorComp.getComponents().length != 0;
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
