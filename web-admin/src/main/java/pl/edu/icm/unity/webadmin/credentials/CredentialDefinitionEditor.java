/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.credentials;

import java.util.Set;

import com.vaadin.data.Binder;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.TextField;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.authn.LocalCredentialState;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.EnumComboBox2;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorFactory;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;
import pl.edu.icm.unity.webui.common.i18n.I18nTextArea2;
import pl.edu.icm.unity.webui.common.i18n.I18nTextField2;
import pl.edu.icm.unity.webui.common.safehtml.SafePanel;

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
	private TextField name;
	private I18nTextField2 displayedName;
	private I18nTextArea2 description;
	private EnumComboBox2<LocalCredentialState> newCredState; 
	private ComboBox<String> credentialType;
	private SafePanel credentialEditorPanel;
	private pl.edu.icm.unity.webui.common.credentials.CredentialDefinitionEditor cdEd;
	
	private Binder<CredentialDefinition> binder;
	
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

		name = new TextField(msg.getMessage("CredentialDefinition.name"));
		addComponent(name);

		displayedName = new I18nTextField2(msg, msg.getMessage("displayedNameF"));
		addComponent(displayedName);
		
		description = new I18nTextArea2(msg, msg.getMessage("descriptionF"));
		addComponent(description);
		
		if (initial != null)
		{
			newCredState = new EnumComboBox2<LocalCredentialState>(
					msg.getMessage("CredentialDefinition.replacementState"), msg, 
					"DesiredCredentialStatus.", 
					LocalCredentialState.class, LocalCredentialState.outdated);
			addComponent(newCredState);
		}
		
		credentialType = new ComboBox<>(msg.getMessage("CredentialDefinition.type"));
		Set<String> supportedTypes = credentialEditorReg.getSupportedTypes();
		credentialType.setItems(supportedTypes);
		credentialType.setEmptySelectionAllowed(false);

		addComponent(credentialType);
		
		credentialEditorPanel = new SafePanel();
		addComponent(credentialEditorPanel);
		
		String firstType = supportedTypes.iterator().next(); 
		CredentialDefinition cd = initial == null ? new CredentialDefinition(
				firstType, msg.getMessage("CredentialDefinition.defaultName"), new I18nString(), 
				new I18nString("")) : initial;
		if (initial != null)
		{
			name.setReadOnly(true);
			setCredentialEditor(initial.getConfiguration(), initial.getTypeId());
		} else
			setCredentialEditor(null, firstType);
		
		binder = new Binder<>(CredentialDefinition.class);
		binder.forField(name).asRequired(msg.getMessage("fieldRequired")).bind("name");
		binder.bind(displayedName, "displayedName");
		binder.bind(description, "description");
		binder.bind(credentialType, "typeId");
		binder.setBean(cd);
		
		//set listener after setting up the form, so we won't get spurious invocation on initial input.
		credentialType.addValueChangeListener(event ->
			setCredentialEditor(null, credentialType.getValue()));
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
		if (!binder.isValid())
			throw new IllegalCredentialException("");
		CredentialDefinition ret = binder.getBean();
		ret.setConfiguration(credConfig);
		ret.getDisplayedName().setDefaultValue(ret.getName());
		return ret;
	}
	
	public LocalCredentialState getLocalCredState()
	{
		return newCredState == null ? null : newCredState.getValue();
	}
}
