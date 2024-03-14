/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.auth.extensions.credreset;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.plugins.ComponentsContainer;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditor;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditorContext;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.IllegalCredentialException;
import io.imunity.vaadin.endpoint.common.plugins.credentials.MissingCredentialException;

/**
 * The last step of credential reset pipeline. On this UI the user must provide the new credential.
 */
public class CredentialResetNewCredentialUI extends CredentialResetLayout
{
	private final MessageSource msg;
	private final NotificationPresenter notificationPresenter;
	private final CredentialEditor credEditor;
	private final NewCredentialConsumer newCredentialConsumer;
	private final String credentialConfiguration;
	private final Long entityId;
	private final Runnable cancelCallback;
	private final CredentialResetFlowConfig credResetConfig;
	
	public CredentialResetNewCredentialUI(CredentialResetFlowConfig credResetConfig, CredentialEditor credEditor,
	                                      NewCredentialConsumer newCredentialConsumer, String credentialConfiguration,
										  NotificationPresenter notificationPresenter, Long entityId,
										  String setCredentialLabel)
	{
		super(credResetConfig);
		this.credResetConfig = credResetConfig;
		this.msg = credResetConfig.msg;
		this.credEditor = credEditor;
		this.newCredentialConsumer = newCredentialConsumer;
		this.notificationPresenter = notificationPresenter;
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
		ret.setPadding(false);
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
		ret.add(credEditorComponents);
		for (Component c: credEditorComponents)
			if (c instanceof Focusable)
				((Focusable) c).focus();
			
		
		Component buttons = getButtonsBar(setCredentialLabel, 
				this::onConfirm, msg.getMessage("cancel"), cancelCallback);
		ret.add(buttons);
		
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
			notificationPresenter.showError(e.getMessage(), "");
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
			notificationPresenter.showError(e.getMessage(), "");
			credEditor.setCredentialError(null);
		}
	}
}
