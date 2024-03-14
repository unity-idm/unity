/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.signup_and_enquiry;

import com.google.common.collect.Streams;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextField;
import io.imunity.console.tprofile.ActionParameterComponentProvider;
import io.imunity.console.tprofile.AttributeSelectionComboBox;
import io.imunity.console.tprofile.DynamicGroupParamWithLabel;
import io.imunity.vaadin.elements.EnumComboBox;
import io.imunity.vaadin.elements.LocalizedTextAreaDetails;
import io.imunity.vaadin.elements.LocalizedTextFieldDetails;
import io.imunity.vaadin.elements.NotEmptyComboBox;
import io.imunity.vaadin.auth.services.idp.PolicyAgreementConfigurationList;
import io.imunity.vaadin.endpoint.common.plugins.ComponentsContainer;
import io.imunity.vaadin.endpoint.common.plugins.attributes.ListOfEmbeddedElements;
import io.imunity.vaadin.endpoint.common.plugins.attributes.ListOfEmbeddedElementsStub.Editor;
import io.imunity.vaadin.endpoint.common.plugins.attributes.ListOfEmbeddedElementsStub.EditorProvider;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.authn.CredentialDefinition;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.identity.IdentityType;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.*;
import pl.edu.icm.unity.base.registration.GroupRegistrationParam.IncludeGroupsMode;
import pl.edu.icm.unity.base.registration.RegistrationWrapUpConfig.TriggeringState;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeDefinition;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import io.imunity.vaadin.endpoint.common.exceptions.FormValidationException;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static io.imunity.console.views.signup_and_enquiry.FormAttributeGroupComboBox.*;
import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_BIG;
import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_MEDIUM;
import static io.imunity.vaadin.elements.CssClassNames.MEDIUM_VAADIN_FORM_ITEM_LABEL;


public class BaseFormEditor extends VerticalLayout
{
	private final MessageSource msg;
	private final PolicyAgreementConfigurationList.PolicyAgreementConfigurationListFactory policyAgreementConfigurationListFactory;
	private final Map<String, IdentityTypeDefinition> allowedIdentitiesByName;
	private final AttributeTypeSupport attributeTypeSupport;
	protected ActionParameterComponentProvider actionComponentProvider;
	private final Collection<AttributeType> attributeTypes;
	private List<GroupModel> groups;
	private final List<String> credentialTypes;
	protected boolean copyMode;
	
	protected TextField name;
	protected TextField description;
	protected Checkbox checkIdentityOnSubmit;
	
	protected LocalizedTextFieldDetails displayedName;
	protected LocalizedTextAreaDetails formInformation;
	protected Checkbox collectComments;
	protected LocalizedTextFieldDetails pageTitle;
	private ListOfEmbeddedElements<AgreementRegistrationParam> optins;
	private ListOfEmbeddedElements<IdentityRegistrationParam> identityParams;
	private ListOfEmbeddedElements<AttributeRegistrationParam> attributeParams;
	private ListOfEmbeddedElements<GroupRegistrationParam> groupParams;
	private ListOfEmbeddedElements<CredentialRegistrationParam> credentialParams;
	private ListOfEmbeddedElements<RegistrationWrapUpConfig> wrapUpConfig;
	private PolicyAgreementConfigurationList policyAgreements;
	private TabSheet collectedParamsTabSheet;
	
	public BaseFormEditor(MessageSource msg, IdentityTypeSupport identityTypeSupport,
			AttributeTypeManagement attributeMan,
			CredentialManagement authenticationMan, 
			PolicyAgreementConfigurationList.PolicyAgreementConfigurationListFactory policyAgreementConfigurationListFactory,
			AttributeTypeSupport attributeTypeSupport,
			ActionParameterComponentProvider actionComponentProvider)
			throws EngineException
	{
		this.attributeTypeSupport = attributeTypeSupport;
		setSpacing(false);
		setMargin(false);
		this.msg = msg;
		attributeTypes = attributeMan.getAttributeTypes();
		Collection<CredentialDefinition> crs = authenticationMan.getCredentialDefinitions();
		credentialTypes = new ArrayList<>(crs.size());
		for (CredentialDefinition cred: crs)
			credentialTypes.add(cred.getName());
		this.policyAgreementConfigurationListFactory = policyAgreementConfigurationListFactory;
		this.allowedIdentitiesByName = getAllowedIdentities(identityTypeSupport);
		this.actionComponentProvider = actionComponentProvider;
		this.actionComponentProvider.init(this::getDynamicGroupsForProfile);
	}

