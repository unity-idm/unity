/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.authentication.credentialReq;

import java.util.Collection;
import java.util.HashSet;

import com.vaadin.data.Binder;
import com.vaadin.ui.TextField;
import com.vaadin.ui.TwinColSelect;

import pl.edu.icm.unity.base.authn.CredentialDefinition;
import pl.edu.icm.unity.base.authn.CredentialRequirements;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.IllegalCredentialException;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.widgets.DescriptionTextField;

/**
 * Allows to edit a credential requirement. Can be configured to edit an existing requirement (name is fixed,
 * updated credentials state combo is shown) or to create a new one (name can be chosen).
 * 
 * @author K. Benedyczak
 */
class CredentialRequirementEditor extends CompactFormLayout
{
	private MessageSource msg;
	private Binder<CredentialRequirements> binder;

	CredentialRequirementEditor(MessageSource msg, Collection<CredentialDefinition> allCredentials, 
			CredentialRequirements initial)
	{
		this.msg = msg;
		init(initial, allCredentials);
	}
	
	CredentialRequirementEditor(MessageSource msg, Collection<CredentialDefinition> allCredentials)
	{
		this(msg, allCredentials, null);
	}
	
	private void init(CredentialRequirements initial, Collection<CredentialDefinition> allCredentials)
	{
		setWidth(100, Unit.PERCENTAGE);

		TextField name = new TextField(msg.getMessage("CredentialRequirements.name"));
		addComponent(name);
		if (initial != null)
			name.setReadOnly(true);
		
		DescriptionTextField description = new DescriptionTextField(msg);
		addComponent(description);
		
		TwinColSelect<String> requiredCredentials = new TwinColSelect<>(
				msg.getMessage("CredentialRequirements.credentials"));
		requiredCredentials.setLeftColumnCaption(msg.getMessage("CredentialRequirements.available"));
		requiredCredentials.setRightColumnCaption(msg.getMessage("CredentialRequirements.chosen"));
		requiredCredentials.setWidth(48, Unit.EM);
		requiredCredentials.setRows(5);
		requiredCredentials.setItems(allCredentials.stream().map(cr -> cr.getName()));
				
		addComponent(requiredCredentials);
		
		CredentialRequirements cr = initial == null ? new CredentialRequirements(
				msg.getMessage("CredentialRequirements.defaultName"), "", new HashSet<>()) : initial;
		binder = new Binder<>(CredentialRequirements.class);
		binder.forField(name).asRequired(msg.getMessage("fieldRequired")).bind("name");
		binder.bind(description, "description");
		binder.bind(requiredCredentials, "requiredCredentials");
		binder.setBean(cr);
	}
	
	CredentialRequirements getCredentialRequirements() throws IllegalCredentialException
	{
		if (!binder.isValid())
			throw new IllegalCredentialException("");
		return binder.getBean();
	}
}
