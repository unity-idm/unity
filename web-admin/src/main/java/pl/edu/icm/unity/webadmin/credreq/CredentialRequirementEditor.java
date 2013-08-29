/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.credreq;

import java.util.Collection;
import java.util.HashSet;

import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.authn.CredentialRequirements;
import pl.edu.icm.unity.webui.common.DescriptionTextArea;
import pl.edu.icm.unity.webui.common.RequiredTextField;

import com.vaadin.data.fieldgroup.FieldGroup;
import com.vaadin.data.fieldgroup.FieldGroup.CommitException;
import com.vaadin.data.util.BeanItem;
import com.vaadin.ui.AbstractTextField;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.TwinColSelect;

/**
 * Allows to edit a credential requirement. Can be configured to edit an existing requirement (name is fixed,
 * updated credentials state combo is shown) or to create a new one (name can be chosen).
 * 
 * @author K. Benedyczak
 */
public class CredentialRequirementEditor extends FormLayout
{
	private UnityMessageSource msg;

	private AbstractTextField name;
	private DescriptionTextArea description;
	private TwinColSelect requiredCredentials;
	
	private FieldGroup binder;
	private BeanItem<CredentialRequirements> formItem;

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

		name = new RequiredTextField(msg);
		name.setCaption(msg.getMessage("CredentialRequirements.name"));
		addComponent(name);
		
		description = new DescriptionTextArea(msg.getMessage("CredentialRequirements.description"));
		addComponent(description);
		if (initial != null)
			description.setValue(initial.getDescription());
		
		requiredCredentials = new TwinColSelect(msg.getMessage("CredentialRequirements.credentials"));
		requiredCredentials.setLeftColumnCaption(msg.getMessage("CredentialRequirements.available"));
		requiredCredentials.setRightColumnCaption(msg.getMessage("CredentialRequirements.chosen"));
		requiredCredentials.setRequired(true);
		requiredCredentials.setRequiredError(msg.getMessage("fieldRequired"));
		requiredCredentials.setWidth(48, Unit.EM);
		for (CredentialDefinition cr: allCredentials)
			requiredCredentials.addItem(cr.getName());
		
		if (initial != null)
			requiredCredentials.setValue(initial.getRequiredCredentials());
		addComponent(requiredCredentials);
		
		CredentialRequirements cr = initial == null ? new CredentialRequirements(
				msg.getMessage("CredentialRequirements.defaultName"), "", new HashSet<String>()) : initial;
		formItem = new BeanItem<CredentialRequirements>(cr);
		if (initial != null)
			formItem.getItemProperty("name").setReadOnly(true);
		
		binder = new FieldGroup(formItem);
		binder.bind(name, "name");
		binder.bind(description, "description");
		binder.bind(requiredCredentials, "requiredCredentials");
	}
	
	public CredentialRequirements getCredentialRequirements() throws IllegalCredentialException
	{
		try
		{
			binder.commit();
		} catch (CommitException e)
		{
			throw new IllegalCredentialException("");
		}
		return formItem.getBean();
	}
}
