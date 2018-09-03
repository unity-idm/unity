/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.formman;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.Sets;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeDefinition;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.IdentityType;
import pl.edu.icm.unity.types.registration.AgreementRegistrationParam;
import pl.edu.icm.unity.types.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.types.registration.BaseForm;
import pl.edu.icm.unity.types.registration.BaseFormBuilder;
import pl.edu.icm.unity.types.registration.ConfirmationMode;
import pl.edu.icm.unity.types.registration.CredentialRegistrationParam;
import pl.edu.icm.unity.types.registration.GroupRegistrationParam;
import pl.edu.icm.unity.types.registration.IdentityRegistrationParam;
import pl.edu.icm.unity.types.registration.OptionalRegistrationParam;
import pl.edu.icm.unity.types.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.types.registration.RegistrationParam;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.DescriptionTextArea;
import pl.edu.icm.unity.webui.common.EnumComboBox;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.GroupComboBox;
import pl.edu.icm.unity.webui.common.ListOfEmbeddedElements;
import pl.edu.icm.unity.webui.common.ListOfEmbeddedElementsStub.Editor;
import pl.edu.icm.unity.webui.common.ListOfEmbeddedElementsStub.EditorProvider;
import pl.edu.icm.unity.webui.common.NotNullComboBox;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.attributes.AttributeSelectionComboBox;
import pl.edu.icm.unity.webui.common.i18n.I18nTextArea;
import pl.edu.icm.unity.webui.common.i18n.I18nTextField;

/**
 * Base code for both registration and enquiry forms editing
 * 
 * @author K. Benedyczak
 */
public class BaseFormEditor extends VerticalLayout
{
	private UnityMessageSource msg;
	private IdentityTypeSupport identityTypeSupport;
	private Collection<IdentityType> identityTypes;
	private Collection<AttributeType> attributeTypes;
	private List<String> groups;
	private List<String> credentialTypes;
	protected boolean copyMode;
	
	protected TextField name;
	protected DescriptionTextArea description;
	
	protected I18nTextField displayedName;
	protected I18nTextArea formInformation;
	protected CheckBox collectComments;
	private ListOfEmbeddedElements<AgreementRegistrationParam> agreements;	
	private ListOfEmbeddedElements<IdentityRegistrationParam> identityParams;
	private ListOfEmbeddedElements<AttributeRegistrationParam> attributeParams;
	private ListOfEmbeddedElements<GroupRegistrationParam> groupParams;
	private ListOfEmbeddedElements<CredentialRegistrationParam> credentialParams;


	public BaseFormEditor(UnityMessageSource msg, IdentityTypeSupport identityTypeSupport,
			AttributeTypeManagement attributeMan,
			CredentialManagement authenticationMan)
			throws EngineException
	{
		setSpacing(false);
		setMargin(false);
		this.identityTypeSupport = identityTypeSupport;
		this.msg = msg;
		identityTypes = identityTypeSupport.getIdentityTypes(); 
		attributeTypes = attributeMan.getAttributeTypes();
		Collection<CredentialDefinition> crs = authenticationMan.getCredentialDefinitions();
		credentialTypes = new ArrayList<>(crs.size());
		for (CredentialDefinition cred: crs)
			credentialTypes.add(cred.getName());
	}

	protected void setValue(BaseForm toEdit)
	{
		setNameFieldValue(toEdit.getName());
		description.setValue(toEdit.getDescription() != null ? toEdit.getDescription() : "");
		
		displayedName.setValue(toEdit.getDisplayedName());
		formInformation.setValue(toEdit.getFormInformation());
		collectComments.setValue(toEdit.isCollectComments());
		List<AgreementRegistrationParam> agreementsP = toEdit.getAgreements();
		if (agreementsP != null)
			agreements.setEntries(agreementsP);
		agreements.setEntries(toEdit.getAgreements());
		identityParams.setEntries(toEdit.getIdentityParams());
		attributeParams.setEntries(toEdit.getAttributeParams());
		groupParams.setEntries(toEdit.getGroupParams());
		credentialParams.setEntries(toEdit.getCredentialParams());
	}
	
