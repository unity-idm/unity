/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.formman;

import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.endpoint.SharedEndpointManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.registration.PublicRegistrationURLSupport;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.engine.translation.form.RegistrationActionsRegistry;
import pl.edu.icm.unity.types.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.types.registration.ExternalSignupSpec;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationFormNotifications;
import pl.edu.icm.unity.types.translation.ProfileType;
import pl.edu.icm.unity.types.translation.TranslationProfile;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.safehtml.SafePanel;

/**
 * Read only UI displaying a {@link RegistrationForm}.
 * 
 * @author K. Benedyczak
 */
@PrototypeComponent
public class RegistrationFormViewer extends BaseFormViewer
{
	private UnityMessageSource msg;
	private MessageTemplateManagement msgTempMan;
	
	private TabSheet tabs;
	
	private Label publiclyAvailable;
	private Label byInvitationOnly;
	private Label publicLink;
	private RegistrationFormNotificationsViewer notViewer;
	private Label captcha;
	private Label registrationCode;
	private Label credentialRequirementAssignment;
	private RegistrationTranslationProfileViewer translationProfile;
	private SharedEndpointManagement sharedEndpointMan;
	private RegistrationActionsRegistry registrationActionsRegistry;
	private Label remoteAuthnSelections;
	private Label userExistsRedirectUrl;
	private SafePanel remoteSignupMethodsP;
	
	@Autowired
	public RegistrationFormViewer(UnityMessageSource msg, RegistrationActionsRegistry registrationActionsRegistry,
			MessageTemplateManagement msgTempMan, SharedEndpointManagement sharedEndpointMan)
	{
		super(msg);
		this.msg = msg;
		this.registrationActionsRegistry = registrationActionsRegistry;
		this.msgTempMan = msgTempMan;
		this.sharedEndpointMan = sharedEndpointMan;
		initUI();
	}
	
	public void setInput(RegistrationForm form)
	{
		super.setInput(form);

		if (form == null)
		{
			tabs.setVisible(false);
			return;
		}
		tabs.setVisible(true);
		
		captcha.setValue(form.getCaptchaLength() > 0 ? 
				msg.getMessage("RegistrationFormViewer.captchaLength", form.getCaptchaLength()) : 
				msg.getMessage("no"));
		publiclyAvailable.setValue(msg.getYesNo(form.isPubliclyAvailable()));
		byInvitationOnly.setValue(msg.getYesNo(form.isByInvitationOnly()));
		
		publicLink.setValue(form.isPubliclyAvailable() ? 
				PublicRegistrationURLSupport.getPublicRegistrationLink(form, sharedEndpointMan) : "-");
		
		RegistrationFormNotifications notCfg = form.getNotificationsConfiguration();
		if (notCfg != null)
			notViewer.setValue(notCfg);
		else
			notViewer.clear();
		
		String code = form.getRegistrationCode() == null ? "-" : form.getRegistrationCode();
		registrationCode.setValue(code);
		
		credentialRequirementAssignment.setValue(form.getDefaultCredentialRequirement());
		TranslationProfile tProfile = new TranslationProfile(form.getTranslationProfile().getName(),
				"",
				ProfileType.REGISTRATION,
				form.getTranslationProfile().getRules());
		translationProfile.setInput(tProfile, registrationActionsRegistry);
		
		setRemoteSignupMethods(form.getExternalSignupSpec());
	}
	
	private void setRemoteSignupMethods(ExternalSignupSpec externalSignupSpec)
	{
		String redirectUrl = externalSignupSpec.getUserExistsRedirectUrl();
		if (StringUtils.isEmpty(redirectUrl))
		{
			userExistsRedirectUrl.setValue(msg.getMessage("MessageTemplateViewer.notSet"));
		} else
		{
			userExistsRedirectUrl.setValue(redirectUrl);
		}
		
		if (externalSignupSpec.isEnabled())
		{
			String selected = externalSignupSpec.getSpecs().stream()
					.map(AuthenticationOptionKey::toGlobalKey)
					.collect(Collectors.joining(", "));
			remoteAuthnSelections.setValue(selected);
		} else
		{
			remoteAuthnSelections.setValue(msg.getMessage("MessageTemplateViewer.notSet"));
		}
		
		remoteSignupMethodsP.setVisible(externalSignupSpec.isEnabled());
	}

