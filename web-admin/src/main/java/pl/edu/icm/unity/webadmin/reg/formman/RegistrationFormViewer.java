/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.formman;

import pl.edu.icm.unity.server.api.MessageTemplateManagement;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.I18nString;
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
import pl.edu.icm.unity.webadmin.msgtemplate.SimpleMessageTemplateViewer;
import pl.edu.icm.unity.webui.common.DescriptionTextArea;
import pl.edu.icm.unity.webui.common.ListOfElements;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.i18n.I18nLabel;
import pl.edu.icm.unity.webui.common.safehtml.HtmlLabel;
import pl.edu.icm.unity.webui.common.safehtml.SafePanel;

import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.VerticalLayout;

/**
 * Read only UI displaying a {@link RegistrationForm}.
 * 
 * @author K. Benedyczak
 */
public class RegistrationFormViewer extends VerticalLayout
{
	private UnityMessageSource msg;
	private AttributeHandlerRegistry attrHandlerRegistry;
	private MessageTemplateManagement msgTempMan;
	
	private TabSheet tabs;
	
	private Label name;
	private DescriptionTextArea description;
	private Label publiclyAvailable;
	private SimpleMessageTemplateViewer submittedTemplate;
	private SimpleMessageTemplateViewer updatedTemplate;
	private SimpleMessageTemplateViewer rejectedTemplate;
	private SimpleMessageTemplateViewer acceptedTemplate;
	private Label channel;
	private Label adminsNotificationGroup;
	private Label autoAcceptCondition;
	private Label captcha;
	private Label redirectAfterSubmit;
	private Label redirectAfterSubmitAndAccept;
	
