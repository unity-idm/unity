/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.credreq;

import java.util.Collection;
import java.util.HashSet;

import com.vaadin.data.Binder;
import com.vaadin.ui.TextField;
import com.vaadin.ui.TwinColSelect;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.authn.CredentialRequirements;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.DescriptionTextArea;

/**
 * Allows to edit a credential requirement. Can be configured to edit an existing requirement (name is fixed,
 * updated credentials state combo is shown) or to create a new one (name can be chosen).
 * 
 * @author K. Benedyczak
 */
public class CredentialRequirementEditor extends CompactFormLayout
{
	private UnityMessageSource msg;
	private Binder<CredentialRequirements> binder;

	public CredentialRequirementEditor(UnityMessageSource msg, Collection<CredentialDefinition> allCredentials, 
			CredentialRequirements initial)
	{
		this.msg = msg;
		init(initial, allCredentials);
	}
	
	public CredentialRequirementEditor(UnityMessageSource msg, Collection<CredentialDefinition> allCredentials)
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
		
		DescriptionTextArea description = new DescriptionTextArea(
				msg.getMessage("CredentialRequirements.description"));
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
	
	public CredentialRequirements getCredentialRequirements() throws IllegalCredentialException
	{
		if (!binder.isValid())
			throw new IllegalCredentialException("");
		return binder.getBean();
	}
}
