/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.formman;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.api.MessageTemplateManagement;
import pl.edu.icm.unity.server.api.NotificationsManagement;
import pl.edu.icm.unity.server.api.registration.AcceptRegistrationTemplateDef;
import pl.edu.icm.unity.server.api.registration.InvitationTemplateDef;
import pl.edu.icm.unity.server.api.registration.RejectRegistrationTemplateDef;
import pl.edu.icm.unity.server.api.registration.SubmitRegistrationTemplateDef;
import pl.edu.icm.unity.server.api.registration.UpdateRegistrationTemplateDef;
import pl.edu.icm.unity.server.registries.RegistrationTranslationActionsRegistry;
import pl.edu.icm.unity.server.translation.form.RegistrationTranslationProfile;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.authn.CredentialRequirements;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.IdentityType;
import pl.edu.icm.unity.types.registration.AgreementRegistrationParam;
import pl.edu.icm.unity.types.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.types.registration.CredentialRegistrationParam;
import pl.edu.icm.unity.types.registration.GroupRegistrationParam;
import pl.edu.icm.unity.types.registration.IdentityRegistrationParam;
import pl.edu.icm.unity.types.registration.OptionalRegistrationParam;
import pl.edu.icm.unity.types.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationFormBuilder;
import pl.edu.icm.unity.types.registration.RegistrationFormNotifications;
import pl.edu.icm.unity.types.registration.RegistrationParam;
import pl.edu.icm.unity.webadmin.tprofile.RegistrationTranslationProfileEditor;
import pl.edu.icm.unity.webadmin.tprofile.TranslationProfileEditor;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.CompatibleTemplatesComboBox;
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

import com.vaadin.data.Validator;
import com.vaadin.data.validator.AbstractValidator;
import com.vaadin.ui.AbstractTextField;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Slider;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

/**
 * Allows to edit a registration form. Can be configured to edit an existing form (name is fixed)
 * or to create a new one (name can be chosen).
 * 
 * @author K. Benedyczak
 */
public class RegistrationFormEditor extends VerticalLayout
{
	private UnityMessageSource msg;
	private GroupsManagement groupsMan;
	private NotificationsManagement notificationsMan;
	private MessageTemplateManagement msgTempMan;
	private AuthenticationManagement authenticationMan;
	private Collection<IdentityType> identityTypes;
	private Collection<AttributeType> attributeTypes;
	private List<String> groups;
	private List<String> credentialTypes;
	private boolean editMode;
	private boolean copyMode;
	
	private TabSheet tabs;
	private CheckBox ignoreRequests;
	
	private AbstractTextField name;
	private DescriptionTextArea description;
	private CheckBox publiclyAvailable;
	private CheckBox byInvitationOnly;
	private ComboBox submittedTemplate;
	private ComboBox updatedTemplate;
	private ComboBox rejectedTemplate;
	private ComboBox acceptedTemplate;
	private ComboBox invitationTemplate;
	private ComboBox channel;
	private GroupComboBox adminsNotificationGroup;
	private Slider captcha;
	
	private I18nTextField displayedName;
	private I18nTextArea formInformation;
	private TextField registrationCode;
	private CheckBox collectComments;
	private ListOfEmbeddedElements<AgreementRegistrationParam> agreements;	
	private ListOfEmbeddedElements<IdentityRegistrationParam> identityParams;
	private ListOfEmbeddedElements<AttributeRegistrationParam> attributeParams;
	private ListOfEmbeddedElements<GroupRegistrationParam> groupParams;
	private ListOfEmbeddedElements<CredentialRegistrationParam> credentialParams;

	private ComboBox credentialRequirementAssignment;
	private TranslationProfileEditor profileEditor;
	private AttributesManagement attributeMan;
	private IdentitiesManagement identitiesMan;
	private RegistrationTranslationActionsRegistry actionsRegistry;
	
	public RegistrationFormEditor(UnityMessageSource msg, GroupsManagement groupsMan,
			NotificationsManagement notificationsMan,
			MessageTemplateManagement msgTempMan, IdentitiesManagement identitiesMan,
			AttributesManagement attributeMan,
			AuthenticationManagement authenticationMan, RegistrationTranslationActionsRegistry actionsRegistry) 
					throws EngineException
	{
		this(msg, groupsMan, notificationsMan, msgTempMan, identitiesMan, attributeMan, authenticationMan, 
				actionsRegistry, null, false);
	}