	private void initUI()
	{
		tabs = new TabSheet();
		initMainTab();
		initCollectedTab();
		initAssignedTab();
		initLayoutTab();
		addComponent(tabs);
	}
	
	private void initCollectedTab()
	{
		FormLayout main = new CompactFormLayout();
		VerticalLayout wrapper = new VerticalLayout(main);
		wrapper.setMargin(true);
		wrapper.setSpacing(true);
		
		tabs.addTab(wrapper, msg.getMessage("RegistrationFormViewer.collectedTab"));

		setupCommonFormInformationComponents();
		registrationCode = new Label();
		registrationCode.setCaption(msg.getMessage("RegistrationFormViewer.registrationCode"));
		
		main.addComponents(displayedName, formInformation, registrationCode, collectComments);
		main.addComponent(getRemoteSignupMethodsInformation());
		main.addComponent(getCollectedDataInformation());
	}
	
	private Component getRemoteSignupMethodsInformation()
	{
		remoteAuthnSelections = new Label();
		remoteAuthnSelections.setCaption(msg.getMessage("RegistrationFormEditor.remoteAuthenOptions"));
		userExistsRedirectUrl = new Label();
		userExistsRedirectUrl.setCaption(msg.getMessage("RegistrationFormViewer.userExistsRedirectUrl"));
		
		FormLayout main = new FormLayout();
		main.addComponents(remoteAuthnSelections, userExistsRedirectUrl);
		main.setMargin(false);
		main.setSpacing(false);
		
		VerticalLayout wrapper = new VerticalLayout(main);
		wrapper.setMargin(true);
		wrapper.setSpacing(false);
		remoteSignupMethodsP = new SafePanel(msg.getMessage("RegistrationFormEditor.remoteSignupMethods"), wrapper);
		return remoteSignupMethodsP;
	}

	private void initAssignedTab()
	{
		FormLayout main = new CompactFormLayout();
		VerticalLayout wrapper = new VerticalLayout(main);
		wrapper.setMargin(true);
		wrapper.setSpacing(true);
		tabs.addTab(wrapper, msg.getMessage("RegistrationFormViewer.assignedTab"));
		
		credentialRequirementAssignment = new Label();
		credentialRequirementAssignment.setCaption(
				msg.getMessage("RegistrationFormViewer.credentialRequirementAssignment"));

		translationProfile = new RegistrationTranslationProfileViewer(msg);
		
		main.addComponents(credentialRequirementAssignment);
		wrapper.addComponent(translationProfile);
	}
	
	private void initMainTab()
	{
		FormLayout main = new CompactFormLayout();
		VerticalLayout wrapper = new VerticalLayout(main);
		wrapper.setMargin(true);
		wrapper.setSpacing(false);
		tabs.addTab(wrapper, msg.getMessage("RegistrationFormViewer.mainTab"));
		
		setupNameAndDesc();
		
		captcha = new Label();
		captcha.setCaption(msg.getMessage("RegistrationFormViewer.captcha"));
		
		publiclyAvailable = new Label();
		publiclyAvailable.setCaption(msg.getMessage("RegistrationFormViewer.publiclyAvailable"));
		
		byInvitationOnly = new Label();
		byInvitationOnly.setCaption(msg.getMessage("RegistrationFormViewer.byInvitationOnly"));
		
		publicLink = new Label();
		publicLink.setCaption(msg.getMessage("RegistrationFormViewer.publicLink"));
		
		notViewer = new RegistrationFormNotificationsViewer(msg, msgTempMan);
		main.addComponents(name, description, publiclyAvailable, byInvitationOnly, 
				publicLink, captcha);
		notViewer.addToLayout(main);
	}
	
	private void initLayoutTab()
	{
		VerticalLayout wrapper = new VerticalLayout();
		wrapper.setMargin(true);
		wrapper.setSpacing(true);
		tabs.addTab(wrapper, msg.getMessage("RegistrationFormViewer.layoutTab"));
		wrapper.addComponent(layout);
	}
	
}
