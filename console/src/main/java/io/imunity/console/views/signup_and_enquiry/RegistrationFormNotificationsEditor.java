/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.signup_and_enquiry;

import io.imunity.vaadin.endpoint.common.message_templates.CompatibleTemplatesComboBox;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.msg_template.reg.SubmitRegistrationTemplateDef;
import pl.edu.icm.unity.base.registration.RegistrationFormNotifications;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.NotificationsManagement;

import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_MEDIUM;

/**
 * Editor of {@link RegistrationFormNotifications}
 * @author K. Benedyczak
 */
public class RegistrationFormNotificationsEditor extends BaseFormNotificationsEditor
{
	private CompatibleTemplatesComboBox submittedTemplate;
	
	public RegistrationFormNotificationsEditor(MessageSource msg,
			GroupsManagement groupsMan, NotificationsManagement notificationsMan,
			MessageTemplateManagement msgTempMan) throws EngineException
	{
		super(msg, groupsMan, notificationsMan, msgTempMan);
		initMyUI();
	}

	private void initMyUI()
	{
		submittedTemplate = new CompatibleTemplatesComboBox(SubmitRegistrationTemplateDef.NAME, msgTempMan);
		submittedTemplate.setLabel(msg.getMessage("RegistrationFormViewer.submittedTemplate"));
		submittedTemplate.setWidth(TEXT_FIELD_MEDIUM.value());

		addComponents(submittedTemplate);
	}
	
	public void setValue(RegistrationFormNotifications toEdit)
	{
		super.setValue(toEdit);
		submittedTemplate.setValue(toEdit.getSubmittedTemplate());
	}
	
	public RegistrationFormNotifications getValue()
	{
		RegistrationFormNotifications notCfg = new RegistrationFormNotifications();
		super.fill(notCfg);
		notCfg.setSubmittedTemplate(submittedTemplate.getValue());
		return notCfg;
	}
}