	protected void buildCommon(BaseFormBuilder<?> builder) throws FormValidationException
	{
		builder.withAgreements(agreements.getElements());
		builder.withAttributeParams(attributeParams.getElements());
		builder.withCollectComments(collectComments.getValue());
		builder.withCredentialParams(credentialParams.getElements());
		builder.withDescription(description.getValue());

		I18nString displayedNameStr = displayedName.getValue();
		displayedNameStr.setDefaultValue(name.getValue());
		builder.withDisplayedName(displayedNameStr);
		builder.withFormInformation(formInformation.getValue());
		builder.withGroupParams(groupParams.getElements());
		builder.withIdentityParams(identityParams.getElements());
		builder.withName(name.getValue());
	}
		
	protected void initNameAndDescFields(String defaultName) throws EngineException
	{
		name = new TextField(msg.getMessage("RegistrationFormEditor.name"));
		name.setValue(defaultName);
		description = new DescriptionTextArea(msg.getMessage("RegistrationFormViewer.description"));
	}
	
	protected void initCommonDisplayedFields()
	{
		displayedName = new I18nTextField(msg, msg.getMessage("RegistrationFormViewer.displayedName"));
		formInformation = new I18nTextArea(msg, msg.getMessage("RegistrationFormViewer.formInformation"));
		collectComments = new CheckBox(msg.getMessage("RegistrationFormEditor.collectComments"));
	}
	
	protected void setNameFieldValue(String initialValue)
	{
		if (!copyMode)
		{	
			name.setValue(initialValue);
			name.setReadOnly(true);
		} else
		{
			name.setValue(msg.getMessage("RegistrationFormEditor.copyPrefix")
					+ initialValue);
		}
	}
	
	protected TabSheet createCollectedParamsTabs(List<String> groups, boolean forceInteractiveRetrieval)
	{
		this.groups = groups;
		TabSheet tabOfLists = new TabSheet();
		tabOfLists.setStyleName(Styles.vTabsheetMinimal.toString());
		
		agreements = new ListOfEmbeddedElements<>(msg.getMessage("RegistrationFormEditor.agreements"), 
				msg, new AgreementEditorAndProvider(), 0, 20, true);
		
		IdentityEditorAndProvider identityEditorAndProvider = new IdentityEditorAndProvider();
		if (forceInteractiveRetrieval)
			identityEditorAndProvider.fixRetrievalSettings(ParameterRetrievalSettings.interactive);
		identityParams = new ListOfEmbeddedElements<>(msg.getMessage("RegistrationFormEditor.identityParams"),
				msg, identityEditorAndProvider, 0, 20, true);

		AttributeEditorAndProvider attributeEditorAndProvider = new AttributeEditorAndProvider();
		if (forceInteractiveRetrieval)
			attributeEditorAndProvider.fixRetrievalSettings(ParameterRetrievalSettings.interactive);
		attributeParams = new ListOfEmbeddedElements<>(msg.getMessage("RegistrationFormEditor.attributeParams"),
				msg, attributeEditorAndProvider, 0, 20, true);
		
		GroupEditorAndProvider groupEditorAndProvider = new GroupEditorAndProvider();
		if (forceInteractiveRetrieval)
			groupEditorAndProvider.fixRetrievalSettings(ParameterRetrievalSettings.interactive);
		groupParams = new ListOfEmbeddedElements<>(msg.getMessage("RegistrationFormEditor.groupParams"),
				msg, groupEditorAndProvider, 0, 20, true);
		
		credentialParams = new ListOfEmbeddedElements<>(msg.getMessage("RegistrationFormEditor.credentialParams"),
				msg, new CredentialEditorAndProvider(), 0, 20, true);
		tabOfLists.addComponents(agreements, identityParams, attributeParams, groupParams, credentialParams);
		return tabOfLists;
	}
	
	
	
	
	private class AgreementEditorAndProvider implements EditorProvider<AgreementRegistrationParam>,
		Editor<AgreementRegistrationParam>
	{
		private CheckBox required;
		private I18nTextArea text;
		
		@Override
		public Editor<AgreementRegistrationParam> getEditor()
		{
			return new AgreementEditorAndProvider();
		}

		@Override
		public ComponentsContainer getEditorComponent(AgreementRegistrationParam value, int index)
		{
			required = new CheckBox(msg.getMessage("RegistrationFormEditor.mandatory"));
			text = new I18nTextArea(msg, msg.getMessage("RegistrationFormViewer.agreement"));
			if (value != null)
			{
				required.setValue(value.isManatory());
				text.setValue(value.getText());
			}
			return new ComponentsContainer(text, required);
		}

		@Override
		public AgreementRegistrationParam getValue() throws FormValidationException
		{
			AgreementRegistrationParam ret = new AgreementRegistrationParam();
			ret.setManatory(required.getValue());
			ret.setText(text.getValue());
			return ret;
		}

		@Override
		public void setEditedComponentPosition(int position) {}
	}
	
