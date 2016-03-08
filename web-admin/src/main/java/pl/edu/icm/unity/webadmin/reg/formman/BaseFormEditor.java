/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.formman;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.registries.RegistrationActionsRegistry;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.IdentityType;
import pl.edu.icm.unity.types.registration.AgreementRegistrationParam;
import pl.edu.icm.unity.types.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.types.registration.BaseForm;
import pl.edu.icm.unity.types.registration.BaseFormBuilder;
import pl.edu.icm.unity.types.registration.CredentialRegistrationParam;
import pl.edu.icm.unity.types.registration.GroupRegistrationParam;
import pl.edu.icm.unity.types.registration.IdentityRegistrationParam;
import pl.edu.icm.unity.types.registration.OptionalRegistrationParam;
import pl.edu.icm.unity.types.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.types.registration.RegistrationParam;
import pl.edu.icm.unity.webadmin.tprofile.ActionParameterComponentFactory.Provider;
import pl.edu.icm.unity.webadmin.tprofile.TranslationProfileEditor;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.DescriptionTextArea;
import pl.edu.icm.unity.webui.common.EnumComboBox;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.GroupComboBox;
import pl.edu.icm.unity.webui.common.ListOfEmbeddedElements;
import pl.edu.icm.unity.webui.common.ListOfEmbeddedElementsStub.Editor;
import pl.edu.icm.unity.webui.common.ListOfEmbeddedElementsStub.EditorProvider;
import pl.edu.icm.unity.webui.common.NotNullComboBox;
import pl.edu.icm.unity.webui.common.RequiredTextField;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.attributes.AttributeSelectionComboBox;
import pl.edu.icm.unity.webui.common.i18n.I18nTextArea;
import pl.edu.icm.unity.webui.common.i18n.I18nTextField;

import com.vaadin.ui.AbstractTextField;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

/**
 * Base code for both registration and enquiry forms editing
 * 
 * @author K. Benedyczak
 */
public class BaseFormEditor extends VerticalLayout
{
	private UnityMessageSource msg;
	private Collection<IdentityType> identityTypes;
	private Collection<AttributeType> attributeTypes;
	private List<String> groups;
	private List<String> credentialTypes;
	protected boolean copyMode;
	
	protected AbstractTextField name;
	protected DescriptionTextArea description;
	
	protected I18nTextField displayedName;
	protected I18nTextArea formInformation;
	protected CheckBox collectComments;
	private ListOfEmbeddedElements<AgreementRegistrationParam> agreements;	
	private ListOfEmbeddedElements<IdentityRegistrationParam> identityParams;
	private ListOfEmbeddedElements<AttributeRegistrationParam> attributeParams;
	private ListOfEmbeddedElements<GroupRegistrationParam> groupParams;
	private ListOfEmbeddedElements<CredentialRegistrationParam> credentialParams;

	private TranslationProfileEditor profileEditor;
	
	public BaseFormEditor(UnityMessageSource msg, IdentitiesManagement identitiesMan,
			AttributesManagement attributeMan,
			AuthenticationManagement authenticationMan, RegistrationActionsRegistry actionsRegistry,
			Provider actionComponentProvider) 
					throws EngineException
	{
		this(msg, identitiesMan, attributeMan, authenticationMan, false);
	}

	public BaseFormEditor(UnityMessageSource msg, IdentitiesManagement identitiesMan,
			AttributesManagement attributeMan,
			AuthenticationManagement authenticationMan,
			boolean copyMode)
			throws EngineException
	{
		this.copyMode = copyMode;
		this.msg = msg;
		identityTypes = identitiesMan.getIdentityTypes(); 
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
		builder.withTranslationProfile(profileEditor.getProfile());
		builder.withDescription(description.getValue());

		I18nString displayedNameStr = displayedName.getValue();
		displayedNameStr.setDefaultValue(name.getValue());
		builder.withDisplayedName(displayedNameStr);
		builder.withFormInformation(formInformation.getValue());
		builder.withGroupParams(groupParams.getElements());
		builder.withIdentityParams(identityParams.getElements());
		builder.withName(name.getValue());
	}
		
