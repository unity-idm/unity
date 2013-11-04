/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.formman;

import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.notifications.TemplatesStore;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.registration.AgreementRegistrationParam;
import pl.edu.icm.unity.types.registration.AttributeClassAssignment;
import pl.edu.icm.unity.types.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.types.registration.CredentialRegistrationParam;
import pl.edu.icm.unity.types.registration.GroupRegistrationParam;
import pl.edu.icm.unity.types.registration.IdentityRegistrationParam;
import pl.edu.icm.unity.types.registration.OptionalRegistrationParam;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationFormNotifications;
import pl.edu.icm.unity.types.registration.RegistrationParam;
import pl.edu.icm.unity.webadmin.notifications.TemplateViewer;
import pl.edu.icm.unity.webui.common.DescriptionTextArea;
import pl.edu.icm.unity.webui.common.ListOfElements;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;

/**
 * Read only UI displaying a {@link RegistrationForm}.
 * 
 * @author K. Benedyczak
 */
public class RegistrationFormViewer extends VerticalLayout
{
	private UnityMessageSource msg;
	private AttributeHandlerRegistry attrHandlerRegistry;
	private TemplatesStore templatesStore;
	
	private TabSheet tabs;
	
	private Label name;
	private DescriptionTextArea description;
	private Label publiclyAvailable;
	private TemplateViewer submittedTemplate;
	private TemplateViewer updatedTemplate;
	private TemplateViewer rejectedTemplate;
	private TemplateViewer acceptedTemplate;
	private Label channel;
	private Label adminsNotificationGroup;
	
	private Label formInformation;
	private Label registrationCode;
	private Label collectComments;
	private ListOfElements<AgreementRegistrationParam> agreements;	
	private ListOfElements<IdentityRegistrationParam> identityParams;
	private ListOfElements<AttributeRegistrationParam> attributeParams;
	private ListOfElements<GroupRegistrationParam> groupParams;
	private ListOfElements<CredentialRegistrationParam> credentialParams;	
	
	private Label credentialRequirementAssignment;
	private Label initialState;
	private ListOfElements<Attribute<?>> attributeAssignments;
	private ListOfElements<String> groupAssignments;
	private ListOfElements<AttributeClassAssignment> attributeClassAssignments;
	
	public RegistrationFormViewer(UnityMessageSource msg, AttributeHandlerRegistry attrHandlerRegistry,
			TemplatesStore templatesStore)
	{
		this.msg = msg;
		this.attrHandlerRegistry = attrHandlerRegistry;
		this.templatesStore = templatesStore;
		initUI();
	}
	
	public void setInput(RegistrationForm form)
	{
		setEmpty();
		if (form == null)
		{
			tabs.setVisible(false);
			return;
		}
		tabs.setVisible(true);
		
		name.setValue(form.getName());
		description.setValue(form.getDescription());
		publiclyAvailable.setValue(msg.getYesNo(form.isPubliclyAvailable()));
		
		RegistrationFormNotifications notCfg = form.getNotificationsConfiguration();
		if (notCfg != null)
		{
			submittedTemplate.setInput(notCfg.getSubmittedTemplate());
			updatedTemplate.setInput(notCfg.getUpdatedTemplate());
			rejectedTemplate.setInput(notCfg.getRejectedTemplate());
			acceptedTemplate.setInput(notCfg.getAcceptedTemplate());
			channel.setValue(notCfg.getChannel());
			adminsNotificationGroup.setValue(notCfg.getAdminsNotificationGroup());
		}
		
		formInformation.setValue(form.getFormInformation());
		String code = form.getRegistrationCode() == null ? "-" : form.getRegistrationCode();
		registrationCode.setValue(code);
		collectComments.setValue(msg.getYesNo(form.isCollectComments()));
		for (AgreementRegistrationParam ap: form.getAgreements())
			agreements.addEntry(ap);
		for (IdentityRegistrationParam ip: form.getIdentityParams())
			identityParams.addEntry(ip);
		for (AttributeRegistrationParam ap: form.getAttributeParams())
			attributeParams.addEntry(ap);
		for (GroupRegistrationParam gp: form.getGroupParams())
			groupParams.addEntry(gp);
		for (CredentialRegistrationParam cp: form.getCredentialParams())
			credentialParams.addEntry(cp);
		
		credentialRequirementAssignment.setValue(form.getCredentialRequirementAssignment());
		initialState.setValue(msg.getMessage("EntityState." + form.getInitialEntityState()));
		for (Attribute<?> a: form.getAttributeAssignments())
			attributeAssignments.addEntry(a);
		for (String g: form.getGroupAssignments())
			groupAssignments.addEntry(g);
		for (AttributeClassAssignment aa: form.getAttributeClassAssignments())
			attributeClassAssignments.addEntry(aa);
	}
	