	private class IdentityEditorAndProvider extends OptionalParameterEditor 
			implements EditorProvider<IdentityRegistrationParam>, Editor<IdentityRegistrationParam>
	{
		private ComboBox<String> identityType;
		private EnumComboBox<ConfirmationMode> confirmationMode;
		
		@Override
		public Editor<IdentityRegistrationParam> getEditor()
		{
			IdentityEditorAndProvider ret = new IdentityEditorAndProvider();
			ret.fixRetrievalSettings(fixedRetrievalSettings);
			return ret;
		}

		@Override
		public ComponentsContainer getEditorComponent(IdentityRegistrationParam value, int index)
		{
			identityType = new NotNullComboBox<>(msg.getMessage("RegistrationFormViewer.paramIdentity"));
			Set<String> items = Sets.newHashSet();
			for (IdentityType it: identityTypes)
			{
				IdentityTypeDefinition typeDef = identityTypeSupport.getTypeDefinition(it.getName());
				if (typeDef.isDynamic())
					continue;
				items.add(it.getIdentityTypeProvider());
			}
			identityType.setItems(items);
			confirmationMode = new EnumComboBox<>(
					msg.getMessage("RegistrationFormViewer.paramConfirmationMode"), 
					msg, 
					"ConfirmationMode.", 
					ConfirmationMode.class, 
					ConfirmationMode.ON_SUBMIT);
			confirmationMode.setDescription(msg.getMessage("RegistrationFormEditor.confirmationModeDesc"));
			main.add(identityType, confirmationMode);
			if (value != null)
			{
				identityType.setValue(value.getIdentityType());
				confirmationMode.setValue(value.getConfirmationMode());
			}
			initEditorComponent(value);
			return main;
		}

		@Override
		public IdentityRegistrationParam getValue() throws FormValidationException
		{
			IdentityRegistrationParam ret = new IdentityRegistrationParam();
			ret.setIdentityType(identityType.getValue());
			ret.setConfirmationMode(confirmationMode.getValue());
			fill(ret);
			return ret;
		}

		@Override
		public void setEditedComponentPosition(int position) {}
	}

