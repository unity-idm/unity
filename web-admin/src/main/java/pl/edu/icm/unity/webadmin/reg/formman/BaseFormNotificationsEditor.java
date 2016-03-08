/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.formman;

import java.util.List;
import java.util.Set;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.api.MessageTemplateManagement;
import pl.edu.icm.unity.server.api.NotificationsManagement;
import pl.edu.icm.unity.server.api.registration.AcceptRegistrationTemplateDef;
import pl.edu.icm.unity.server.api.registration.RejectRegistrationTemplateDef;
import pl.edu.icm.unity.server.api.registration.SubmitRegistrationTemplateDef;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.registration.BaseFormNotifications;
import pl.edu.icm.unity.webui.common.CompatibleTemplatesComboBox;
import pl.edu.icm.unity.webui.common.GroupComboBox;
import pl.edu.icm.unity.webui.common.LayoutEmbeddable;

import com.vaadin.ui.ComboBox;

/**
 * Code for editing BaseFormNotifications, i.e. a base for registration and enquiry forms notifications config editing.
 * @author K. Benedyczak
 */
public class BaseFormNotificationsEditor extends LayoutEmbeddable
{
	protected final UnityMessageSource msg;
	protected final GroupsManagement groupsMan;
	protected final NotificationsManagement notificationsMan;
	protected final MessageTemplateManagement msgTempMan;
	
	private ComboBox channel;
	private GroupComboBox adminsNotificationGroup;

	private ComboBox submittedTemplate;
	private ComboBox rejectedTemplate;
	private ComboBox acceptedTemplate;

	public BaseFormNotificationsEditor(UnityMessageSource msg, GroupsManagement groupsMan,
			NotificationsManagement notificationsMan, MessageTemplateManagement msgTempMan) throws EngineException
	{
		this.msg = msg;
		this.groupsMan = groupsMan;
		this.notificationsMan = notificationsMan;
		this.msgTempMan = msgTempMan;
		initUI();
	}


	protected void initUI() throws EngineException
	{
		channel = new ComboBox(msg.getMessage("RegistrationFormViewer.channel"));
		Set<String> channels = notificationsMan.getNotificationChannels().keySet();
		for (String c: channels)
			channel.addItem(c);
		
		adminsNotificationGroup = new GroupComboBox(
				msg.getMessage("RegistrationFormViewer.adminsNotificationsGroup"), groupsMan);
		adminsNotificationGroup.setNullSelectionAllowed(true);
		adminsNotificationGroup.setInput("/", true);
		
		
		submittedTemplate = new CompatibleTemplatesComboBox(SubmitRegistrationTemplateDef.NAME, msgTempMan);
		submittedTemplate.setCaption(msg.getMessage("RegistrationFormViewer.submittedTemplate"));
		rejectedTemplate =  new CompatibleTemplatesComboBox(RejectRegistrationTemplateDef.NAME, msgTempMan);
		rejectedTemplate.setCaption(msg.getMessage("RegistrationFormViewer.rejectedTemplate"));
		acceptedTemplate =  new CompatibleTemplatesComboBox(AcceptRegistrationTemplateDef.NAME, msgTempMan);
		acceptedTemplate.setCaption(msg.getMessage("RegistrationFormViewer.acceptedTemplate"));
		
		addComponents(channel, adminsNotificationGroup,
				submittedTemplate, rejectedTemplate, acceptedTemplate);
	}
	
	protected void setValue(BaseFormNotifications toEdit)
	{
		adminsNotificationGroup.setValue(toEdit.getAdminsNotificationGroup());
		channel.setValue(toEdit.getChannel());
		submittedTemplate.setValue(toEdit.getSubmittedTemplate());
		rejectedTemplate.setValue(toEdit.getRejectedTemplate());
		acceptedTemplate.setValue(toEdit.getAcceptedTemplate());
	}
	
	protected void fill(BaseFormNotifications notCfg)
	{
		notCfg.setAcceptedTemplate((String) acceptedTemplate.getValue());
		notCfg.setAdminsNotificationGroup((String) adminsNotificationGroup.getValue());
		notCfg.setChannel((String) channel.getValue());
		notCfg.setRejectedTemplate((String) rejectedTemplate.getValue());
		notCfg.setSubmittedTemplate((String) submittedTemplate.getValue());
	}
	
	public List<String> getGroups()
	{
		return adminsNotificationGroup.getAllGroups();
	}
}