	private Map<String, IdentityTypeDefinition> getAllowedIdentities(IdentityTypeSupport identityTypeSupport)
	{
		Collection<IdentityType> identityTypes = identityTypeSupport.getIdentityTypes();
		Map<String, IdentityTypeDefinition> allowedIdentitiesByName = new HashMap<>();
		for (IdentityType it: identityTypes)
		{
			IdentityTypeDefinition typeDef = identityTypeSupport.getTypeDefinition(it.getName());
			if (typeDef.isUserSettable())
				allowedIdentitiesByName.put(it.getIdentityTypeProvider(), typeDef);
		}
		return Collections.unmodifiableMap(allowedIdentitiesByName);
	}
	
	protected void setValue(BaseForm toEdit)
	{
		setNameFieldValue(toEdit.getName());
		description.setValue(toEdit.getDescription() != null ? toEdit.getDescription() : "");
		
		displayedName.setValue(toEdit.getDisplayedName().getLocalizedMap());
		formInformation.setValue(toEdit.getFormInformation().getLocalizedMap());
		collectComments.setValue(toEdit.isCollectComments());
		List<AgreementRegistrationParam> agreementsP = toEdit.getAgreements();
		if (agreementsP != null)
			optins.setEntries(agreementsP);
		optins.setEntries(toEdit.getAgreements());
		identityParams.setEntries(toEdit.getIdentityParams());
		//order is important as attributes depend on groups for dynamic groups
		groupParams.setEntries(toEdit.getGroupParams());
		attributeParams.setEntries(toEdit.getAttributeParams());
		credentialParams.setEntries(toEdit.getCredentialParams());
		wrapUpConfig.setEntries(toEdit.getWrapUpConfig());
		if (toEdit.getPageTitle() != null)
			pageTitle.setValue(toEdit.getPageTitle().getLocalizedMap());
		policyAgreements.setValue(toEdit.getPolicyAgreements());
		checkIdentityOnSubmit.setValue(toEdit.isCheckIdentityOnSubmit());
	}
	
	protected void buildCommon(BaseFormBuilder<?> builder) throws FormValidationException
	{
		builder.withAgreements(optins.getElements());
		builder.withAttributeParams(attributeParams.getElements());
		builder.withCollectComments(collectComments.getValue());
		builder.withCredentialParams(credentialParams.getElements());
		builder.withDescription(description.getValue());

		I18nString displayedNameStr = new I18nString(displayedName.getValue());
		displayedNameStr.setDefaultValue(name.getValue());
		builder.withDisplayedName(displayedNameStr);
		builder.withFormInformation(new I18nString(formInformation.getValue()));
		builder.withGroupParams(groupParams.getElements());
		builder.withIdentityParams(identityParams.getElements());
		builder.withName(name.getValue());
		
		builder.withWrapUpConfig(wrapUpConfig.getElements());

		builder.withPageTitle(new I18nString(pageTitle.getValue()));
		policyAgreements.validate();
		builder.withPolicyAgreements(policyAgreements.getValue());
		builder.withCheckIdentityOnSubmit(checkIdentityOnSubmit.getValue());
	}
		
	protected void initNameAndDescFields(String defaultName)
	{
		name = new TextField();
		name.setValue(defaultName);
		name.setWidth(TEXT_FIELD_MEDIUM.value());
		description = new TextField();
		description.setWidth(TEXT_FIELD_BIG.value());
		
		checkIdentityOnSubmit = new Checkbox(msg.getMessage("RegistrationFormEditor.checkIdentityOnSubmit"));
	}
	
