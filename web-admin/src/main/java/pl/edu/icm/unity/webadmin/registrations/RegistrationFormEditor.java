/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.registrations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.vaadin.ui.AbstractTextField;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.Reindeer;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.notifications.TemplatesStore;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.api.NotificationsManagement;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.authn.CredentialRequirements;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.IdentityType;
import pl.edu.icm.unity.types.registration.AgreementRegistrationParam;
import pl.edu.icm.unity.types.registration.AttributeClassAssignment;
import pl.edu.icm.unity.types.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.types.registration.CredentialRegistrationParam;
import pl.edu.icm.unity.types.registration.GroupRegistrationParam;
import pl.edu.icm.unity.types.registration.IdentityRegistrationParam;
import pl.edu.icm.unity.types.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationFormNotifications;
import pl.edu.icm.unity.types.registration.RegistrationParam;
import pl.edu.icm.unity.webui.common.DescriptionTextArea;
import pl.edu.icm.unity.webui.common.EnumComboBox;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.GroupComboBox;
import pl.edu.icm.unity.webui.common.ListOfEmbeddedElements;
import pl.edu.icm.unity.webui.common.ListOfEmbeddedElements.Editor;
import pl.edu.icm.unity.webui.common.NotNullComboBox;
import pl.edu.icm.unity.webui.common.RequiredTextField;
import pl.edu.icm.unity.webui.common.ListOfEmbeddedElements.EditorProvider;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.attributes.AttributeSelectionComboBox;
import pl.edu.icm.unity.webui.common.attributes.SelectableAttributeEditor;

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
	private TemplatesStore templatesStore;
	private AuthenticationManagement authenticationMan;
	private AttributeHandlerRegistry attrHandlerRegistry;
	private List<IdentityType> identityTypes;
	private Collection<AttributeType> attributeTypes;
	private List<String> groups;
	private List<String> credentialTypes;
	private Collection<String> attributeClasses;
	private boolean editMode;
	
	private TabSheet tabs;
	private CheckBox ignoreRequests;
	
	private AbstractTextField name;
	private DescriptionTextArea description;
	private CheckBox publiclyAvailable;
	private ComboBox submittedTemplate;
	private ComboBox updatedTemplate;
	private ComboBox rejectedTemplate;
	private ComboBox acceptedTemplate;
	private ComboBox channel;
	private GroupComboBox adminsNotificationGroup;
	
	private DescriptionTextArea formInformation;
	private TextField registrationCode;
	private CheckBox collectComments;
	private ListOfEmbeddedElements<AgreementRegistrationParam> agreements;	
	private ListOfEmbeddedElements<IdentityRegistrationParam> identityParams;
	private ListOfEmbeddedElements<AttributeRegistrationParam> attributeParams;
	private ListOfEmbeddedElements<GroupRegistrationParam> groupParams;
	private ListOfEmbeddedElements<CredentialRegistrationParam> credentialParams;

	private ComboBox credentialRequirementAssignment;
	private EnumComboBox<EntityState> initialState;
	private ListOfEmbeddedElements<Attribute<?>> attributeAssignments;
	private ListOfEmbeddedElements<String> groupAssignments;
	private ListOfEmbeddedElements<AttributeClassAssignment> attributeClassAssignments;
	
	public RegistrationFormEditor(UnityMessageSource msg, 
			GroupsManagement groupsMan, NotificationsManagement notificationsMan,
			UnityServerConfiguration cfg, IdentitiesManagement identitiesMan,
			AttributesManagement attributeMan, AuthenticationManagement authenticationMan,
			AttributeHandlerRegistry attrHandlerRegistry) throws EngineException
	{
		this(msg, groupsMan, notificationsMan, cfg, identitiesMan, attributeMan, authenticationMan, 
				attrHandlerRegistry, null);
	}

	public RegistrationFormEditor(UnityMessageSource msg, 
			GroupsManagement groupsMan, NotificationsManagement notificationsMan,
			UnityServerConfiguration cfg, IdentitiesManagement identitiesMan,
			AttributesManagement attributeMan, AuthenticationManagement authenticationMan,
			AttributeHandlerRegistry attrHandlerRegistry, RegistrationForm toEdit) throws EngineException
	{
		super();
		editMode = toEdit != null;
		this.msg = msg;
		this.groupsMan = groupsMan;
		this.notificationsMan = notificationsMan;
		this.templatesStore = cfg.getTemplatesStore();
		this.authenticationMan = authenticationMan;
		this.attrHandlerRegistry = attrHandlerRegistry;
		identityTypes = identitiesMan.getIdentityTypes(); 
		attributeTypes = attributeMan.getAttributeTypes();
		Collection<CredentialDefinition> crs = authenticationMan.getCredentialDefinitions();
		credentialTypes = new ArrayList<>(crs.size());
		for (CredentialDefinition cred: crs)
			credentialTypes.add(cred.getName());
		attributeClasses = attributeMan.getAttributeClasses().keySet(); 
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
		if (editMode)
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
		RegistrationForm ret = new RegistrationForm();
		
		ret.setAgreements(agreements.getElements());
		ret.setAttributeAssignments(attributeAssignments.getElements());
		ret.setAttributeClassAssignments(attributeClassAssignments.getElements());
		ret.setAttributeParams(attributeParams.getElements());
		ret.setCollectComments(collectComments.getValue());
		ret.setCredentialParams(credentialParams.getElements());
		ret.setCredentialRequirementAssignment((String) credentialRequirementAssignment.getValue());
		ret.setDescription(description.getValue());
		ret.setFormInformation(formInformation.getValue());
		ret.setGroupAssignments(groupAssignments.getElements());
		ret.setGroupParams(groupParams.getElements());
		ret.setIdentityParams(identityParams.getElements());
		ret.setInitialEntityState(initialState.getSelectedValue());
		ret.setName(name.getValue());
		
		RegistrationFormNotifications notCfg = ret.getNotificationsConfiguration();
		notCfg.setAcceptedTemplate((String) acceptedTemplate.getValue());
		notCfg.setAdminsNotificationGroup((String) adminsNotificationGroup.getValue());
		notCfg.setChannel((String) channel.getValue());
		notCfg.setRejectedTemplate((String) rejectedTemplate.getValue());
		notCfg.setSubmittedTemplate((String) submittedTemplate.getValue());
		notCfg.setUpdatedTemplate((String) updatedTemplate.getValue());
		ret.setPubliclyAvailable(publiclyAvailable.getValue());
		
		String code = registrationCode.getValue();
		if (code != null && !code.equals(""))
			ret.setRegistrationCode(code);
		
		return ret;
	}
	
	private void initMainTab(RegistrationForm toEdit) throws EngineException
	{
		FormLayout main = new FormLayout();
		VerticalLayout wrapper = new VerticalLayout(main);
		wrapper.setMargin(true);
		tabs.addTab(wrapper, msg.getMessage("RegistrationFormViewer.mainTab"));
		
		name = new RequiredTextField(msg);
		if (toEdit != null)
		{
			name.setValue(toEdit.getName());
			name.setReadOnly(true);
		} else
			name.setValue(msg.getMessage("RegistrationFormEditor.defaultName"));
		name.setCaption(msg.getMessage("RegistrationFormEditor.name"));
		
		description = new DescriptionTextArea(msg.getMessage("RegistrationFormViewer.description"));
		
		publiclyAvailable = new CheckBox(msg.getMessage("RegistrationFormEditor.publiclyAvailable"));
		
		channel = new ComboBox(msg.getMessage("RegistrationFormViewer.channel"));
		Set<String> channels = notificationsMan.getNotificationChannels().keySet();
		for (String c: channels)
			channel.addItem(c);
		
		adminsNotificationGroup = new GroupComboBox(
				msg.getMessage("RegistrationFormViewer.adminsNotificationsGroup"), groupsMan);
		adminsNotificationGroup.setNullSelectionAllowed(true);
		adminsNotificationGroup.setInput("/", true);
		this.groups = adminsNotificationGroup.getGroups();
		
		submittedTemplate = new ComboBox(msg.getMessage("RegistrationFormViewer.submittedTemplate"));
		updatedTemplate = new ComboBox(msg.getMessage("RegistrationFormViewer.updatedTemplate"));
		rejectedTemplate = new ComboBox(msg.getMessage("RegistrationFormViewer.rejectedTemplate"));
		acceptedTemplate = new ComboBox(msg.getMessage("RegistrationFormViewer.acceptedTemplate"));
		Set<String> templateIds = templatesStore.getTemplateIds();
		for (String template: templateIds)
		{
			submittedTemplate.addItem(template);
			updatedTemplate.addItem(template);
			rejectedTemplate.addItem(template);
			acceptedTemplate.addItem(template);
		}
		
		main.addComponents(name, description, publiclyAvailable, channel, adminsNotificationGroup,
				submittedTemplate, updatedTemplate, rejectedTemplate, acceptedTemplate);
		
		if (toEdit != null)
		{
			description.setValue(toEdit.getDescription());
			publiclyAvailable.setValue(toEdit.isPubliclyAvailable());
			RegistrationFormNotifications notCfg = toEdit.getNotificationsConfiguration();
			adminsNotificationGroup.setValue(notCfg.getAdminsNotificationGroup());
			channel.setValue(notCfg.getChannel());
			submittedTemplate.setValue(notCfg.getSubmittedTemplate());
			updatedTemplate.setValue(notCfg.getUpdatedTemplate());
			rejectedTemplate.setValue(notCfg.getRejectedTemplate());
			acceptedTemplate.setValue(notCfg.getAcceptedTemplate());
		}
	}
	
	private void initCollectedTab(RegistrationForm toEdit)
	{
		FormLayout main = new FormLayout();
		VerticalLayout wrapper = new VerticalLayout(main);
		wrapper.setMargin(true);
		tabs.addTab(wrapper, msg.getMessage("RegistrationFormViewer.collectedTab"));
		
		formInformation = new DescriptionTextArea(msg.getMessage("RegistrationFormViewer.formInformation"));
		registrationCode = new TextField(msg.getMessage("RegistrationFormViewer.registrationCode"));
		collectComments = new CheckBox(msg.getMessage("RegistrationFormEditor.collectComments"));
		
		TabSheet tabOfLists = new TabSheet();
		tabOfLists.setStyleName(Reindeer.TABSHEET_MINIMAL);
		
		agreements = new ListOfEmbeddedElements<>(msg.getMessage("RegistrationFormEditor.agreements"), 
				msg, new AgreementEditorAndProvider(), 0, 20);
		identityParams = new ListOfEmbeddedElements<>(msg.getMessage("RegistrationFormEditor.identityParams"),
				msg, new IdentityEditorAndProvider(), 1, 20);
		attributeParams = new ListOfEmbeddedElements<>(msg.getMessage("RegistrationFormEditor.attributeParams"),
				msg, new AttributeEditorAndProvider(), 0, 20);
		groupParams = new ListOfEmbeddedElements<>(msg.getMessage("RegistrationFormEditor.groupParams"),
				msg, new GroupEditorAndProvider(), 0, 20);
		credentialParams = new ListOfEmbeddedElements<>(msg.getMessage("RegistrationFormEditor.credentialParams"),
				msg, new CredentialEditorAndProvider(), 0, 20);
		main.addComponents(formInformation, registrationCode, collectComments, tabOfLists);
		tabOfLists.addComponents(agreements, identityParams, attributeParams, groupParams, credentialParams);
		
		if (toEdit != null)
		{
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
		FormLayout main = new FormLayout();
		VerticalLayout wrapper = new VerticalLayout(main);
		wrapper.setMargin(true);
		tabs.addTab(wrapper, msg.getMessage("RegistrationFormViewer.assignedTab"));
		
		credentialRequirementAssignment = new NotNullComboBox(
				msg.getMessage("RegistrationFormViewer.credentialRequirementAssignment"));
		Collection<CredentialRequirements> credentialRequirements = authenticationMan.getCredentialRequirements();
		for (CredentialRequirements cr: credentialRequirements)
			credentialRequirementAssignment.addItem(cr.getName());
		credentialRequirementAssignment.setNullSelectionAllowed(false);
		
		initialState = new EnumComboBox<EntityState>(msg.getMessage("RegistrationFormViewer.initialState"), 
				msg, "EntityState.", EntityState.class, EntityState.valid);
		
		TabSheet tabOfLists = new TabSheet();
		tabOfLists.setStyleName(Reindeer.TABSHEET_MINIMAL);
		attributeAssignments = new ListOfEmbeddedElements<>(msg.getMessage("RegistrationFormEditor.attributeAssignments"),
				msg, new AttributeAssignmentEditorAndProvider(), 0, 20);
		groupAssignments = new ListOfEmbeddedElements<>(msg.getMessage("RegistrationFormEditor.groupAssignments"),
				msg, new GroupAssignmentEditorAndProvider(), 0, 20);
		attributeClassAssignments = new ListOfEmbeddedElements<>(
				msg.getMessage("RegistrationFormEditor.attributeClassAssignments"), msg, 
				new ACAssignmentEditorAndProvider(), 0, 20);
		main.addComponents(credentialRequirementAssignment, initialState, tabOfLists);
		tabOfLists.addComponents(attributeAssignments, groupAssignments, attributeClassAssignments);
		
		if (toEdit!= null)
		{
			credentialRequirementAssignment.setValue(toEdit.getCredentialRequirementAssignment());
			initialState.setEnumValue(toEdit.getInitialEntityState());
			attributeAssignments.setEntries(toEdit.getAttributeAssignments());
			groupAssignments.setEntries(toEdit.getGroupAssignments());
			attributeClassAssignments.setEntries(toEdit.getAttributeClassAssignments());
		}
	}
	
	private class AgreementEditorAndProvider implements EditorProvider<AgreementRegistrationParam>,
		Editor<AgreementRegistrationParam>
	{
		private CheckBox required;
		private TextArea text;
		
		@Override
		public Editor<AgreementRegistrationParam> getEditor()
		{
			return new AgreementEditorAndProvider();
		}

		@Override
		public Component getEditorComponent(AgreementRegistrationParam value)
		{
			FormLayout ret = new FormLayout();
			required = new CheckBox(msg.getMessage("RegistrationFormEditor.mandatory"));
			text = new TextArea(msg.getMessage("RegistrationFormViewer.agreement"));
			ret.addComponents(text, required);
			if (value != null)
			{
				required.setValue(value.isManatory());
				text.setValue(value.getText());
			}
			return ret;
		}

		@Override
		public AgreementRegistrationParam getValue() throws FormValidationException
		{
			AgreementRegistrationParam ret = new AgreementRegistrationParam();
			ret.setManatory(required.getValue());
			ret.setText(text.getValue());
			return ret;
		}
	}
	
	private class IdentityEditorAndProvider extends ParameterEditor implements EditorProvider<IdentityRegistrationParam>,
			Editor<IdentityRegistrationParam>
	{
		private ComboBox identityType;

		@Override
		public Editor<IdentityRegistrationParam> getEditor()
		{
			return new IdentityEditorAndProvider();
		}

		@Override
		public Component getEditorComponent(IdentityRegistrationParam value)
		{
			identityType = new NotNullComboBox(msg.getMessage("RegistrationFormViewer.paramIdentity"));
			for (IdentityType it: identityTypes)
				identityType.addItem(it.getIdentityTypeProvider().getId());
			main.addComponent(identityType);
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
	}

	private class AttributeEditorAndProvider extends ParameterEditor implements EditorProvider<AttributeRegistrationParam>,
			Editor<AttributeRegistrationParam>
	{
		private AttributeSelectionComboBox attributeType;
		private GroupComboBox group;
		private CheckBox showGroups;
		private CheckBox useDescription;
		
		@Override
		public Editor<AttributeRegistrationParam> getEditor()
		{
			return new AttributeEditorAndProvider();
		}

		@Override
		public Component getEditorComponent(AttributeRegistrationParam value)
		{
			attributeType = new AttributeSelectionComboBox(
					msg.getMessage("RegistrationFormViewer.paramAttribute"), attributeTypes);
			group = new GroupComboBox(msg.getMessage("RegistrationFormViewer.paramAttributeGroup"), groups);
			group.setInput("/", true);
			showGroups = new CheckBox(msg.getMessage("RegistrationFormViewer.paramShowGroup"));
			useDescription = new CheckBox(msg.getMessage("RegistrationFormViewer.paramUseDescription"));
			
			main.addComponents(attributeType, group, showGroups, useDescription);
			
			if (value != null)
			{
				attributeType.setValue(value.getAttributeType());
				group.setValue(value.getGroup());
				showGroups.setValue(value.isShowGroups());
				useDescription.setValue(value.isUseDescription());
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
			ret.setUseDescription(useDescription.getValue());
			fill(ret);
			return ret;
		}
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
		public Component getEditorComponent(GroupRegistrationParam value)
		{
			group = new GroupComboBox(msg.getMessage("RegistrationFormViewer.paramGroup"), groups);
			group.setInput("/", true);
			if (value != null)
				group.setValue(value.getGroupPath());
			main.addComponent(group);
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
		public Component getEditorComponent(CredentialRegistrationParam value)
		{
			FormLayout main = new FormLayout();
			
			credential = new NotNullComboBox(msg.getMessage("RegistrationFormViewer.paramCredential"));
			for (String c: credentialTypes)
				credential.addItem(c);
			label = new TextField(msg.getMessage("RegistrationFormViewer.paramLabel"));
			description = new TextField(msg.getMessage("RegistrationFormViewer.paramDescription"));

			main.addComponents(credential, label, description);

			if (value != null)
			{
				credential.setValue(value.getCredentialName());
				label.setValue(value.getLabel());
				description.setValue(value.getDescription());
			}
			return main;
		}

		@Override
		public CredentialRegistrationParam getValue() throws FormValidationException
		{
			CredentialRegistrationParam ret = new CredentialRegistrationParam();
			ret.setCredentialName((String) credential.getValue());
			ret.setLabel(label.getValue());
			ret.setDescription(description.getValue());
			return ret;
		}
	}

	
	private abstract class ParameterEditor 
	{
		protected FormLayout main = new FormLayout();
		protected TextField label;
		protected TextField description;
		protected CheckBox optional;
		protected EnumComboBox<ParameterRetrievalSettings> retrievalSettings;

		protected void initEditorComponent(RegistrationParam value)
		{
			label = new TextField(msg.getMessage("RegistrationFormViewer.paramLabel"));
			description = new TextField(msg.getMessage("RegistrationFormViewer.paramDescription"));
			optional = new CheckBox(msg.getMessage("RegistrationFormViewer.paramOptional"));
			retrievalSettings = new EnumComboBox<ParameterRetrievalSettings>(
					msg.getMessage("RegistrationFormViewer.paramSettings"), msg, 
					"ParameterRetrievalSettings.", ParameterRetrievalSettings.class, 
					ParameterRetrievalSettings.interactive);
			main.addComponents(label, description, optional, retrievalSettings);
			
			if (value != null)
			{
				label.setValue(value.getLabel());
				description.setValue(value.getDescription());
				optional.setValue(value.isOptional());
				retrievalSettings.setEnumValue(value.getRetrievalSettings());
			}
		}
		
		protected void fill(RegistrationParam v)
		{
			v.setDescription(description.getValue());
			v.setLabel(label.getValue());
			v.setOptional(optional.getValue());
			v.setRetrievalSettings(retrievalSettings.getSelectedValue());
		}
	}
	
	
	private class GroupAssignmentEditorAndProvider implements EditorProvider<String>,
			Editor<String>
	{
		private GroupComboBox group;

		@Override
		public Editor<String> getEditor()
		{
			return new GroupAssignmentEditorAndProvider();
		}

		@Override
		public Component getEditorComponent(String value)
		{
			FormLayout main = new FormLayout();
			group = new GroupComboBox(msg.getMessage("RegistrationFormViewer.paramGroup"), groups);
			group.setInput("/", true);
			if (value != null)
				group.setValue(value);
			main.addComponent(group);
			return main;
		}

		@Override
		public String getValue() throws FormValidationException
		{
			return (String) group.getValue();
		}
	}

	private class ACAssignmentEditorAndProvider implements EditorProvider<AttributeClassAssignment>,
				Editor<AttributeClassAssignment>
	{
		private GroupComboBox group;
		private ComboBox ac;

		@Override
		public Editor<AttributeClassAssignment> getEditor()
		{
			return new ACAssignmentEditorAndProvider();
		}

		@Override
		public Component getEditorComponent(AttributeClassAssignment value)
		{
			FormLayout main = new FormLayout();
			group = new GroupComboBox(msg.getMessage("RegistrationFormViewer.paramGroup"), groups);
			group.setInput("/", true);
			
			ac = new NotNullComboBox(msg.getMessage("RegistrationFormViewer.assignedAC")); 
			for (String a: attributeClasses)
				ac.addItem(a);
			if (value != null)
			{
				group.setValue(value.getGroup());
				ac.setValue(value.getAcName());
			}
			main.addComponents(ac, group);
			return main;
		}

		@Override
		public AttributeClassAssignment getValue() throws FormValidationException
		{
			AttributeClassAssignment ret = new AttributeClassAssignment();
			ret.setGroup((String) group.getValue());
			ret.setAcName((String) ac.getValue());
			return ret;
		}
	}
	
	private class AttributeAssignmentEditorAndProvider implements EditorProvider<Attribute<?>>,
			Editor<Attribute<?>>
	{
		private SelectableAttributeEditor ae;
		
		@Override
		public Editor<Attribute<?>> getEditor()
		{
			return new AttributeAssignmentEditorAndProvider();
		}

		@Override
		public Component getEditorComponent(Attribute<?> value)
		{
			ae = new SelectableAttributeEditor(msg, attrHandlerRegistry, attributeTypes, true, groups);
			return ae;
		}

		@Override
		public Attribute<?> getValue() throws FormValidationException
		{
			return ae.getAttribute();
		}
	}

}