	public RegistrationFormEditor(UnityMessageSource msg, GroupsManagement groupsMan,
			NotificationsManagement notificationsMan,
			MessageTemplateManagement msgTempMan, IdentitiesManagement identitiesMan,
			AttributesManagement attributeMan,
			AuthenticationManagement authenticationMan, RegistrationTranslationActionsRegistry actionsRegistry,
			RegistrationForm toEdit, boolean copyMode)
			throws EngineException
	{
		super();
		this.identitiesMan = identitiesMan;
		this.attributeMan = attributeMan;
		this.actionsRegistry = actionsRegistry;
		editMode = toEdit != null;
		this.copyMode = editMode && copyMode;
		this.msg = msg;
		this.groupsMan = groupsMan;
		this.notificationsMan = notificationsMan;
		this.msgTempMan = msgTempMan;
		this.authenticationMan = authenticationMan;
		identityTypes = identitiesMan.getIdentityTypes(); 
		attributeTypes = attributeMan.getAttributeTypes();
		Collection<CredentialDefinition> crs = authenticationMan.getCredentialDefinitions();
		credentialTypes = new ArrayList<>(crs.size());
		for (CredentialDefinition cred: crs)
			credentialTypes.add(cred.getName());
		initUI(toEdit);
	}

	private void initUI(RegistrationForm toEdit) throws EngineException
	{
		setWidth(100, Unit.PERCENTAGE);
		setHeight(100, Unit.PERCENTAGE);
		setSpacing(true);
		tabs = new TabSheet();
		initMainTab(toEdit);
		initCollectedTab(toEdit);
		initAssignedTab(toEdit);
		ignoreRequests = new CheckBox(msg.getMessage("RegistrationFormEditDialog.ignoreRequests"));
		if (editMode && !copyMode)
		{
			addComponent(ignoreRequests);
			setComponentAlignment(ignoreRequests, Alignment.TOP_RIGHT);
		}
		addComponent(tabs);
		setComponentAlignment(tabs, Alignment.TOP_LEFT);
		setExpandRatio(tabs, 1);
	}
	
	public boolean isIgnoreRequests()
	{
		return ignoreRequests.getValue();
	}
	
	public RegistrationForm getForm() throws FormValidationException
	{
		try
		{
			publiclyAvailable.validate();
		} catch (Validator.InvalidValueException e)
		{
			throw new FormValidationException(e.getMessage(), e);
		}
		RegistrationFormBuilder builder = new RegistrationFormBuilder();	
		builder.withAgreements(agreements.getElements());
		builder.withAttributeParams(attributeParams.getElements());
		builder.withCollectComments(collectComments.getValue());
		builder.withCredentialParams(credentialParams.getElements());
		builder.withDefaultCredentialRequirement((String) credentialRequirementAssignment.getValue());
		builder.withTranslationProfile(profileEditor.getProfile());
		builder.withDescription(description.getValue());
		I18nString displayedNameStr = displayedName.getValue();
		displayedNameStr.setDefaultValue(name.getValue());
		builder.withDisplayedName(displayedNameStr);
		builder.withFormInformation(formInformation.getValue());
		builder.withGroupParams(groupParams.getElements());
		builder.withIdentityParams(identityParams.getElements());
		builder.withName(name.getValue());
		builder.withCaptchaLength(captcha.getValue().intValue());
		builder.withPubliclyAvailable(publiclyAvailable.getValue());
		builder.withByInvitationOnly(byInvitationOnly.getValue());
		
		RegistrationFormNotifications notCfg = new RegistrationFormNotifications();
		notCfg.setAcceptedTemplate((String) acceptedTemplate.getValue());
		notCfg.setAdminsNotificationGroup((String) adminsNotificationGroup.getValue());
		notCfg.setChannel((String) channel.getValue());
		notCfg.setRejectedTemplate((String) rejectedTemplate.getValue());
		notCfg.setSubmittedTemplate((String) submittedTemplate.getValue());
		notCfg.setUpdatedTemplate((String) updatedTemplate.getValue());
		notCfg.setInvitationTemplate((String) invitationTemplate.getValue());
		builder.withNotificationsConfiguration(notCfg);
		
		String code = registrationCode.getValue();
		if (code != null && !code.equals(""))
			builder.withRegistrationCode(code);
		
		return builder.build();
	}
	