	protected void initCommonDisplayedFields()
	{
		displayedName = new LocalizedTextFieldDetails(msg.getEnabledLocales().values(), msg.getLocale());
		displayedName.setWidth(TEXT_FIELD_MEDIUM.value());
		formInformation = new LocalizedTextAreaDetails(msg.getEnabledLocales().values(), msg.getLocale());
		pageTitle =  new LocalizedTextFieldDetails(msg.getEnabledLocales().values(), msg.getLocale());
		displayedName.setWidth(TEXT_FIELD_MEDIUM.value());
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
	
	protected TabSheet createCollectedParamsTabs(List<Group> groups, boolean forceInteractiveRetrieval) throws EngineException
	{
		this.groups = groups.stream()
				.map(group -> new GroupModel(group.getDisplayedName().getValue(msg), group.getPathEncoded()))
				.toList();
		collectedParamsTabSheet = new TabSheet();

		optins = new ListOfEmbeddedElements<>(msg, new AgreementEditorAndProvider(), 0, 20, true);

		IdentityEditorAndProvider identityEditorAndProvider = new IdentityEditorAndProvider();
		if (forceInteractiveRetrieval)
			identityEditorAndProvider.fixRetrievalSettings(ParameterRetrievalSettings.interactive);
		identityParams = new ListOfEmbeddedElements<>(
				msg, identityEditorAndProvider, 0, 20, true);
		identityParams.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());
		Component localSignupMethods = createLocalSignupMethodsTab(forceInteractiveRetrieval);
		
		GroupEditorAndProvider groupEditorAndProvider = new GroupEditorAndProvider();
		if (forceInteractiveRetrieval)
			groupEditorAndProvider.fixRetrievalSettings(ParameterRetrievalSettings.interactive);
		groupParams = new ListOfEmbeddedElements<>(
				msg, groupEditorAndProvider, 0, 20, true);
		groupParams.setValueChangeListener(this::onGroupChanges);
		groupParams.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());

		AttributeEditorAndProvider attributeEditorAndProvider = new AttributeEditorAndProvider();
		if (forceInteractiveRetrieval)
			attributeEditorAndProvider.fixRetrievalSettings(ParameterRetrievalSettings.interactive);
		attributeParams = new ListOfEmbeddedElements<>(
				msg, attributeEditorAndProvider, 0, 20, true);
		attributeParams.addClassName(MEDIUM_VAADIN_FORM_ITEM_LABEL.getName());

		policyAgreements = policyAgreementConfigurationListFactory.getInstance();

