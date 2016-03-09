/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.formman;

import pl.edu.icm.unity.server.api.MessageTemplateManagement;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.registration.RegistrationFormNotifications;
import pl.edu.icm.unity.webadmin.msgtemplate.SimpleMessageTemplateViewer;

/**
 * Viewer of {@link RegistrationFormNotifications}
 * @author K. Benedyczak
 */
public class RegistrationFormNotificationsViewer extends BaseFormNotificationsViewer
{
	private SimpleMessageTemplateViewer updatedTemplate;
	private SimpleMessageTemplateViewer invitationTemplate;
	
	public RegistrationFormNotificationsViewer(UnityMessageSource msg,
			MessageTemplateManagement msgTempMan)
	{
		super(msg, msgTempMan);
		initMyUI();
	}

	private void initMyUI()
	{
		updatedTemplate = new SimpleMessageTemplateViewer(msg.getMessage("RegistrationFormViewer.updatedTemplate"),
				msg, msgTempMan);
		invitationTemplate = new SimpleMessageTemplateViewer(msg.getMessage("RegistrationFormViewer.invitationTemplate"),
				msg, msgTempMan);
		addComponents(updatedTemplate, invitationTemplate);
	}
	
	public void clear()
	{
		super.clear();
		updatedTemplate.clearContent();
		invitationTemplate.clearContent();
	}
	
	public void setValue(RegistrationFormNotifications notCfg)
	{
		super.setValue(notCfg);
		updatedTemplate.setInput(notCfg.getUpdatedTemplate());
		invitationTemplate.setInput(notCfg.getInvitationTemplate());
	}
}