	private void initMainTab(RegistrationForm toEdit) throws EngineException
	{
		FormLayout main = new CompactFormLayout();
		VerticalLayout wrapper = new VerticalLayout(main);
		wrapper.setMargin(true);
		tabs.addTab(wrapper, msg.getMessage("RegistrationFormViewer.mainTab"));
		
		name = new RequiredTextField(msg);
		name.setCaption(msg.getMessage("RegistrationFormEditor.name"));
		if (editMode)
		{
			if (!copyMode)
			{	
				name.setValue(toEdit.getName());
				name.setReadOnly(true);
			} else
			{
				name.setValue(msg.getMessage("RegistrationFormEditor.copyPrefix")
						+ toEdit.getName());
			}
		} else
		{
			name.setValue(msg.getMessage("RegistrationFormEditor.defaultName"));
		}
				
		description = new DescriptionTextArea(msg.getMessage("RegistrationFormViewer.description"));
		
		publiclyAvailable = new CheckBox(msg.getMessage("RegistrationFormEditor.publiclyAvailable"));
		publiclyAvailable.addValidator(new AbstractValidator<Boolean>(msg
				.getMessage("RegistrationFormEditor.publiclyValidationFalse"))
		{

			@Override
			protected boolean isValidValue(Boolean value)
			{
				RegistrationForm empty;
				try
				{
					RegistrationFormBuilder formBuilder = new RegistrationFormBuilder();
					formBuilder.withGroupParams(groupParams.getElements());
					formBuilder.withAttributeParams(attributeParams.getElements());
					formBuilder.withIdentityParams(identityParams.getElements());
					formBuilder.withName("test").withDefaultCredentialRequirement("testcr");
					empty = formBuilder.build();
				} catch (FormValidationException e)
				{
					return false;
				}

				if (value == true && empty.containsAutomaticAndMandatoryParams())
					return false;
				return true;
			}

			@Override
			public Class<Boolean> getType()
			{
				return Boolean.class;
			}
		});
		publiclyAvailable.setValidationVisible(true);
		publiclyAvailable.setImmediate(true);
		publiclyAvailable.addValueChangeListener(event -> {
			byInvitationOnly.setEnabled(publiclyAvailable.getValue());
		});
		
		byInvitationOnly = new CheckBox(msg.getMessage("RegistrationFormEditor.byInvitationOnly"));
		byInvitationOnly.setEnabled(false);
		
		channel = new ComboBox(msg.getMessage("RegistrationFormViewer.channel"));
		Set<String> channels = notificationsMan.getNotificationChannels().keySet();
		for (String c: channels)
			channel.addItem(c);
		
		adminsNotificationGroup = new GroupComboBox(
				msg.getMessage("RegistrationFormViewer.adminsNotificationsGroup"), groupsMan);
		adminsNotificationGroup.setNullSelectionAllowed(true);
		adminsNotificationGroup.setInput("/", true);
		this.groups = adminsNotificationGroup.getGroups();
		
		
		submittedTemplate = new CompatibleTemplatesComboBox(SubmitRegistrationTemplateDef.NAME, msgTempMan);
		submittedTemplate.setCaption(msg.getMessage("RegistrationFormViewer.submittedTemplate"));
		updatedTemplate =  new CompatibleTemplatesComboBox(UpdateRegistrationTemplateDef.NAME, msgTempMan);
		updatedTemplate.setCaption(msg.getMessage("RegistrationFormViewer.updatedTemplate"));
		rejectedTemplate =  new CompatibleTemplatesComboBox(RejectRegistrationTemplateDef.NAME, msgTempMan);
		rejectedTemplate.setCaption(msg.getMessage("RegistrationFormViewer.rejectedTemplate"));
		acceptedTemplate =  new CompatibleTemplatesComboBox(AcceptRegistrationTemplateDef.NAME, msgTempMan);
		acceptedTemplate.setCaption(msg.getMessage("RegistrationFormViewer.acceptedTemplate"));
		invitationTemplate =  new CompatibleTemplatesComboBox(InvitationTemplateDef.NAME, msgTempMan);
		invitationTemplate.setCaption(msg.getMessage("RegistrationFormViewer.invitationTemplate"));
		
		captcha = new Slider(msg.getMessage("RegistrationFormViewer.captcha"), 0, 8);
		captcha.setWidth(10, Unit.EM);
		captcha.setDescription(msg.getMessage("RegistrationFormEditor.captchaDescription"));
		
		main.addComponents(name, description, publiclyAvailable, byInvitationOnly, channel, adminsNotificationGroup,
				submittedTemplate, updatedTemplate, rejectedTemplate, acceptedTemplate, 
				invitationTemplate, captcha);
		
		if (toEdit != null)
		{
			description.setValue(toEdit.getDescription() != null ? toEdit.getDescription() : "");
			publiclyAvailable.setValue(toEdit.isPubliclyAvailable());
			byInvitationOnly.setValue(toEdit.isByInvitationOnly());
			RegistrationFormNotifications notCfg = toEdit.getNotificationsConfiguration();
			adminsNotificationGroup.setValue(notCfg.getAdminsNotificationGroup());
			channel.setValue(notCfg.getChannel());
			submittedTemplate.setValue(notCfg.getSubmittedTemplate());
			updatedTemplate.setValue(notCfg.getUpdatedTemplate());
			rejectedTemplate.setValue(notCfg.getRejectedTemplate());
			acceptedTemplate.setValue(notCfg.getAcceptedTemplate());
			invitationTemplate.setValue(notCfg.getInvitationTemplate());
			captcha.setValue(Double.valueOf(toEdit.getCaptchaLength()));
		}
	}
	
