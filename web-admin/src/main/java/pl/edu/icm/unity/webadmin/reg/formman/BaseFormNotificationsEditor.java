/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.formman;

import java.util.List;
import java.util.Set;

import com.vaadin.ui.ComboBox;

import pl.edu.icm.unity.base.msgtemplates.reg.AcceptRegistrationTemplateDef;
import pl.edu.icm.unity.base.msgtemplates.reg.RejectRegistrationTemplateDef;
import pl.edu.icm.unity.base.msgtemplates.reg.UpdateRegistrationTemplateDef;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.NotificationsManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.registration.BaseFormNotifications;
import pl.edu.icm.unity.webui.common.CompatibleTemplatesComboBox2;
import pl.edu.icm.unity.webui.common.GroupComboBox2;
import pl.edu.icm.unity.webui.common.LayoutEmbeddable;

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
	
	private ComboBox<String> channel;
	private GroupComboBox2 adminsNotificationGroup;

	private CompatibleTemplatesComboBox2 rejectedTemplate;
	private CompatibleTemplatesComboBox2 acceptedTemplate;
	private CompatibleTemplatesComboBox2 updatedTemplate;
	
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
		channel = new ComboBox<>(msg.getMessage("RegistrationFormViewer.channel"));
		Set<String> channels = notificationsMan.getNotificationChannels().keySet();
		channel.setItems(channels);
		
		adminsNotificationGroup = new GroupComboBox2(
				msg.getMessage("RegistrationFormViewer.adminsNotificationsGroup"), groupsMan);
		adminsNotificationGroup.setEmptySelectionAllowed(true);
		adminsNotificationGroup.setInput("/", true);
		
		rejectedTemplate =  new CompatibleTemplatesComboBox2(RejectRegistrationTemplateDef.NAME, msgTempMan);
		rejectedTemplate.setCaption(msg.getMessage("RegistrationFormViewer.rejectedTemplate"));
		acceptedTemplate =  new CompatibleTemplatesComboBox2(AcceptRegistrationTemplateDef.NAME, msgTempMan);
		acceptedTemplate.setCaption(msg.getMessage("RegistrationFormViewer.acceptedTemplate"));
		updatedTemplate =  new CompatibleTemplatesComboBox2(UpdateRegistrationTemplateDef.NAME, msgTempMan);
		updatedTemplate.setCaption(msg.getMessage("RegistrationFormViewer.updatedTemplate"));
		
		addComponents(channel, adminsNotificationGroup,
				rejectedTemplate, acceptedTemplate, updatedTemplate);
	}
	
	protected void setValue(BaseFormNotifications toEdit)
	{
		adminsNotificationGroup.setValue(toEdit.getAdminsNotificationGroup());
		channel.setValue(toEdit.getChannel());
		rejectedTemplate.setValue(toEdit.getRejectedTemplate());
		acceptedTemplate.setValue(toEdit.getAcceptedTemplate());
		updatedTemplate.setValue(toEdit.getUpdatedTemplate());
	}
	
	protected void fill(BaseFormNotifications notCfg)
	{
		notCfg.setAcceptedTemplate(acceptedTemplate.getValue());
		notCfg.setAdminsNotificationGroup(adminsNotificationGroup.getValue());
		notCfg.setChannel(channel.getValue());
		notCfg.setRejectedTemplate(rejectedTemplate.getValue());
		notCfg.setUpdatedTemplate(updatedTemplate.getValue());
	}
	
	public List<String> getGroups()
	{
		return adminsNotificationGroup.getAllGroups();
	}
}
