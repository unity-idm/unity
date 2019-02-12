/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.formman;

import java.util.List;

import com.vaadin.ui.CheckBox;

import pl.edu.icm.unity.base.msgtemplates.reg.AcceptRegistrationTemplateDef;
import pl.edu.icm.unity.base.msgtemplates.reg.InvitationTemplateDef;
import pl.edu.icm.unity.base.msgtemplates.reg.RejectRegistrationTemplateDef;
import pl.edu.icm.unity.base.msgtemplates.reg.UpdateRegistrationTemplateDef;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.NotificationsManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.registration.BaseFormNotifications;
import pl.edu.icm.unity.webui.common.CompatibleTemplatesComboBox;
import pl.edu.icm.unity.webui.common.GroupComboBox;
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
	
	private CheckBox sendAdminCopy;
	private GroupComboBox adminsNotificationGroup;

	private CompatibleTemplatesComboBox rejectedTemplate;
	private CompatibleTemplatesComboBox acceptedTemplate;
	private CompatibleTemplatesComboBox updatedTemplate;
	private CompatibleTemplatesComboBox invitationTemplate;
	
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
		sendAdminCopy = new CheckBox(msg.getMessage("BaseFormNotificationsEditor.sendAdminCopy"));
		
		adminsNotificationGroup = new GroupComboBox(
				msg.getMessage("RegistrationFormViewer.adminsNotificationsGroup"), groupsMan);
		adminsNotificationGroup.setEmptySelectionAllowed(true);
		adminsNotificationGroup.setInput("/", true);
		
		rejectedTemplate =  new CompatibleTemplatesComboBox(RejectRegistrationTemplateDef.NAME, msgTempMan);
		rejectedTemplate.setCaption(msg.getMessage("RegistrationFormViewer.rejectedTemplate"));
		acceptedTemplate =  new CompatibleTemplatesComboBox(AcceptRegistrationTemplateDef.NAME, msgTempMan);
		acceptedTemplate.setCaption(msg.getMessage("RegistrationFormViewer.acceptedTemplate"));
		updatedTemplate =  new CompatibleTemplatesComboBox(UpdateRegistrationTemplateDef.NAME, msgTempMan);
		updatedTemplate.setCaption(msg.getMessage("RegistrationFormViewer.updatedTemplate"));	
		invitationTemplate =  new CompatibleTemplatesComboBox(InvitationTemplateDef.NAME, msgTempMan);
		invitationTemplate.setCaption(msg.getMessage("RegistrationFormViewer.invitationTemplate"));
		
		addComponents(sendAdminCopy, adminsNotificationGroup,
				rejectedTemplate, acceptedTemplate, updatedTemplate, invitationTemplate);
	}
	
	protected void setValue(BaseFormNotifications toEdit)
	{
		adminsNotificationGroup.setValue(toEdit.getAdminsNotificationGroup());
		sendAdminCopy.setValue(toEdit.isSendUserNotificationCopyToAdmin());
		rejectedTemplate.setValue(toEdit.getRejectedTemplate());
		acceptedTemplate.setValue(toEdit.getAcceptedTemplate());
		updatedTemplate.setValue(toEdit.getUpdatedTemplate());
		invitationTemplate.setValue(toEdit.getInvitationTemplate());
	}
	
	protected void fill(BaseFormNotifications notCfg)
	{
		notCfg.setAcceptedTemplate(acceptedTemplate.getValue());
		notCfg.setAdminsNotificationGroup(adminsNotificationGroup.getValue());
		notCfg.setSendUserNotificationCopyToAdmin(sendAdminCopy.getValue());
		notCfg.setRejectedTemplate(rejectedTemplate.getValue());
		notCfg.setUpdatedTemplate(updatedTemplate.getValue());
		notCfg.setInvitationTemplate(invitationTemplate.getValue());
	}
	
	public List<String> getGroups()
	{
		return adminsNotificationGroup.getAllGroups();
	}
}
