/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.formman;

import pl.edu.icm.unity.server.registries.RegistrationActionsRegistry;
import pl.edu.icm.unity.server.translation.form.RegistrationTranslationProfile;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.registration.AgreementRegistrationParam;
import pl.edu.icm.unity.types.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.types.registration.BaseForm;
import pl.edu.icm.unity.types.registration.CredentialRegistrationParam;
import pl.edu.icm.unity.types.registration.GroupRegistrationParam;
import pl.edu.icm.unity.types.registration.IdentityRegistrationParam;
import pl.edu.icm.unity.types.registration.OptionalRegistrationParam;
import pl.edu.icm.unity.types.registration.RegistrationParam;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.ListOfElements;
import pl.edu.icm.unity.webui.common.i18n.I18nLabel;
import pl.edu.icm.unity.webui.common.safehtml.HtmlLabel;
import pl.edu.icm.unity.webui.common.safehtml.SafePanel;

import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.VerticalLayout;

/**
 * Read only UI displaying common parts of a {@link BaseForm}.
 * 
 * @author K. Benedyczak
 */
public class BaseFormViewer extends VerticalLayout
{
	private UnityMessageSource msg;
	
	private TabSheet tabs;
	
	private Label name;
	private Label description;
	
	private I18nLabel displayedName;
	private I18nLabel formInformation;
	private Label collectComments;
	private ListOfElements<AgreementRegistrationParam> agreements;	
	private ListOfElements<IdentityRegistrationParam> identityParams;
	private ListOfElements<AttributeRegistrationParam> attributeParams;
	private ListOfElements<GroupRegistrationParam> groupParams;
	private ListOfElements<CredentialRegistrationParam> credentialParams;	
	
	private RegistrationTranslationProfileViewer translationProfile;
	private RegistrationActionsRegistry registrationActionsRegistry;
	
	public BaseFormViewer(UnityMessageSource msg, RegistrationActionsRegistry registrationActionsRegistry)
	{
		this.msg = msg;
		this.registrationActionsRegistry = registrationActionsRegistry;
		initUI();
	}
	
	protected void setInput(BaseForm form)
	{
		if (form == null)
		{
			tabs.setVisible(false);
			return;
		}
		tabs.setVisible(true);
		
		name.setValue(form.getName());
		description.setValue(form.getDescription());
		
		displayedName.setValue(form.getDisplayedName());
		formInformation.setValue(form.getFormInformation());
		
		collectComments.setValue(msg.getYesNo(form.isCollectComments()));
		
		agreements.clearContents();
		identityParams.clearContents();
		attributeParams.clearContents();
		groupParams.clearContents();
		credentialParams.clearContents();
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
		
		translationProfile.setInput(new RegistrationTranslationProfile(form.getTranslationProfile().getName(), 
				form.getTranslationProfile().getRules(), registrationActionsRegistry));
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
		FormLayout main = new CompactFormLayout();
		VerticalLayout wrapper = new VerticalLayout(main);
		wrapper.setMargin(true);
		wrapper.setSpacing(true);

		tabs.addTab(wrapper, msg.getMessage("RegistrationFormViewer.collectedTab"));
		
		displayedName = new I18nLabel(msg, msg.getMessage("RegistrationFormViewer.displayedName"));
		formInformation = new I18nLabel(msg, msg.getMessage("RegistrationFormViewer.formInformation"));
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
		
		main.addComponents(displayedName, formInformation, collectComments);
		wrapper.addComponents(agreementsP, identityParamsP, attributeParamsP, groupParamsP, credentialParamsP);
	}
	
	private void initAssignedTab()
	{
		FormLayout main = new CompactFormLayout();
		VerticalLayout wrapper = new VerticalLayout(main);
		wrapper.setMargin(true);
		wrapper.setSpacing(true);
		tabs.addTab(wrapper, msg.getMessage("RegistrationFormViewer.assignedTab"));
		
		translationProfile = new RegistrationTranslationProfileViewer(msg, registrationActionsRegistry);
		
		wrapper.addComponent(translationProfile);
	}
	
	private void initMainTab()
	{
		FormLayout main = new CompactFormLayout();
		VerticalLayout wrapper = new VerticalLayout(main);
		wrapper.setMargin(true);
		tabs.addTab(wrapper, msg.getMessage("RegistrationFormViewer.mainTab"));
		
		name = new Label();
		name.setCaption(msg.getMessage("RegistrationFormViewer.name"));
		
		description = new Label();
		description.setCaption(msg.getMessage("RegistrationFormViewer.description"));
		
		main.addComponents(name, description);
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