	private void initCollectedTab(RegistrationForm toEdit)
	{
		FormLayout main = new CompactFormLayout();
		VerticalLayout wrapper = new VerticalLayout(main);
		wrapper.setMargin(true);
		tabs.addTab(wrapper, msg.getMessage("RegistrationFormViewer.collectedTab"));
		
		displayedName = new I18nTextField(msg, msg.getMessage("RegistrationFormViewer.displayedName"));
		formInformation = new I18nTextArea(msg, msg.getMessage("RegistrationFormViewer.formInformation"));
		registrationCode = new TextField(msg.getMessage("RegistrationFormViewer.registrationCode"));
		collectComments = new CheckBox(msg.getMessage("RegistrationFormEditor.collectComments"));
		
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
		main.addComponents(displayedName, formInformation, registrationCode, collectComments, tabOfLists);
		tabOfLists.addComponents(agreements, identityParams, attributeParams, groupParams, credentialParams);
		
		if (toEdit != null)
		{
			displayedName.setValue(toEdit.getDisplayedName());
			formInformation.setValue(toEdit.getFormInformation());
			if (toEdit.getRegistrationCode() != null)
				registrationCode.setValue(toEdit.getRegistrationCode());
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
	}
	
	private void initAssignedTab(RegistrationForm toEdit) throws EngineException
	{
		FormLayout main = new CompactFormLayout();
		VerticalLayout wrapper = new VerticalLayout(main);
		wrapper.setMargin(true);
		tabs.addTab(wrapper, msg.getMessage("RegistrationFormViewer.assignedTab"));
		
		credentialRequirementAssignment = new NotNullComboBox(
				msg.getMessage("RegistrationFormViewer.credentialRequirementAssignment"));
		Collection<CredentialRequirements> credentialRequirements = authenticationMan.getCredentialRequirements();
		for (CredentialRequirements cr: credentialRequirements)
			credentialRequirementAssignment.addItem(cr.getName());
		credentialRequirementAssignment.setNullSelectionAllowed(false);
		
		RegistrationTranslationProfile profile = toEdit == null ? 
				new RegistrationTranslationProfile("form profile", new ArrayList<>(), actionsRegistry) : 
				new RegistrationTranslationProfile(toEdit.getTranslationProfile().getName(), 
						toEdit.getTranslationProfile().getRules(), actionsRegistry);
		profileEditor = new RegistrationTranslationProfileEditor(msg, actionsRegistry, profile, 
				attributeMan, identitiesMan, authenticationMan, groupsMan);
		
		main.addComponents(credentialRequirementAssignment);
		wrapper.addComponent(profileEditor);
		
		if (toEdit!= null)
		{
			credentialRequirementAssignment.setValue(toEdit.getDefaultCredentialRequirement());
		}
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
