/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.formman;

import java.util.Arrays;
import java.util.stream.Collectors;

import pl.edu.icm.unity.server.api.MessageTemplateManagement;
import pl.edu.icm.unity.server.registries.RegistrationActionsRegistry;
import pl.edu.icm.unity.server.translation.form.RegistrationTranslationProfile;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryFormNotifications;
import pl.edu.icm.unity.webui.common.CompactFormLayout;

import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.VerticalLayout;

/**
 * Read only UI displaying a {@link EnquiryForm}.
 * 
 * @author K. Benedyczak
 */
public class EnquiryFormViewer extends BaseFormViewer
{
	private UnityMessageSource msg;
	private MessageTemplateManagement msgTempMan;
	
	private TabSheet tabs;
	
	private Label type;
	private Label targetGroups;
	
	private EnquiryFormNotificationsViewer notViewer;
	private RegistrationTranslationProfileViewer translationProfile;
	private RegistrationActionsRegistry registrationActionsRegistry;
	
	public EnquiryFormViewer(UnityMessageSource msg, RegistrationActionsRegistry registrationActionsRegistry,
			MessageTemplateManagement msgTempMan)
	{
		super(msg);
		this.msg = msg;
		this.registrationActionsRegistry = registrationActionsRegistry;
		this.msgTempMan = msgTempMan;
		initUI();
	}
	
	public void setInput(EnquiryForm form)
	{
		super.setInput(form);

		if (form == null)
		{
			tabs.setVisible(false);
			return;
		}
		tabs.setVisible(true);
		
		type.setValue(msg.getMessage("EnquiryType." + form.getType().name()));
		targetGroups.setValue(Arrays.stream(form.getTargetGroups()).
				collect(Collectors.joining(", ")));
		
		EnquiryFormNotifications notCfg = form.getNotificationsConfiguration();
		if (notCfg != null)
			notViewer.setValue(notCfg);
		else
			notViewer.clear();
		
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

		setupCommonFormInformationComponents();
		
		main.addComponents(displayedName, formInformation, collectComments);
		main.addComponent(getCollectedDataInformation());
	}
	
	private void initAssignedTab()
	{
		VerticalLayout wrapper = new VerticalLayout();
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
		
		setupNameAndDesc();
		
		type = new Label();
		type.setCaption(msg.getMessage("EnquiryFormViewer.type"));
		
		targetGroups = new Label();
		targetGroups.setCaption(msg.getMessage("EnquiryFormViewer.targetGroups"));
		
		notViewer = new EnquiryFormNotificationsViewer(msg, msgTempMan);
		main.addComponents(name, description, type, targetGroups);
		notViewer.addToLayout(main);
	}
}