	private class AttributeEditorAndProvider extends OptionalParameterEditor 
			implements EditorProvider<AttributeRegistrationParam>, Editor<AttributeRegistrationParam>
	{
		private AttributeSelectionComboBox attributeType;
		private GroupComboBox group;
		private CheckBox showGroups;
		private EnumComboBox<ConfirmationMode> confirmationMode;

		@Override
		public Editor<AttributeRegistrationParam> getEditor()
		{
			AttributeEditorAndProvider ret = new AttributeEditorAndProvider();
			ret.fixRetrievalSettings(fixedRetrievalSettings);
			return ret;
		}

		@Override
		public ComponentsContainer getEditorComponent(AttributeRegistrationParam value, int index)
		{
			attributeType = new AttributeSelectionComboBox(
					msg.getMessage("RegistrationFormViewer.paramAttribute"), attributeTypes);
			group = new GroupComboBox(msg.getMessage("RegistrationFormViewer.paramAttributeGroup"), groups);
			group.setInput("/", true);
			showGroups = new CheckBox(msg.getMessage("RegistrationFormViewer.paramShowGroup"));
			confirmationMode = new EnumComboBox<>(
					msg.getMessage("RegistrationFormViewer.paramConfirmationMode"), 
					msg, 
					"ConfirmationMode.", 
					ConfirmationMode.class, 
					ConfirmationMode.ON_SUBMIT);
			confirmationMode.setDescription(msg.getMessage("RegistrationFormEditor.confirmationModeDesc"));
			
			main.add(attributeType, group, showGroups, confirmationMode);
			
			if (value != null)
			{
				attributeType.setSelectedItemByName(value.getAttributeType());
				group.setValue(value.getGroup());
				showGroups.setValue(value.isShowGroups());
				confirmationMode.setValue(value.getConfirmationMode());
			}
			initEditorComponent(value);
			return main;
		}

		@Override
		public AttributeRegistrationParam getValue() throws FormValidationException
		{
			AttributeRegistrationParam ret = new AttributeRegistrationParam();
			ret.setAttributeType(attributeType.getValue().getName());
			ret.setGroup(group.getValue());
			ret.setShowGroups(showGroups.getValue());
			ret.setConfirmationMode(confirmationMode.getValue());
			fill(ret);
			return ret;
		}

		@Override
		public void setEditedComponentPosition(int position) {}
	}

	
	private class GroupEditorAndProvider extends ParameterEditor implements EditorProvider<GroupRegistrationParam>,
			Editor<GroupRegistrationParam>
	{
		private TextField group;
		private CheckBox multiSelectable;
		
		@Override
		public Editor<GroupRegistrationParam> getEditor()
		{
			GroupEditorAndProvider ret = new GroupEditorAndProvider();
			ret.fixRetrievalSettings(fixedRetrievalSettings);
			return ret;
		}

		@Override
		public ComponentsContainer getEditorComponent(GroupRegistrationParam value, int index)
		{
			group = new TextField(msg.getMessage("RegistrationFormViewer.paramGroup"));
			group.setDescription(msg.getMessage("RegistrationFormEditor.paramGroupDesc"));
			multiSelectable = new CheckBox(msg.getMessage("RegistrationFormEditor.paramGroupMulti"));
			main.add(group, multiSelectable);

			if (value != null)
			{
				group.setValue(value.getGroupPath());
				multiSelectable.setValue(value.isMultiSelect());
			} else
			{
				group.setValue("/**");
				multiSelectable.setValue(true);
			}
			initEditorComponent(value, Optional.of(msg.getMessage("RegistrationFormEditor.groupMembership")));
			return main;
		}

		@Override
		public GroupRegistrationParam getValue() throws FormValidationException
		{
			GroupRegistrationParam ret = new GroupRegistrationParam();
			ret.setGroupPath(group.getValue());
			ret.setMultiSelect(multiSelectable.getValue());
			ret.setLabel(label.getValue());
			fill(ret);
			return ret;
		}

		@Override
		public void setEditedComponentPosition(int position) {}
	}

