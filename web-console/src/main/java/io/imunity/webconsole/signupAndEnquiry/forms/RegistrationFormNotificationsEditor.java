/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.signupAndEnquiry.forms;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.msg_template.reg.SubmitRegistrationTemplateDef;
import pl.edu.icm.unity.base.registration.RegistrationFormNotifications;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.NotificationsManagement;
import pl.edu.icm.unity.webui.common.CompatibleTemplatesComboBox;

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

	private void initMyUI() throws EngineException
	{
		submittedTemplate = new CompatibleTemplatesComboBox(SubmitRegistrationTemplateDef.NAME, msgTempMan);
		submittedTemplate.setCaption(msg.getMessage("RegistrationFormViewer.submittedTemplate"));

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