		collectedParamsTabSheet.add(msg.getMessage("RegistrationFormEditor.identityParams"), identityParams);
		collectedParamsTabSheet.add(msg.getMessage("RegistrationFormEditor.localSignupMethods"), localSignupMethods);
		collectedParamsTabSheet.add(msg.getMessage("RegistrationFormEditor.groupParams"), groupParams);
		collectedParamsTabSheet.add(msg.getMessage("RegistrationFormEditor.attributeParams"), attributeParams);
		collectedParamsTabSheet.add(msg.getMessage("RegistrationFormEditor.optins"), optins);
		collectedParamsTabSheet.add(msg.getMessage("RegistrationFormEditor.policyAgreements"), policyAgreements);
		return collectedParamsTabSheet;
	}

	protected Component getWrapUpComponent(Predicate<TriggeringState> filter)
	{
		VerticalLayout wrapper = new VerticalLayout();
		wrapper.setPadding(false);
		wrapper.setSpacing(false);

		WrapupConfigEditorAndProvider groupEditorAndProvider = new WrapupConfigEditorAndProvider(filter);
		wrapUpConfig = new ListOfEmbeddedElements<>(null,
				msg, groupEditorAndProvider, 0, 20, true);
		wrapper.add(wrapUpConfig);
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
	
	private List<DynamicGroupParamWithLabel> getDynamicGroupsForProfile()
	{
		try
		{
			return Streams.mapWithIndex(groupParams.getElements().stream(),
					(gp, index) -> new DynamicGroupParamWithLabel(gp.getGroupPath(),
							Long.valueOf(index).intValue()))
					.collect(Collectors.toList());
		} catch (FormValidationException e)
		{
			return Collections.emptyList();
		}
	}
	
	protected void onGroupChanges()
	{
		attributeParams.refresh();
	}
	
	private Component createLocalSignupMethodsTab(boolean forceInteractiveRetrieval)
	{
		credentialParams = new ListOfEmbeddedElements<>(msg.getMessage("RegistrationFormEditor.credentialParams"),
				msg, new CredentialEditorAndProvider(), 0, 20, true);
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
		private Checkbox required;
		private LocalizedTextAreaDetails text;
		private String defValue;

		@Override
		public Editor<AgreementRegistrationParam> getEditor()
		{
			return new AgreementEditorAndProvider();
		}

		@Override
		public ComponentsContainer getEditorComponent(AgreementRegistrationParam value, int index)
		{
			required = new Checkbox(msg.getMessage("RegistrationFormEditor.mandatory"));
			text = new LocalizedTextAreaDetails(msg.getEnabledLocales().values(), msg.getLocale(), msg.getMessage("RegistrationFormViewer.agreement"));
			text.setWidth(TEXT_FIELD_BIG.value());
			defValue = null;
			if (value != null)
			{
				required.setValue(value.isManatory());
				text.setValue(value.getText().getLocalizedMap());
				defValue = value.getText().getDefaultValue();
			}
			return new ComponentsContainer(text, required);
		}

		@Override
		public AgreementRegistrationParam getValue()
		{
			AgreementRegistrationParam ret = new AgreementRegistrationParam();
			ret.setManatory(required.getValue());
			I18nString text = new I18nString(defValue);
			text.addAllMapValues(this.text.getValue());
			ret.setText(text);
			return ret;
		}

		@Override
		public void setEditedComponentPosition(int position) {}
	}
	
	private class IdentityEditorAndProvider extends OptionalParameterEditor 
			implements EditorProvider<IdentityRegistrationParam>, Editor<IdentityRegistrationParam>
	{
		private ComboBox<String> identityType;
		private ComboBox<ConfirmationMode> confirmationMode;
		private URLPrefillConfigEditor urlPrefillEditor;
		
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
			identityType = new NotEmptyComboBox<>(msg.getMessage("RegistrationFormViewer.paramIdentity"));
			identityType.setItems(allowedIdentitiesByName.keySet());
			identityType.setValue(allowedIdentitiesByName.keySet().iterator().next());
			identityType.setWidth(TEXT_FIELD_MEDIUM.value());
			confirmationMode = new EnumComboBox<>(
					msg.getMessage("RegistrationFormViewer.paramConfirmationMode"), 
					msg::getMessage,
					"ConfirmationMode.", 
					ConfirmationMode.class, 
					ConfirmationMode.ON_SUBMIT);
			confirmationMode.setTooltipText(msg.getMessage("RegistrationFormEditor.confirmationModeDesc"));
			confirmationMode.setWidth(TEXT_FIELD_MEDIUM.value());

			identityType.addValueChangeListener(val -> {
				IdentityTypeDefinition identityTypeDefinition = allowedIdentitiesByName.get(val.getValue());
				if(identityTypeDefinition == null)
					return;
				boolean emailVerifiable = identityTypeDefinition.isEmailVerifiable();
				confirmationMode.setVisible(emailVerifiable);
				confirmationMode.getParent().ifPresent(parent -> parent.setVisible(emailVerifiable));
			});
			
			urlPrefillEditor = new URLPrefillConfigEditor(msg);
			main.add(identityType, confirmationMode);
			if (value != null)
			{
				identityType.setValue(value.getIdentityType());
				confirmationMode.setValue(value.getConfirmationMode());	
				urlPrefillEditor.setValue(value.getUrlQueryPrefill());
			}
			boolean emailVerifiable = allowedIdentitiesByName.get(identityType.getValue()).isEmailVerifiable();
			confirmationMode.setVisible(emailVerifiable);
			initEditorComponent(value);
			main.add(urlPrefillEditor);
			return main;
		}

		@Override
		public IdentityRegistrationParam getValue() throws FormValidationException
		{
			urlPrefillEditor.valid();
			IdentityRegistrationParam ret = new IdentityRegistrationParam();
			ret.setIdentityType(identityType.getValue());
			ret.setConfirmationMode(confirmationMode.getValue());
			ret.setUrlQueryPrefill(urlPrefillEditor.getValue());
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
		private Checkbox showGroups;
		private EnumComboBox<ConfirmationMode> confirmationMode;
		private URLPrefillConfigEditor urlPrefillEditor;

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
					msg.getMessage("RegistrationFormViewer.paramAttribute"), attributeTypes, msg);
			attributeType.setWidth(TEXT_FIELD_MEDIUM.value());
			group = new FormAttributeGroupComboBox(msg.getMessage("RegistrationFormViewer.paramAttributeGroup"), 
					groups, getDynamicGroups());
			group.updateDynamicGroups(getDynamicGroups());
			group.setWidth(TEXT_FIELD_MEDIUM.value());
			showGroups = new Checkbox(msg.getMessage("RegistrationFormViewer.paramShowGroup"));
			confirmationMode = new EnumComboBox<>(
					msg.getMessage("RegistrationFormViewer.paramConfirmationMode"), 
					msg::getMessage,
					"ConfirmationMode.", 
					ConfirmationMode.class, 
					ConfirmationMode.ON_SUBMIT);
			confirmationMode.setTooltipText(msg.getMessage("RegistrationFormEditor.confirmationModeDesc"));
			confirmationMode.setWidth(TEXT_FIELD_MEDIUM.value());
			urlPrefillEditor = new URLPrefillConfigEditor(msg);
			main.add(attributeType, group, showGroups, confirmationMode);
			
			attributeType.addValueChangeListener(val ->
			{
				String syntaxId = val.getValue().getValueSyntax();
				boolean emailVerifiable = attributeTypeSupport.getUnconfiguredSyntax(syntaxId).isEmailVerifiable();
				confirmationMode.setVisible(emailVerifiable);
				confirmationMode.getParent().ifPresent(parent -> parent.setVisible(emailVerifiable));
			});
			if (value != null)
			{
				attributeType.setSelectedItemByName(value.getAttributeType());
				group.setValue(value.getGroup());
				showGroups.setValue(value.isShowGroups());
				confirmationMode.setValue(value.getConfirmationMode());
				urlPrefillEditor.setValue(value.getUrlQueryPrefill());
			}
			confirmationMode.setVisible(attributeTypeSupport.getUnconfiguredSyntax(
					attributeType.getValue().getValueSyntax()).isEmailVerifiable());
			initEditorComponent(value);
			main.add(urlPrefillEditor);
			return main;
		}

		@Override
		public AttributeRegistrationParam getValue() throws FormValidationException
		{
			urlPrefillEditor.valid();
			AttributeRegistrationParam ret = new AttributeRegistrationParam();
			ret.setAttributeType(attributeType.getValue().getName());
			ret.setGroup(group.getValue().path());
			ret.setShowGroups(showGroups.getValue());
			ret.setConfirmationMode(confirmationMode.getValue());
			ret.setUrlQueryPrefill(urlPrefillEditor.getValue());
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
		private Checkbox multiSelectable;
		private Select<IncludeGroupsMode> includeGroupsMode;
		
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
			group.setTooltipText(msg.getMessage("RegistrationFormEditor.paramGroupDesc"));
			group.addValueChangeListener(nv -> onGroupChanges());
			multiSelectable = new Checkbox(msg.getMessage("RegistrationFormEditor.paramGroupMulti"));
			multiSelectable.addValueChangeListener(nv -> onGroupChanges());
			
			includeGroupsMode = new Select<>();
			includeGroupsMode.setLabel(msg.getMessage("RegistrationFormViewer.paramGroupMode"));
			includeGroupsMode.setItemLabelGenerator(item -> msg.getMessage("GroupAccessMode." + item));
			includeGroupsMode.setItems(IncludeGroupsMode.values());
			includeGroupsMode.setValue(IncludeGroupsMode.all);
			
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
			credential = new NotEmptyComboBox<>(msg.getMessage("RegistrationFormViewer.paramCredential"));
			credential.setItems(credentialTypes);
			credential.setValue(credentialTypes.stream().findFirst().orElse(null));
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
		public CredentialRegistrationParam getValue()
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
		protected ComboBox<ParameterRetrievalSettings> retrievalSettings;
		protected ParameterRetrievalSettings fixedRetrievalSettings;

		protected void initEditorComponent(RegistrationParam value, Optional<String> defaultLabel)
		{
			label = new TextField(msg.getMessage("RegistrationFormViewer.paramLabel"));
			description = new TextField(msg.getMessage("RegistrationFormViewer.paramDescription"));
			retrievalSettings = new NotEmptyComboBox<>(msg.getMessage("RegistrationFormViewer.paramSettings"));
			retrievalSettings.setItemLabelGenerator(item -> msg.getMessage("ParameterRetrievalSettings." + item));
			retrievalSettings.setItems(ParameterRetrievalSettings.values());
			retrievalSettings.setValue(ParameterRetrievalSettings.interactive);
			retrievalSettings.setWidth(TEXT_FIELD_MEDIUM.value());
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
		protected Checkbox optional;

		protected void initEditorComponent(OptionalRegistrationParam value)
		{
			super.initEditorComponent(value, Optional.empty());
			optional = new Checkbox(msg.getMessage("RegistrationFormViewer.paramOptional"));
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
		private final Predicate<TriggeringState> filter;
		
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