	private class CredentialEditorAndProvider implements EditorProvider<CredentialRegistrationParam>,
			Editor<CredentialRegistrationParam>
	{
		private ComboBox<String> credential;
		protected TextField label;
		protected TextField description;

		@Override
		public Editor<CredentialRegistrationParam> getEditor()
		{
			return new CredentialEditorAndProvider();
		}

		@Override
		public ComponentsContainer getEditorComponent(CredentialRegistrationParam value, int index)
		{
			credential = new NotNullComboBox<>(msg.getMessage("RegistrationFormViewer.paramCredential"));
			credential.setItems(credentialTypes);
			label = new TextField(msg.getMessage("RegistrationFormViewer.paramLabel"));
			description = new TextField(msg.getMessage("RegistrationFormViewer.paramDescription"));

			ComponentsContainer ret = new ComponentsContainer(credential);
			if (value != null)
			{
				credential.setValue(value.getCredentialName());
				if (value.getLabel() != null)
				{
					label.setValue(value.getLabel());
					ret.add(label);
				}
				if (value.getDescription() != null)
				{
					description.setValue(value.getDescription());
					ret.add(description);
				}
			}
			return ret;
		}

		@Override
		public CredentialRegistrationParam getValue() throws FormValidationException
		{
			CredentialRegistrationParam ret = new CredentialRegistrationParam();
			ret.setCredentialName(credential.getValue());
			if (!label.getValue().isEmpty())
				ret.setLabel(label.getValue());
			if (!description.getValue().isEmpty())
				ret.setDescription(description.getValue());
			return ret;
		}

		@Override
		public void setEditedComponentPosition(int position) {}
	}

	
	private abstract class ParameterEditor 
	{
		protected ComponentsContainer main = new ComponentsContainer();
		protected TextField label;
		protected TextField description;
		protected EnumComboBox<ParameterRetrievalSettings> retrievalSettings;
		protected ParameterRetrievalSettings fixedRetrievalSettings;

		protected void initEditorComponent(RegistrationParam value, Optional<String> defaultLabel)
		{
			label = new TextField(msg.getMessage("RegistrationFormViewer.paramLabel"));
			description = new TextField(msg.getMessage("RegistrationFormViewer.paramDescription"));
			retrievalSettings = new EnumComboBox<>(
					msg.getMessage("RegistrationFormViewer.paramSettings"), msg, 
					"ParameterRetrievalSettings.", ParameterRetrievalSettings.class, 
					ParameterRetrievalSettings.interactive);			
			if (value != null)
			{
				String labelStr = value.getLabel() != null ? value.getLabel() : defaultLabel.orElse(null);
				if (labelStr != null)
				{
					label.setValue(labelStr);
					main.add(label);
				}
				if (value.getDescription() != null)
				{
					description.setValue(value.getDescription());
					main.add(description);
				}
				retrievalSettings.setValue(value.getRetrievalSettings());
			} else
			{
				if (defaultLabel.isPresent())
				{
					label.setValue(defaultLabel.get());
					main.add(label);
				}
			}

			if (fixedRetrievalSettings != null)
			{
				retrievalSettings.setValue(fixedRetrievalSettings);
				retrievalSettings.setVisible(false);
			}
			
			main.add(retrievalSettings);
		}
		
		protected void fill(RegistrationParam v)
		{
			if (!description.getValue().isEmpty())
				v.setDescription(description.getValue());
			if (!label.getValue().isEmpty())
				v.setLabel(label.getValue());
			v.setRetrievalSettings(retrievalSettings.getValue());
		}
		
		public void fixRetrievalSettings(ParameterRetrievalSettings fixedValue)
		{
			this.fixedRetrievalSettings = fixedValue;
		}
	}
	
	private abstract class OptionalParameterEditor extends ParameterEditor
	{
		protected CheckBox optional;

		protected void initEditorComponent(OptionalRegistrationParam value)
		{
			super.initEditorComponent(value, Optional.empty());
			optional = new CheckBox(msg.getMessage("RegistrationFormViewer.paramOptional"));
			main.add(optional);
			
			if (value != null)
			{
				optional.setValue(value.isOptional());
			}
		}
		
		protected void fill(OptionalRegistrationParam v)
		{
			super.fill(v);
			v.setOptional(optional.getValue());
		}
	}	

}
