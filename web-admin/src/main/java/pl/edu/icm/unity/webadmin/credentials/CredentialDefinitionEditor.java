/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.credentials;

import java.util.Set;

import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.authn.LocalAuthenticationState;
import pl.edu.icm.unity.webui.common.DescriptionTextArea;
import pl.edu.icm.unity.webui.common.EnumComboBox;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorFactory;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.fieldgroup.FieldGroup;
import com.vaadin.data.fieldgroup.FieldGroup.CommitException;
import com.vaadin.data.util.BeanItem;
import com.vaadin.ui.AbstractTextField;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextField;

/**
 * Allows to edit a credential definition. Can be configured to edit an existing definition (name and is fixed,
 * updated credentials state combo is shown) or to create a new one (name can be chosen).
 * 
 * @author K. Benedyczak
 */
public class CredentialDefinitionEditor extends FormLayout
{
	private UnityMessageSource msg;
	private CredentialEditorRegistry credentialEditorReg;
	private AbstractTextField name;
	private DescriptionTextArea description;
	private EnumComboBox<LocalAuthenticationState> newAuthnState; 
	private ComboBox credentialType;
	private Panel credentialEditorPanel;
	private pl.edu.icm.unity.webui.common.credentials.CredentialDefinitionEditor cdEd;
	
	private FieldGroup binder;
	private BeanItem<CredentialDefinition> formItem;
	
	public CredentialDefinitionEditor(UnityMessageSource msg, CredentialEditorRegistry credentialEditorReg,
			CredentialDefinition initial)
	{
		this.msg = msg;
		this.credentialEditorReg = credentialEditorReg;
		init(initial, credentialEditorReg);
	}
	
	public CredentialDefinitionEditor(UnityMessageSource msg, CredentialEditorRegistry credentialEditorReg)
	{
		this(msg, credentialEditorReg, null);
	}
	
	private void init(CredentialDefinition initial, final CredentialEditorRegistry credentialEditorReg)
	{
		setWidth(100, Unit.PERCENTAGE);

		name = new TextField();
		name.setCaption(msg.getMessage("CredentialDefinition.name"));
		name.setRequired(true);
		name.setRequiredError(msg.getMessage("fieldRequired"));
		addComponent(name);
		
		description = new DescriptionTextArea(msg.getMessage("CredentialDefinition.description"));
		addComponent(description);
		if (initial != null)
			description.setValue(initial.getDescription());
		
		if (initial != null)
		{
			newAuthnState = new EnumComboBox<LocalAuthenticationState>(
					msg.getMessage("CredentialDefinition.replacementState"), msg, 
					"CredentialState.", 
					LocalAuthenticationState.class, LocalAuthenticationState.outdated);
			addComponent(newAuthnState);
		}
		
		credentialType = new ComboBox(msg.getMessage("CredentialDefinition.type"));
		Set<String> supportedTypes = credentialEditorReg.getSupportedTypes();
		for (String t: supportedTypes)
			credentialType.addItem(t);
		credentialType.setNullSelectionAllowed(false);
		credentialType.setImmediate(true);

		addComponent(credentialType);
		
		credentialEditorPanel = new Panel();
		addComponent(credentialEditorPanel);
		
		String firstType = supportedTypes.iterator().next(); 
		CredentialDefinition cd = initial == null ? new CredentialDefinition(
				firstType, "", "") : initial;
		formItem = new BeanItem<CredentialDefinition>(cd);
		if (initial != null)
		{
			formItem.getItemProperty("name").setReadOnly(true);
			setCredentialEditor(initial.getJsonConfiguration(), initial.getTypeId());
		} else
			setCredentialEditor(null, firstType);
		
		binder = new FieldGroup(formItem);
		binder.bind(name, "name");
		binder.bind(description, "description");
		binder.bind(credentialType, "typeId");
		
		//set listener after setting up the form, so we won't get spurious invocation on initial input.
		credentialType.addValueChangeListener(new ValueChangeListener()
		{
			@Override
			public void valueChange(ValueChangeEvent event)
			{
				String type = (String) credentialType.getValue();
				setCredentialEditor(null, type);
			}
		});
	}
	
	private void setCredentialEditor(String state, String type)
	{
		CredentialEditorFactory edFact = credentialEditorReg.getFactory(type);
		cdEd = edFact.creteCredentialDefinitionEditor();
		credentialEditorPanel.setContent(cdEd.getEditor(state));
	}
	
	public CredentialDefinition getCredentialDefinition() throws IllegalCredentialException
	{
		String credConfig = cdEd.getCredentialDefinition();
		try
		{
			binder.commit();
		} catch (CommitException e)
		{
			throw new IllegalCredentialException("");
		}
		CredentialDefinition ret = formItem.getBean();
		ret.setJsonConfiguration(credConfig);
		return ret;
	}
	
	public LocalAuthenticationState getLocalAuthnState()
	{
		return newAuthnState == null ? null : newAuthnState.getSelectedValue();
	}
}
