/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.credentials.password;

import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditorContext;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.IllegalCredentialException;
import pl.edu.icm.unity.stdext.credential.pass.PasswordCredential;

public class PasswordEditorComponent extends VerticalLayout implements Focusable<PasswordEditorComponent>
{

	private final PasswordFieldsComponent fieldsComponent;
	private final PasswordQualityComponent qualityComponent;
	private int tabIndex;
	
	public PasswordEditorComponent(MessageSource msg, CredentialEditorContext context, PasswordCredential config,
	                               NotificationPresenter notificationPresenter)
	{
		qualityComponent = new PasswordQualityComponent(msg, config, context);
		fieldsComponent = new PasswordFieldsComponent(msg, context, config, qualityComponent::onNewPassword, notificationPresenter);
		
		HorizontalLayout root = new HorizontalLayout();
		root.setMargin(false);
		root.setSpacing(false);
		root.setPadding(false);
		root.add(fieldsComponent, qualityComponent);
		add(root);
		setPadding(false);
	}

	@Override
	public void focus()
	{
		fieldsComponent.focus();
	}
	
	public void setLabel(String label)
	{
		fieldsComponent.setLabel(label);
	}

	public String getValue() throws IllegalCredentialException
	{
		return fieldsComponent.getValue();
	}

	public void setCredentialError(EngineException error)
	{
		fieldsComponent.setCredentialError(error);
	}

	public void disablePasswordRepeat()
	{
		fieldsComponent.disablePasswordRepeat();
	}

	@Override
	public int getTabIndex()
	{
		return tabIndex;
	}

	@Override
	public void setTabIndex(int tabIndex)
	{
		this.tabIndex = tabIndex;
	}
}
