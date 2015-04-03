/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.credentials;

import java.util.Set;

import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.authn.LocalCredentialState;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.EnumComboBox;
import pl.edu.icm.unity.webui.common.RequiredTextField;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorFactory;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;
import pl.edu.icm.unity.webui.common.i18n.I18nTextArea;
import pl.edu.icm.unity.webui.common.i18n.I18nTextField;
import pl.edu.icm.unity.webui.common.safehtml.SafePanel;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.fieldgroup.FieldGroup;
import com.vaadin.data.fieldgroup.FieldGroup.CommitException;
import com.vaadin.data.util.BeanItem;
import com.vaadin.ui.AbstractTextField;
import com.vaadin.ui.ComboBox;

/**
 * Allows to edit a credential definition. Can be configured to edit an existing definition (name and is fixed,
 * updated credentials state combo is shown) or to create a new one (name can be chosen).
 * 
 * @author K. Benedyczak
 */
public class CredentialDefinitionEditor extends CompactFormLayout
{
	private UnityMessageSource msg;
	private CredentialEditorRegistry credentialEditorReg;
	private AbstractTextField name;
	private I18nTextField displayedName;
	private I18nTextArea description;
	private EnumComboBox<LocalCredentialState> newCredState; 
	private ComboBox credentialType;
	private SafePanel credentialEditorPanel;
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

		name = new RequiredTextField(msg);
		name.setCaption(msg.getMessage("CredentialDefinition.name"));
		addComponent(name);

		displayedName = new I18nTextField(msg, msg.getMessage("displayedNameF"));
		addComponent(displayedName);
		
		description = new I18nTextArea(msg, msg.getMessage("descriptionF"));
		addComponent(description);
		
		if (initial != null)
		{
			newCredState = new EnumComboBox<LocalCredentialState>(
					msg.getMessage("CredentialDefinition.replacementState"), msg, 
					"DesiredCredentialStatus.", 
					LocalCredentialState.class, LocalCredentialState.outdated);
			addComponent(newCredState);
		}
		
		credentialType = new ComboBox(msg.getMessage("CredentialDefinition.type"));
		Set<String> supportedTypes = credentialEditorReg.getSupportedTypes();
		for (String t: supportedTypes)
			credentialType.addItem(t);
		credentialType.setNullSelectionAllowed(false);
		credentialType.setImmediate(true);

		addComponent(credentialType);
		
		credentialEditorPanel = new SafePanel();
		addComponent(credentialEditorPanel);
		
		String firstType = supportedTypes.iterator().next(); 
		CredentialDefinition cd = initial == null ? new CredentialDefinition(
				firstType, msg.getMessage("CredentialDefinition.defaultName"), new I18nString(), 
				new I18nString("")) : initial;
		formItem = new BeanItem<CredentialDefinition>(cd);
		if (initial != null)
		{
			formItem.getItemProperty("name").setReadOnly(true);
			setCredentialEditor(initial.getJsonConfiguration(), initial.getTypeId());
		} else
			setCredentialEditor(null, firstType);
		
		binder = new FieldGroup(formItem);
		binder.bind(name, "name");
		binder.bind(displayedName, "displayedName");
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
		ret.getDisplayedName().setDefaultValue(ret.getName());
		return ret;
	}
	
	public LocalCredentialState getLocalCredState()
	{
		return newCredState == null ? null : newCredState.getSelectedValue();
	}
}