	private I18nLabel displayedName;
	private I18nLabel formInformation;
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
			MessageTemplateManagement msgTempMan)
	{
		this.msg = msg;
		this.attrHandlerRegistry = attrHandlerRegistry;
		this.msgTempMan = msgTempMan;
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
		autoAcceptCondition.setValue(form.getAutoAcceptCondition());
		captcha.setValue(form.getCaptchaLength() > 0 ? 
				msg.getMessage("RegistrationFormViewer.captchaLength", form.getCaptchaLength()) : 
				msg.getMessage("no"));
		redirectAfterSubmit.setValue(form.getRedirectAfterSubmit() == null ? 
				"-" : form.getRedirectAfterSubmit());
		redirectAfterSubmitAndAccept.setValue(form.getRedirectAfterSubmitAndAccept() == null ? 
				"-" : form.getRedirectAfterSubmitAndAccept());
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
		
		displayedName.setValue(form.getDisplayedName());
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
		autoAcceptCondition.setValue("");
		captcha.setValue("");
		redirectAfterSubmit.setValue("");
		redirectAfterSubmitAndAccept.setValue("");
		
		submittedTemplate.setInput(null);
		updatedTemplate.setInput(null);
		rejectedTemplate.setInput(null);
		acceptedTemplate.setInput(null);
		channel.setValue("");
		adminsNotificationGroup.setValue("");
		
		displayedName.setValue(new I18nString());
		formInformation.setValue(new I18nString());
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
		
		displayedName = new I18nLabel(msg, msg.getMessage("RegistrationFormViewer.displayedName"));
		formInformation = new I18nLabel(msg, msg.getMessage("RegistrationFormViewer.formInformation"));
		registrationCode = new Label();
		registrationCode.setCaption(msg.getMessage("RegistrationFormViewer.registrationCode"));
		collectComments = new Label();
		collectComments.setCaption(msg.getMessage("RegistrationFormViewer.collectComments"));

		agreements = new ListOfElements<>(msg, new ListOfElements.LabelConverter<AgreementRegistrationParam>()
		{
			@Override
			public VerticalLayout toLabel(AgreementRegistrationParam value)
			{
				Label mandatory = new Label(getOptionalStr(!value.isManatory()));
				I18nLabel main = new I18nLabel(msg);
				main.setValue(value.getText());
				
				return new VerticalLayout(mandatory, main);
			}
		});
		agreements.setMargin(true);
		Panel agreementsP = new SafePanel(msg.getMessage("RegistrationFormViewer.agreements"), agreements);

		identityParams = new ListOfElements<>(msg, new ListOfElements.LabelConverter<IdentityRegistrationParam>()
		{
			@Override
			public Label toLabel(IdentityRegistrationParam value)
			{
				String content = msg.getMessage("RegistrationFormViewer.identityType", 
						value.getIdentityType());
				HtmlLabel ret = new HtmlLabel(msg);
				ret.setHtmlValue("RegistrationFormViewer.twoLines", content, toHTMLLabel(value));
				return ret;
			}
		});
		identityParams.setMargin(true);
		Panel identityParamsP = new SafePanel(msg.getMessage("RegistrationFormViewer.identityParams"), identityParams);
		
		attributeParams = new ListOfElements<>(msg, new ListOfElements.LabelConverter<AttributeRegistrationParam>()
		{
			@Override
			public Label toLabel(AttributeRegistrationParam value)
			{
				String showGroup = value.isShowGroups() ? 
						"[" + msg.getMessage("RegistrationFormViewer.showAttributeGroup")+"]" : ""; 
				HtmlLabel ret = new HtmlLabel(msg);
				String line1 = value.getAttributeType() + " @ " + value.getGroup() + " " +
						showGroup;
				ret.setHtmlValue("RegistrationFormViewer.twoLines", line1, toHTMLLabel(value));
				return ret;
			}
		});
		attributeParams.setMargin(true);
		Panel attributeParamsP = new SafePanel(msg.getMessage("RegistrationFormViewer.attributeParams"), 
				attributeParams);
		
		
		groupParams = new ListOfElements<>(msg, new ListOfElements.LabelConverter<GroupRegistrationParam>()
		{
			@Override
			public Label toLabel(GroupRegistrationParam value)
			{
				HtmlLabel ret = new HtmlLabel(msg);
				ret.setHtmlValue("RegistrationFormViewer.twoLines", value.getGroupPath(), 
						toHTMLLabel(value));
				return ret;
			}
		});
		groupParams.setMargin(true);
		Panel groupParamsP = new SafePanel(msg.getMessage("RegistrationFormViewer.groupParams"), groupParams);
		
		credentialParams = new ListOfElements<>(msg, new ListOfElements.LabelConverter<CredentialRegistrationParam>()
		{
			@Override
			public Label toLabel(CredentialRegistrationParam value)
			{
				HtmlLabel ret = new HtmlLabel(msg);
				ret.setHtmlValue("RegistrationFormViewer.twoLines", value.getCredentialName(), 
						"[" + emptyNull(value.getLabel()) +  "] ["+
								emptyNull(value.getDescription()) + "]");
				return ret;
			}
		});
		credentialParams.setMargin(true);
		Panel credentialParamsP = new SafePanel(msg.getMessage("RegistrationFormViewer.credentialParams"), 
				credentialParams);
		
		main.addComponents(displayedName, formInformation, registrationCode, collectComments);
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
				return new Label(content);
			}
		});
		attributeAssignments.setMargin(true);
		Panel attributeAssignmentsP = new SafePanel(msg.getMessage("RegistrationFormViewer.attributeAssignments"),
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
		Panel groupAssignmentsP = new SafePanel(msg.getMessage("RegistrationFormViewer.groupAssignments"), 
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
		Panel attributeClassAssignmentsP = new SafePanel(msg.getMessage("RegistrationFormViewer.attributeClassAssignments"),
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
		
		autoAcceptCondition = new Label();
		autoAcceptCondition.setCaption(msg.getMessage("RegistrationFormViewer.autoAcceptCondition"));
		
		captcha = new Label();
		captcha.setCaption(msg.getMessage("RegistrationFormViewer.captcha"));
		
		redirectAfterSubmit = new Label();
		redirectAfterSubmit.setCaption(msg.getMessage("RegistrationFormViewer.redirectAfterSubmit"));

		redirectAfterSubmitAndAccept = new Label();
		redirectAfterSubmitAndAccept.setCaption(msg.getMessage("RegistrationFormViewer.redirectAfterSubmitAndAccept"));
		
		publiclyAvailable = new Label();
		publiclyAvailable.setCaption(msg.getMessage("RegistrationFormViewer.publiclyAvailable"));
		
		channel = new Label();
		channel.setCaption(msg.getMessage("RegistrationFormViewer.channel"));
		
		adminsNotificationGroup = new Label();
		adminsNotificationGroup.setCaption(msg.getMessage("RegistrationFormViewer.adminsNotificationsGroup"));
		
		submittedTemplate = new SimpleMessageTemplateViewer(msg.getMessage("RegistrationFormViewer.submittedTemplate"),
				msg, msgTempMan);
		updatedTemplate = new SimpleMessageTemplateViewer(msg.getMessage("RegistrationFormViewer.updatedTemplate"),
				msg, msgTempMan);
		rejectedTemplate = new SimpleMessageTemplateViewer(msg.getMessage("RegistrationFormViewer.rejectedTemplate"),
				msg, msgTempMan);
		acceptedTemplate = new SimpleMessageTemplateViewer(msg.getMessage("RegistrationFormViewer.acceptedTemplate"),
				msg, msgTempMan);
		
		main.addComponents(name, description, publiclyAvailable, channel, adminsNotificationGroup,
				submittedTemplate, updatedTemplate, rejectedTemplate, acceptedTemplate, captcha,
				redirectAfterSubmit, redirectAfterSubmitAndAccept, autoAcceptCondition);
	}
	
	private String toHTMLLabel(OptionalRegistrationParam value)
	{
		String settings = msg.getMessage("ParameterRetrievalSettings."+value.getRetrievalSettings());
		return emptyNull(value.getLabel()) + " " + getOptionalStr(value.isOptional()) + " [" + settings + "] [" + 
				emptyNull(value.getDescription()) + "]";
	}

	private String toHTMLLabel(RegistrationParam value)
	{
		String settings = msg.getMessage("ParameterRetrievalSettings."+value.getRetrievalSettings());
		return emptyNull(value.getLabel()) + " [" + settings + "] [" + emptyNull(value.getDescription()) + "]";
	}
	
	private String getOptionalStr(boolean value)
	{
		return "[" + (value ? msg.getMessage("RegistrationFormViewer.optional") : 
			msg.getMessage("RegistrationFormViewer.mandatory")) + "]";
	}
	
	private String emptyNull(String a)
	{
		return a == null ? "" : a;
	}
}
