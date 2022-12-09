/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin23.shared.endpoint.plugins.credentials.password;

import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.imunity.vaadin23.elements.NotificationPresenter;
import io.imunity.vaadin23.shared.endpoint.plugins.credentials.CredentialEditorContext;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
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
		getStyle().set("margin-bottom", "-5em");
		getStyle().set("height", "15em");
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
