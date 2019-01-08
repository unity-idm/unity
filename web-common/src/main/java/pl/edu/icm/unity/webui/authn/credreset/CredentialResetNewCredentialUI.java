/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.credreset;

import com.vaadin.ui.Component;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.webui.authn.credreset.password.PasswordCredentialResetController.NewCredentialConsumer;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditor;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorContext;
import pl.edu.icm.unity.webui.common.credentials.MissingCredentialException;

/**
 * The last step of credential reset pipeline. On this UI the user must provide the new credential.
 * 
 * @author K. Benedyczak
 */
public class CredentialResetNewCredentialUI extends CredentialResetLayout
{
	private UnityMessageSource msg;
	private CredentialEditor credEditor;
	private NewCredentialConsumer newCredentialConsumer;
	private String credentialConfiguration;
	private Long entityId;
	private Runnable cancelCallback;
	private CredentialResetFlowConfig credResetConfig;
	
	public CredentialResetNewCredentialUI(CredentialResetFlowConfig credResetConfig, CredentialEditor credEditor,
			NewCredentialConsumer newCredentialConsumer, String credentialConfiguration,
			Long entityId, String setCredentialLabel)
	{
		super(credResetConfig);
		this.credResetConfig = credResetConfig;
		this.msg = credResetConfig.msg;
		this.credEditor = credEditor;
		this.newCredentialConsumer = newCredentialConsumer;
		this.credentialConfiguration = credentialConfiguration;
		this.entityId = entityId;
		this.cancelCallback = credResetConfig.cancelCallback;
		initUI(msg.getMessage("CredentialReset.updateCredentialTitle"), 
				getContents(setCredentialLabel));
	}

	private Component getContents(String setCredentialLabel)
	{
		VerticalLayout ret = new VerticalLayout();
		ret.setMargin(false);
		ret.setSpacing(true);
		ret.setWidth(MAIN_WIDTH_EM, Unit.EM);
		ComponentsContainer componentContainer = credEditor.getEditor(CredentialEditorContext.builder()
				.withConfiguration(credentialConfiguration)
				.withRequired(true)
				.withEntityId(entityId)
				.withCustomWidth(MAIN_WIDTH_EM)
				.withCustomWidthUnit(Unit.EM)
				.withShowLabelInline(credResetConfig.compactLayout)
				.build());
		Component[] credEditorComponents = componentContainer.getComponents();
		ret.addComponents(credEditorComponents);
		for (Component c: credEditorComponents)
			if (c instanceof Focusable)
				((Focusable) c).focus();
			
		
		Component buttons = getButtonsBar(setCredentialLabel, 
				this::onConfirm, msg.getMessage("cancel"), cancelCallback);
		ret.addComponent(buttons);
		
		return ret;
	}

	protected void onConfirm()
	{
		String updatedValue;
		try
		{
			updatedValue = credEditor.getValue();
		} catch (MissingCredentialException mc)
		{
			return;
		} catch (IllegalCredentialException e)
		{
			NotificationPopup.showError(e.getMessage(), "");
			return;
		}
		
		try
		{
			newCredentialConsumer.acceptNewCredential(updatedValue);
		} catch (EngineException e)
		{
			credEditor.setCredentialError(e);
		} catch (Exception e)
		{
			NotificationPopup.showError(e.getMessage(), "");
			credEditor.setCredentialError(null);
		}
	}
}