	private void setEmpty()
	{
		name.setValue("");
		description.setValue("");
		publiclyAvailable.setValue("");
		
		submittedTemplate.setInput(null);
		updatedTemplate.setInput(null);
		rejectedTemplate.setInput(null);
		acceptedTemplate.setInput(null);
		channel.setValue("");
		adminsNotificationGroup.setValue("");
		
		formInformation.setValue("");
		registrationCode.setValue("");
		collectComments.setValue("");
		agreements.clearContents();
		identityParams.clearContents();
		attributeParams.clearContents();
		groupParams.clearContents();
		credentialParams.clearContents();
		
		credentialRequirementAssignment.setValue("");
		initialState.setValue("");
		attributeAssignments.clearContents();
		groupAssignments.clearContents();
		attributeClassAssignments.clearContents();
	}
	
	private void initUI()
	{
		tabs = new TabSheet();
		initMainTab();
		initCollectedTab();
		initAssignedTab();
		addComponent(tabs);
	}
	
	private void initCollectedTab()
	{
		FormLayout main = new FormLayout();
		VerticalLayout wrapper = new VerticalLayout(main);
		wrapper.setMargin(true);
		wrapper.setSpacing(true);

		tabs.addTab(wrapper, msg.getMessage("RegistrationFormViewer.collectedTab"));
		
		formInformation = new Label();
		formInformation.setContentMode(ContentMode.HTML);
		formInformation.setCaption(msg.getMessage("RegistrationFormViewer.formInformation"));
		registrationCode = new Label();
		registrationCode.setCaption(msg.getMessage("RegistrationFormViewer.registrationCode"));
		collectComments = new Label();
		collectComments.setCaption(msg.getMessage("RegistrationFormViewer.collectComments"));

		agreements = new ListOfElements<>(msg, new ListOfElements.LabelConverter<AgreementRegistrationParam>()
		{
			@Override
			public Label toLabel(AgreementRegistrationParam value)
			{
				String content = getOptionalStr(!value.isManatory()) + "<br>" + value.getText();
				return new Label(content, ContentMode.HTML);
			}
		});
		agreements.setMargin(true);
		Panel agreementsP = new Panel(msg.getMessage("RegistrationFormViewer.agreements"), agreements);

		identityParams = new ListOfElements<>(msg, new ListOfElements.LabelConverter<IdentityRegistrationParam>()
		{
			@Override
			public Label toLabel(IdentityRegistrationParam value)
			{
				String content = msg.getMessage("RegistrationFormViewer.identityType", value.getIdentityType())
						 + "<br>" + toHTMLLabel(value);
				return new Label(content, ContentMode.HTML);
			}
		});
		identityParams.setMargin(true);
		Panel identityParamsP = new Panel(msg.getMessage("RegistrationFormViewer.identityParams"), identityParams);
		
		attributeParams = new ListOfElements<>(msg, new ListOfElements.LabelConverter<AttributeRegistrationParam>()
		{
			@Override
			public Label toLabel(AttributeRegistrationParam value)
			{
				String useDescription = value.isUseDescription() ? 
						("[" + msg.getMessage("RegistrationFormViewer.useAttributeTypeDescription") +"]") : ""; 
				String showGroup = value.isShowGroups() ? 
						"[" + msg.getMessage("RegistrationFormViewer.showAttributeGroup")+"]" : ""; 
				String content = value.getAttributeType() + " @ " + value.getGroup() + " " +
						useDescription + " " + showGroup  + 
						"<br>" + toHTMLLabel(value);
				return new Label(content, ContentMode.HTML);
			}
		});
		attributeParams.setMargin(true);
		Panel attributeParamsP = new Panel(msg.getMessage("RegistrationFormViewer.attributeParams"), 
				attributeParams);
		
		
		groupParams = new ListOfElements<>(msg, new ListOfElements.LabelConverter<GroupRegistrationParam>()
		{
			@Override
			public Label toLabel(GroupRegistrationParam value)
			{
				String content = value.getGroupPath()  + "<br>" + toHTMLLabel(value);
				return new Label(content, ContentMode.HTML);
			}
		});
		groupParams.setMargin(true);
		Panel groupParamsP = new Panel(msg.getMessage("RegistrationFormViewer.groupParams"), groupParams);
		
		credentialParams = new ListOfElements<>(msg, new ListOfElements.LabelConverter<CredentialRegistrationParam>()
		{
			@Override
			public Label toLabel(CredentialRegistrationParam value)
			{
				String content = value.getLabel() + "<br>" + 
						value.getDescription() + "<br>" + 
						value.getCredentialName();
				return new Label(content, ContentMode.HTML);
			}
		});
		credentialParams.setMargin(true);
		Panel credentialParamsP = new Panel(msg.getMessage("RegistrationFormViewer.credentialParams"), 
				credentialParams);
		
		main.addComponents(formInformation, registrationCode, collectComments);
		wrapper.addComponents(agreementsP, identityParamsP, attributeParamsP, groupParamsP, credentialParamsP);
	}
	
