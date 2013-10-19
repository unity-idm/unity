/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.registrations;

import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.registration.AgreementRegistrationParam;
import pl.edu.icm.unity.types.registration.AttributeClassAssignment;
import pl.edu.icm.unity.types.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.types.registration.CredentialRegistrationParam;
import pl.edu.icm.unity.types.registration.GroupRegistrationParam;
import pl.edu.icm.unity.types.registration.IdentityRegistrationParam;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationFormNotifications;
import pl.edu.icm.unity.types.registration.RegistrationParam;
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
	
	private TabSheet tabs;
	
	private Label name;
	private DescriptionTextArea description;
	private Label publiclyAvailable;
	private TextArea submittedTemplate;
	private TextArea updatedTemplate;
	private TextArea rejectedTemplate;
	private TextArea acceptedTemplate;
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
	
	public RegistrationFormViewer(UnityMessageSource msg, AttributeHandlerRegistry attrHandlerRegistry)
	{
		this.msg = msg;
		this.attrHandlerRegistry = attrHandlerRegistry;
		initUI();
	}
	
	public void setInput(RegistrationForm form)
	{
		setEmpty();
		if (form == null)
			return;
		
		name.setValue(form.getName());
		description.setValue(form.getDescription());
		publiclyAvailable.setValue(msg.getYesNo(form.isPubliclyAvailable()));
		
		RegistrationFormNotifications notCfg = form.getNotificationsConfiguration();
		if (notCfg != null)
		{
			submittedTemplate.setValue(notCfg.getSubmittedTemplate());
			updatedTemplate.setValue(notCfg.getUpdatedTemplate());
			rejectedTemplate.setValue(notCfg.getRejectedTemplate());
			acceptedTemplate.setValue(notCfg.getAcceptedTemplate());
			channel.setValue(notCfg.getChannel());
			adminsNotificationGroup.setValue(notCfg.getAdminsNotificationGroup());
		}
		
		formInformation.setValue(form.getFormInformation());
		registrationCode.setValue(form.getRegistrationCode() == null ? 
				msg.getMessage("no") : form.getRegistrationCode());
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
		
		submittedTemplate.setValue("");
		updatedTemplate.setValue("");
		rejectedTemplate.setValue("");
		acceptedTemplate.setValue("");
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

		tabs.addTab(wrapper, msg.getMessage("RegistrationFormViewer.collectedTab"));
		
		formInformation = new Label();
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
		agreements.setCaption(msg.getMessage("RegistrationFormViewer.agreements"));

		identityParams = new ListOfElements<>(msg, new ListOfElements.LabelConverter<IdentityRegistrationParam>()
		{
			@Override
			public Label toLabel(IdentityRegistrationParam value)
			{
				String content = toHTMLLabel(value) + "<br>" + 
						msg.getMessage("RegistrationFormViewer.identityType", value.getIdentityType());
				return new Label(content, ContentMode.HTML);
			}
		});
		identityParams.setCaption(msg.getMessage("RegistrationFormViewer.identityParams"));
		
		attributeParams = new ListOfElements<>(msg, new ListOfElements.LabelConverter<AttributeRegistrationParam>()
		{
			@Override
			public Label toLabel(AttributeRegistrationParam value)
			{
				String useDescription = value.isUseDescription() ? 
						msg.getMessage("RegistrationFormViewer.useAttributeTypeDescription") : ""; 
				String showGroup = value.isShowGroups() ? 
						msg.getMessage("RegistrationFormViewer.showAttributeGroup") : ""; 
				String content = toHTMLLabel(value) + "<br>" + 
						value.getAttributeType() + " @ " + value.getGroup() + " " +
						useDescription + " " + showGroup;
				return new Label(content, ContentMode.HTML);
			}
		});
		attributeParams.setCaption(msg.getMessage("RegistrationFormViewer.attributeParams"));
		
		groupParams = new ListOfElements<>(msg, new ListOfElements.LabelConverter<GroupRegistrationParam>()
		{
			@Override
			public Label toLabel(GroupRegistrationParam value)
			{
				String content = toHTMLLabel(value) + "<br>" + value.getGroupPath();
				return new Label(content, ContentMode.HTML);
			}
		});
		groupParams.setCaption(msg.getMessage("RegistrationFormViewer.groupParams"));
		
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
		credentialParams.setCaption(msg.getMessage("RegistrationFormViewer.credentialParams"));
		
		main.addComponents(formInformation, registrationCode, collectComments, agreements, identityParams, 
				attributeParams, groupParams, credentialParams);
	}
	
	private void initAssignedTab()
	{
		FormLayout main = new FormLayout();
		VerticalLayout wrapper = new VerticalLayout(main);
		wrapper.setMargin(true);
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
						value, 64);
				return new Label(content, ContentMode.HTML);
			}
		});
		attributeAssignments.setCaption(msg.getMessage("RegistrationFormViewer.attributeAssignments"));

		groupAssignments = new ListOfElements<>(msg, new ListOfElements.LabelConverter<String>()
		{
			@Override
			public Label toLabel(String value)
			{
				return new Label(value);
			}
		});
		groupAssignments.setCaption(msg.getMessage("RegistrationFormViewer.groupAssignments"));

		attributeClassAssignments = new ListOfElements<>(msg, new ListOfElements.LabelConverter<AttributeClassAssignment>()
		{
			@Override
			public Label toLabel(AttributeClassAssignment value)
			{
				return new Label(value.getAcName() + " @ " + value.getGroup());
			}
		});
		attributeClassAssignments.setCaption(msg.getMessage("RegistrationFormViewer.attributeClassAssignments"));
		
		main.addComponents(credentialRequirementAssignment, initialState, attributeAssignments, groupAssignments, 
				attributeClassAssignments);
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
		
		submittedTemplate = new TextArea(msg.getMessage("RegistrationFormViewer.submittedTemplate"));
		updatedTemplate = new TextArea(msg.getMessage("RegistrationFormViewer.updatedTemplate"));
		rejectedTemplate = new TextArea(msg.getMessage("RegistrationFormViewer.rejectedTemplate"));
		acceptedTemplate = new TextArea(msg.getMessage("RegistrationFormViewer.acceptedTemplate"));
		
		main.addComponents(name, description, publiclyAvailable, channel, adminsNotificationGroup,
				submittedTemplate, updatedTemplate, rejectedTemplate, acceptedTemplate);
	}
	
	private String toHTMLLabel(RegistrationParam value)
	{
		String settings = msg.getMessage("ParameterRetrievalSettings."+value.getRetrievalSettings());
		return value.getLabel() + " " + getOptionalStr(value.isOptional()) + " " + settings + "<br>" + 
				value.getDescription();
	}
	
	private String getOptionalStr(boolean value)
	{
		return value ? msg.getMessage("RegistrationFormViewer.optional") : 
			msg.getMessage("RegistrationFormViewer.mandatory");
	}
}
