/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.formman;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
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
import pl.edu.icm.unity.types.registration.GroupRegistrationParam.IncludeGroupsMode;
import pl.edu.icm.unity.types.registration.IdentityRegistrationParam;
import pl.edu.icm.unity.types.registration.OptionalRegistrationParam;
import pl.edu.icm.unity.types.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.types.registration.RegistrationParam;
import pl.edu.icm.unity.types.registration.RegistrationWrapUpConfig;
import pl.edu.icm.unity.types.registration.RegistrationWrapUpConfig.TriggeringState;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.DescriptionTextArea;
import pl.edu.icm.unity.webui.common.EnumComboBox;
import pl.edu.icm.unity.webui.common.FormValidationException;
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
	private static final int COMBO_WIDTH_EM = 18;
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
	protected I18nTextField pageTitle;
	private ListOfEmbeddedElements<AgreementRegistrationParam> agreements;	
	private ListOfEmbeddedElements<IdentityRegistrationParam> identityParams;
	private ListOfEmbeddedElements<AttributeRegistrationParam> attributeParams;
	private ListOfEmbeddedElements<GroupRegistrationParam> groupParams;
	private ListOfEmbeddedElements<CredentialRegistrationParam> credentialParams;
	private ListOfEmbeddedElements<RegistrationWrapUpConfig> wrapUpConfig;
	private TabSheet collectedParamsTabSheet;
	
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
		//order is important as attributes depend on groups for dynamic groups
		groupParams.setEntries(toEdit.getGroupParams());
		attributeParams.setEntries(toEdit.getAttributeParams());
		credentialParams.setEntries(toEdit.getCredentialParams());
		wrapUpConfig.setEntries(toEdit.getWrapUpConfig());
		if (toEdit.getPageTitle() != null)
			pageTitle.setValue(toEdit.getPageTitle());
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
		
		builder.withWrapUpConfig(wrapUpConfig.getElements());
		
		builder.withPageTitle(pageTitle.getValue());
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
		pageTitle = new I18nTextField(msg, msg.getMessage("RegistrationFormEditor.registrationPageTitle"));
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
		collectedParamsTabSheet = new TabSheet();
		collectedParamsTabSheet.setStyleName(Styles.vTabsheetMinimal.toString());
		
		agreements = new ListOfEmbeddedElements<>(msg.getMessage("RegistrationFormEditor.agreements"), 
				msg, new AgreementEditorAndProvider(), 0, 20, true);
		agreements.setSpacing(false);
		agreements.setMargin(true);

		IdentityEditorAndProvider identityEditorAndProvider = new IdentityEditorAndProvider();
		if (forceInteractiveRetrieval)
			identityEditorAndProvider.fixRetrievalSettings(ParameterRetrievalSettings.interactive);
		identityParams = new ListOfEmbeddedElements<>(msg.getMessage("RegistrationFormEditor.identityParams"),
				msg, identityEditorAndProvider, 0, 20, true);
		
		Component localSignupMethods = createLocalSignupMethodsTab(forceInteractiveRetrieval);
		
		GroupEditorAndProvider groupEditorAndProvider = new GroupEditorAndProvider();
		if (forceInteractiveRetrieval)
			groupEditorAndProvider.fixRetrievalSettings(ParameterRetrievalSettings.interactive);
		groupParams = new ListOfEmbeddedElements<>(msg.getMessage("RegistrationFormEditor.groupParams"),
				msg, groupEditorAndProvider, 0, 20, true);
		groupParams.setValueChangeListener(this::onGroupChanges);
		
		AttributeEditorAndProvider attributeEditorAndProvider = new AttributeEditorAndProvider();
		if (forceInteractiveRetrieval)
			attributeEditorAndProvider.fixRetrievalSettings(ParameterRetrievalSettings.interactive);
		attributeParams = new ListOfEmbeddedElements<>(msg.getMessage("RegistrationFormEditor.attributeParams"),
				msg, attributeEditorAndProvider, 0, 20, true);
				
		collectedParamsTabSheet.addComponents(identityParams, localSignupMethods, groupParams, attributeParams, agreements);
		return collectedParamsTabSheet;
	}

	protected Component getWrapUpComponent(Predicate<TriggeringState> filter) throws EngineException
	{
		FormLayout main = new CompactFormLayout();
		VerticalLayout wrapper = new VerticalLayout(main);
		wrapper.setMargin(true);
		wrapper.setSpacing(false);

		WrapupConfigEditorAndProvider groupEditorAndProvider = new WrapupConfigEditorAndProvider(filter);
		wrapUpConfig = new ListOfEmbeddedElements<>(null,
				msg, groupEditorAndProvider, 0, 20, true);
		main.addComponents(wrapUpConfig);
		return wrapper;
	}
	
	private List<String> getDynamicGroups()
	{
		try
		{
			return groupParams.getElements().stream()
					.filter(gp -> !gp.isMultiSelect())
					.map(gp -> gp.getGroupPath()).collect(Collectors.toList());
		} catch (FormValidationException e)
		{
			return Collections.emptyList();
		}
	}
	
	private void onGroupChanges()
	{
		attributeParams.refresh();
	}
	
	private Component createLocalSignupMethodsTab(boolean forceInteractiveRetrieval)
	{
		credentialParams = new ListOfEmbeddedElements<>(msg.getMessage("RegistrationFormEditor.credentialParams"),
				msg, new CredentialEditorAndProvider(), 0, 20, true);
		credentialParams.setCaption(msg.getMessage("RegistrationFormEditor.localSignupMethods"));
		return credentialParams;
	}
	
	protected void setCredentialsTabVisible(boolean visible)
	{
		collectedParamsTabSheet.getTab(credentialParams).setVisible(visible);
	}
	
	protected void setIdentitiesTabVisible(boolean visible)
	{
		collectedParamsTabSheet.getTab(identityParams).setVisible(visible);
	}
	
	protected void resetCredentialTab()
	{
		credentialParams.resetContents();
	}
	
	protected void resetIdentitiesTab()
	{
		identityParams.resetContents();
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
			identityType.setWidth(COMBO_WIDTH_EM, Unit.EM);
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
			confirmationMode.setWidth(COMBO_WIDTH_EM, Unit.EM);
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
		private FormAttributeGroupComboBox group;
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
			attributeType.setWidth(COMBO_WIDTH_EM, Unit.EM);
			group = new FormAttributeGroupComboBox(msg.getMessage("RegistrationFormViewer.paramAttributeGroup"), 
					msg, groups, getDynamicGroups());
			group.updateDynamicGroups(getDynamicGroups());
			group.setWidth(COMBO_WIDTH_EM, Unit.EM);
			showGroups = new CheckBox(msg.getMessage("RegistrationFormViewer.paramShowGroup"));
			confirmationMode = new EnumComboBox<>(
					msg.getMessage("RegistrationFormViewer.paramConfirmationMode"), 
					msg, 
					"ConfirmationMode.", 
					ConfirmationMode.class, 
					ConfirmationMode.ON_SUBMIT);
			confirmationMode.setDescription(msg.getMessage("RegistrationFormEditor.confirmationModeDesc"));
			confirmationMode.setWidth(COMBO_WIDTH_EM, Unit.EM);
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
		public void setEditedComponentPosition(int position)
		{
			group.updateDynamicGroups(getDynamicGroups());
		}
	}

	
	private class GroupEditorAndProvider extends ParameterEditor implements EditorProvider<GroupRegistrationParam>,
			Editor<GroupRegistrationParam>
	{
		private TextField group;
		private CheckBox multiSelectable;
		private EnumComboBox<IncludeGroupsMode> includeGroupsMode;
		
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
			group.addValueChangeListener(nv -> onGroupChanges());
			multiSelectable = new CheckBox(msg.getMessage("RegistrationFormEditor.paramGroupMulti"));
			multiSelectable.addValueChangeListener(nv -> onGroupChanges());
			
			includeGroupsMode = new EnumComboBox<>(
					msg.getMessage("RegistrationFormViewer.paramGroupMode"), msg, 
					"GroupAccessMode.", IncludeGroupsMode.class, 
					IncludeGroupsMode.all);
			
			main.add(group, multiSelectable, includeGroupsMode);

			if (value != null)
			{
				group.setValue(value.getGroupPath());
				multiSelectable.setValue(value.isMultiSelect());
				includeGroupsMode.setValue(value.getIncludeGroupsMode());
			} else
			{
				group.setValue("/**");
				multiSelectable.setValue(true);
				includeGroupsMode.setValue(IncludeGroupsMode.all);
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
			ret.setIncludeGroupsMode(includeGroupsMode.getValue());
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
			retrievalSettings.setWidth(COMBO_WIDTH_EM, Unit.EM);
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

	
	private class WrapupConfigEditorAndProvider implements EditorProvider<RegistrationWrapUpConfig>, Editor<RegistrationWrapUpConfig>
	{
		private RegistrationWrapUpConfigEditor editor;
		private Predicate<RegistrationWrapUpConfig.TriggeringState> filter;
		
		WrapupConfigEditorAndProvider(Predicate<TriggeringState> filter)
		{
			this.filter = filter;
		}

		@Override
		public Editor<RegistrationWrapUpConfig> getEditor()
		{
			return new WrapupConfigEditorAndProvider(filter);
		}

		@Override
		public ComponentsContainer getEditorComponent(RegistrationWrapUpConfig value, int index)
		{
			editor = new RegistrationWrapUpConfigEditor(msg, filter);
			if (value != null)
				editor.setValue(value);
			return new ComponentsContainer(editor);
		}

		@Override
		public RegistrationWrapUpConfig getValue() throws FormValidationException
		{
			return editor.getValue();
		}

		@Override
		public void setEditedComponentPosition(int position) {}
	}
}