	private void initAssignedTab()
	{
		FormLayout main = new FormLayout();
		VerticalLayout wrapper = new VerticalLayout(main);
		wrapper.setMargin(true);
		wrapper.setSpacing(true);
		tabs.addTab(wrapper, msg.getMessage("RegistrationFormViewer.assignedTab"));
		
		credentialRequirementAssignment = new Label();
		credentialRequirementAssignment.setCaption(msg.getMessage("RegistrationFormViewer.credentialRequirementAssignment"));
		initialState = new Label();
		initialState.setCaption(msg.getMessage("RegistrationFormViewer.initialState"));

		attributeAssignments = new ListOfElements<>(msg, new ListOfElements.LabelConverter<Attribute<?>>()
		{
			@Override
			public Label toLabel(Attribute<?> value)
			{
				String content = attrHandlerRegistry.getSimplifiedAttributeRepresentation(
						value, 64) + " @ " + value.getGroupPath();
				return new Label(content, ContentMode.HTML);
			}
		});
		attributeAssignments.setMargin(true);
		Panel attributeAssignmentsP = new Panel(msg.getMessage("RegistrationFormViewer.attributeAssignments"),
				attributeAssignments);

		groupAssignments = new ListOfElements<>(msg, new ListOfElements.LabelConverter<String>()
		{
			@Override
			public Label toLabel(String value)
			{
				return new Label(value);
			}
		});
		groupAssignments.setMargin(true);
		Panel groupAssignmentsP = new Panel(msg.getMessage("RegistrationFormViewer.groupAssignments"), 
				groupAssignments);

		attributeClassAssignments = new ListOfElements<>(msg, new ListOfElements.LabelConverter<AttributeClassAssignment>()
		{
			@Override
			public Label toLabel(AttributeClassAssignment value)
			{
				return new Label(value.getAcName() + " @ " + value.getGroup());
			}
		});
		attributeClassAssignments.setMargin(true);
		Panel attributeClassAssignmentsP = new Panel(msg.getMessage("RegistrationFormViewer.attributeClassAssignments"),
				attributeClassAssignments);
		
		main.addComponents(credentialRequirementAssignment, initialState);
		wrapper.addComponents(attributeAssignmentsP, groupAssignmentsP,	attributeClassAssignmentsP);
	}
	
	private void initMainTab()
	{
		FormLayout main = new FormLayout();
		VerticalLayout wrapper = new VerticalLayout(main);
		wrapper.setMargin(true);
		tabs.addTab(wrapper, msg.getMessage("RegistrationFormViewer.mainTab"));
		
		name = new Label();
		name.setCaption(msg.getMessage("RegistrationFormViewer.name"));
		
		description = new DescriptionTextArea(msg.getMessage("RegistrationFormViewer.description"), true, "");
		
		publiclyAvailable = new Label();
		publiclyAvailable.setCaption(msg.getMessage("RegistrationFormViewer.publiclyAvailable"));
		
		channel = new Label();
		channel.setCaption(msg.getMessage("RegistrationFormViewer.channel"));
		
		adminsNotificationGroup = new Label();
		adminsNotificationGroup.setCaption(msg.getMessage("RegistrationFormViewer.adminsNotificationsGroup"));
		
		submittedTemplate = new TemplateViewer(msg.getMessage("RegistrationFormViewer.submittedTemplate"),
				templatesStore, msg);
		updatedTemplate = new TemplateViewer(msg.getMessage("RegistrationFormViewer.updatedTemplate"),
				templatesStore, msg);
		rejectedTemplate = new TemplateViewer(msg.getMessage("RegistrationFormViewer.rejectedTemplate"),
				templatesStore, msg);
		acceptedTemplate = new TemplateViewer(msg.getMessage("RegistrationFormViewer.acceptedTemplate"),
				templatesStore, msg);
		
		main.addComponents(name, description, publiclyAvailable, channel, adminsNotificationGroup,
				submittedTemplate, updatedTemplate, rejectedTemplate, acceptedTemplate);
	}
	
	private String toHTMLLabel(OptionalRegistrationParam value)
	{
		String settings = msg.getMessage("ParameterRetrievalSettings."+value.getRetrievalSettings());
		return value.getLabel() + " " + getOptionalStr(value.isOptional()) + " [" + settings + "] [" + 
				value.getDescription() + "]";
	}

	private String toHTMLLabel(RegistrationParam value)
	{
		String settings = msg.getMessage("ParameterRetrievalSettings."+value.getRetrievalSettings());
		return value.getLabel() + " [" + settings + "] [" + value.getDescription() + "]";
	}
	
	private String getOptionalStr(boolean value)
	{
		return "[" + (value ? msg.getMessage("RegistrationFormViewer.optional") : 
			msg.getMessage("RegistrationFormViewer.mandatory")) + "]";
	}
}