	protected void initNameAndDescFields() throws EngineException
	{
		name = new RequiredTextField(msg);
		name.setCaption(msg.getMessage("RegistrationFormEditor.name"));
		name.setValue(msg.getMessage("RegistrationFormEditor.defaultName"));
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
	
	protected TabSheet createCollectedParamsTabs(List<String> groups)
	{
		this.groups = groups;
		TabSheet tabOfLists = new TabSheet();
		tabOfLists.setStyleName(Styles.vTabsheetMinimal.toString());
		
		agreements = new ListOfEmbeddedElements<>(msg.getMessage("RegistrationFormEditor.agreements"), 
				msg, new AgreementEditorAndProvider(), 0, 20, true);
		identityParams = new ListOfEmbeddedElements<>(msg.getMessage("RegistrationFormEditor.identityParams"),
				msg, new IdentityEditorAndProvider(), 1, 20, true);
		attributeParams = new ListOfEmbeddedElements<>(msg.getMessage("RegistrationFormEditor.attributeParams"),
				msg, new AttributeEditorAndProvider(), 0, 20, true);
		groupParams = new ListOfEmbeddedElements<>(msg.getMessage("RegistrationFormEditor.groupParams"),
				msg, new GroupEditorAndProvider(), 0, 20, true);
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
		private ComboBox identityType;

		@Override
		public Editor<IdentityRegistrationParam> getEditor()
		{
			return new IdentityEditorAndProvider();
		}

		@Override
		public ComponentsContainer getEditorComponent(IdentityRegistrationParam value, int index)
		{
			identityType = new NotNullComboBox(msg.getMessage("RegistrationFormViewer.paramIdentity"));
			for (IdentityType it: identityTypes)
			{
				if (it.getIdentityTypeProvider().isDynamic())
					continue;
				identityType.addItem(it.getIdentityTypeProvider().getId());
			}
			main.add(identityType);
			if (value != null)
				identityType.setValue(value.getIdentityType());
			initEditorComponent(value);
			return main;
		}

		@Override
		public IdentityRegistrationParam getValue() throws FormValidationException
		{
			IdentityRegistrationParam ret = new IdentityRegistrationParam();
			ret.setIdentityType((String) identityType.getValue());
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

		@Override
		public Editor<AttributeRegistrationParam> getEditor()
		{
			return new AttributeEditorAndProvider();
		}

		@Override
		public ComponentsContainer getEditorComponent(AttributeRegistrationParam value, int index)
		{
			attributeType = new AttributeSelectionComboBox(
					msg.getMessage("RegistrationFormViewer.paramAttribute"), attributeTypes);
			group = new GroupComboBox(msg.getMessage("RegistrationFormViewer.paramAttributeGroup"), groups);
			group.setInput("/", true);
			showGroups = new CheckBox(msg.getMessage("RegistrationFormViewer.paramShowGroup"));
			
			main.add(attributeType, group, showGroups);
			
			if (value != null)
			{
				attributeType.setValue(value.getAttributeType());
				group.setValue(value.getGroup());
				showGroups.setValue(value.isShowGroups());
			}
			initEditorComponent(value);
			return main;
		}

		@Override
		public AttributeRegistrationParam getValue() throws FormValidationException
		{
			AttributeRegistrationParam ret = new AttributeRegistrationParam();
			ret.setAttributeType((String) attributeType.getValue());
			ret.setGroup((String) group.getValue());
			ret.setShowGroups(showGroups.getValue());
			fill(ret);
			return ret;
		}

		@Override
		public void setEditedComponentPosition(int position) {}
	}

	
	private class GroupEditorAndProvider extends ParameterEditor implements EditorProvider<GroupRegistrationParam>,
			Editor<GroupRegistrationParam>
	{
		private GroupComboBox group;

		@Override
		public Editor<GroupRegistrationParam> getEditor()
		{
			return new GroupEditorAndProvider();
		}

		@Override
		public ComponentsContainer getEditorComponent(GroupRegistrationParam value, int index)
		{
			group = new GroupComboBox(msg.getMessage("RegistrationFormViewer.paramGroup"), groups);
			group.setInput("/", false);
			if (value != null)
				group.setValue(value.getGroupPath());
			main.add(group);
			initEditorComponent(value);
			return main;
		}

		@Override
		public GroupRegistrationParam getValue() throws FormValidationException
		{
			GroupRegistrationParam ret = new GroupRegistrationParam();
			ret.setGroupPath((String) group.getValue());
			fill(ret);
			return ret;
		}

		@Override
		public void setEditedComponentPosition(int position) {}
	}

	private class CredentialEditorAndProvider implements EditorProvider<CredentialRegistrationParam>,
			Editor<CredentialRegistrationParam>
	{
		private ComboBox credential;
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
			credential = new NotNullComboBox(msg.getMessage("RegistrationFormViewer.paramCredential"));
			for (String c: credentialTypes)
				credential.addItem(c);
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
			ret.setCredentialName((String) credential.getValue());
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

		protected void initEditorComponent(RegistrationParam value)
		{
			label = new TextField(msg.getMessage("RegistrationFormViewer.paramLabel"));
			description = new TextField(msg.getMessage("RegistrationFormViewer.paramDescription"));
			retrievalSettings = new EnumComboBox<ParameterRetrievalSettings>(
					msg.getMessage("RegistrationFormViewer.paramSettings"), msg, 
					"ParameterRetrievalSettings.", ParameterRetrievalSettings.class, 
					ParameterRetrievalSettings.interactive);			
			if (value != null)
			{
				if (value.getLabel() != null)
				{
					label.setValue(value.getLabel());
					main.add(label);
				}
				if (value.getDescription() != null)
				{
					description.setValue(value.getDescription());
					main.add(description);
				}
				retrievalSettings.setEnumValue(value.getRetrievalSettings());
			}
			main.add(retrievalSettings);
		}
		
		protected void fill(RegistrationParam v)
		{
			if (!description.getValue().isEmpty())
				v.setDescription(description.getValue());
			if (!label.getValue().isEmpty())
				v.setLabel(label.getValue());
			v.setRetrievalSettings(retrievalSettings.getSelectedValue());
		}
	}
	
	private abstract class OptionalParameterEditor extends ParameterEditor
	{
		protected CheckBox optional;

		protected void initEditorComponent(OptionalRegistrationParam value)
		{
			super.initEditorComponent(value);
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
