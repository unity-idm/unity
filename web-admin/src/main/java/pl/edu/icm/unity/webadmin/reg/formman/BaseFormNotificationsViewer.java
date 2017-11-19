/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.formman;

import com.vaadin.ui.Label;

import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.registration.BaseFormNotifications;
import pl.edu.icm.unity.webadmin.msgtemplate.SimpleMessageTemplateViewer;
import pl.edu.icm.unity.webui.common.LayoutEmbeddable;

/**
 * Components collection showing {@link BaseFormNotifications} in read only mode.
 * 
 * @author K. Benedyczak
 */
public class BaseFormNotificationsViewer extends LayoutEmbeddable
{
	protected UnityMessageSource msg;
	protected MessageTemplateManagement msgTempMan;
	
	private SimpleMessageTemplateViewer updatedTemplate;
	private SimpleMessageTemplateViewer rejectedTemplate;
	private SimpleMessageTemplateViewer acceptedTemplate;
	private Label channel;
	private Label sendAdminCopy;
	private Label adminsNotificationGroup;
	
	public BaseFormNotificationsViewer(UnityMessageSource msg,
			MessageTemplateManagement msgTempMan)
	{
		this.msg = msg;
		this.msgTempMan = msgTempMan;
		initUI();
	}

	private void initUI()
	{
		channel = new Label();
		channel.setCaption(msg.getMessage("RegistrationFormViewer.channel"));

		sendAdminCopy = new Label();
		sendAdminCopy.setCaption(msg.getMessage("RegistrationFormViewer.sendAdminCopy"));
		
		adminsNotificationGroup = new Label();
		adminsNotificationGroup.setCaption(msg.getMessage("RegistrationFormViewer.adminsNotificationsGroup"));
		
		rejectedTemplate = new SimpleMessageTemplateViewer(msg.getMessage("RegistrationFormViewer.rejectedTemplate"),
				msg, msgTempMan);
		acceptedTemplate = new SimpleMessageTemplateViewer(msg.getMessage("RegistrationFormViewer.acceptedTemplate"),
				msg, msgTempMan);
		updatedTemplate = new SimpleMessageTemplateViewer(msg.getMessage("RegistrationFormViewer.updatedTemplate"),
				msg, msgTempMan);
		addComponents(channel, sendAdminCopy, adminsNotificationGroup, rejectedTemplate, 
				acceptedTemplate, updatedTemplate);
	}
	
	protected void clear()
	{
		rejectedTemplate.clearContent();
		acceptedTemplate.clearContent();
		updatedTemplate.clearContent();
		channel.setValue("");
		sendAdminCopy.setValue("");
		adminsNotificationGroup.setValue("");
	}
	
	protected void setValue(BaseFormNotifications notCfg)
	{
		rejectedTemplate.setInput(notCfg.getRejectedTemplate());
		acceptedTemplate.setInput(notCfg.getAcceptedTemplate());
		updatedTemplate.setInput(notCfg.getUpdatedTemplate());
		channel.setValue(notCfg.getChannel());
		sendAdminCopy.setValue(msg.getYesNo(notCfg.isSendUserNotificationCopyToAdmin()));
		adminsNotificationGroup.setValue(notCfg.getAdminsNotificationGroup());
	}
}
