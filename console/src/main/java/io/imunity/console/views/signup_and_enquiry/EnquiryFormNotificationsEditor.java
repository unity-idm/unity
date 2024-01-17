/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.signup_and_enquiry;

import io.imunity.vaadin.endpoint.common.message_templates.CompatibleTemplatesComboBox;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.msg_template.reg.EnquiryFilledTemplateDef;
import pl.edu.icm.unity.base.msg_template.reg.NewEnquiryTemplateDef;
import pl.edu.icm.unity.base.registration.EnquiryFormNotifications;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.NotificationsManagement;


class EnquiryFormNotificationsEditor extends BaseFormNotificationsEditor
{
	private CompatibleTemplatesComboBox enquiryToFillTemplate;
	private CompatibleTemplatesComboBox enquiryFilledTemplate;
	
	public EnquiryFormNotificationsEditor(MessageSource msg,
			GroupsManagement groupsMan, NotificationsManagement notificationsMan,
			MessageTemplateManagement msgTempMan) throws EngineException
	{
		super(msg, groupsMan, notificationsMan, msgTempMan);
		initMyUI();
	}

	private void initMyUI()
	{
		enquiryFilledTemplate = new CompatibleTemplatesComboBox(EnquiryFilledTemplateDef.NAME, msgTempMan);
		enquiryFilledTemplate.setLabel(msg.getMessage("RegistrationFormViewer.submittedTemplate"));
		enquiryToFillTemplate =  new CompatibleTemplatesComboBox(NewEnquiryTemplateDef.NAME, msgTempMan);
		enquiryToFillTemplate.setLabel(msg.getMessage("EnquiryFormNotificationsViewer.enquiryToFillTemplate"));
		addComponents(enquiryToFillTemplate, enquiryFilledTemplate);
	}
	
	public void setValue(EnquiryFormNotifications toEdit)
	{
		super.setValue(toEdit);
		enquiryFilledTemplate.setValue(toEdit.getSubmittedTemplate());
		enquiryToFillTemplate.setValue(toEdit.getEnquiryToFillTemplate());
	}
	
	public EnquiryFormNotifications getValue()
	{
		EnquiryFormNotifications notCfg = new EnquiryFormNotifications();
		super.fill(notCfg);
		notCfg.setEnquiryToFillTemplate(enquiryToFillTemplate.getValue());
		notCfg.setSubmittedTemplate(enquiryFilledTemplate.getValue());
		return notCfg;
	}
}
