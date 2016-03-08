/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.formman;

import pl.edu.icm.unity.server.api.MessageTemplateManagement;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.registration.BaseFormNotifications;
import pl.edu.icm.unity.webadmin.msgtemplate.SimpleMessageTemplateViewer;
import pl.edu.icm.unity.webui.common.LayoutEmbeddable;

import com.vaadin.ui.Label;

/**
 * Components collection showing {@link BaseFormNotifications} in read only mode.
 * 
 * @author K. Benedyczak
 */
public class BaseFormNotificationsViewer extends LayoutEmbeddable
{
	protected UnityMessageSource msg;
	protected MessageTemplateManagement msgTempMan;
	
	private SimpleMessageTemplateViewer submittedTemplate;
	private SimpleMessageTemplateViewer rejectedTemplate;
	private SimpleMessageTemplateViewer acceptedTemplate;
	private Label channel;
	private Label adminsNotificationGroup;
	
	public BaseFormNotificationsViewer(UnityMessageSource msg,
			MessageTemplateManagement msgTempMan)
	{
		this.msg = msg;
		this.msgTempMan = msgTempMan;
	}

	protected void initUI()
	{
		channel = new Label();
		channel.setCaption(msg.getMessage("RegistrationFormViewer.channel"));
		
		adminsNotificationGroup = new Label();
		adminsNotificationGroup.setCaption(msg.getMessage("RegistrationFormViewer.adminsNotificationsGroup"));
		
		submittedTemplate = new SimpleMessageTemplateViewer(msg.getMessage("RegistrationFormViewer.submittedTemplate"),
				msg, msgTempMan);
		rejectedTemplate = new SimpleMessageTemplateViewer(msg.getMessage("RegistrationFormViewer.rejectedTemplate"),
				msg, msgTempMan);
		acceptedTemplate = new SimpleMessageTemplateViewer(msg.getMessage("RegistrationFormViewer.acceptedTemplate"),
				msg, msgTempMan);
		addComponents(channel, adminsNotificationGroup,	submittedTemplate, rejectedTemplate, acceptedTemplate);
	}
	
	protected void setValue(BaseFormNotifications notCfg)
	{
		submittedTemplate.setInput(notCfg.getSubmittedTemplate());
		rejectedTemplate.setInput(notCfg.getRejectedTemplate());
		acceptedTemplate.setInput(notCfg.getAcceptedTemplate());
		channel.setValue(notCfg.getChannel());
		adminsNotificationGroup.setValue(notCfg.getAdminsNotificationGroup());
	}
}
