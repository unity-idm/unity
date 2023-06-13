/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.signupAndEnquiry.forms;

import java.util.List;

import com.vaadin.ui.CheckBox;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.msgtemplates.reg.AcceptRegistrationTemplateDef;
import pl.edu.icm.unity.base.msgtemplates.reg.InvitationProcessedNotificationTemplateDef;
import pl.edu.icm.unity.base.msgtemplates.reg.InvitationTemplateDef;
import pl.edu.icm.unity.base.msgtemplates.reg.RejectRegistrationTemplateDef;
import pl.edu.icm.unity.base.msgtemplates.reg.UpdateRegistrationTemplateDef;
import pl.edu.icm.unity.base.registration.BaseFormNotifications;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.NotificationsManagement;
import pl.edu.icm.unity.webui.common.CompatibleTemplatesComboBox;
import pl.edu.icm.unity.webui.common.GroupComboBox;
import pl.edu.icm.unity.webui.common.LayoutEmbeddable;

/**
 * Code for editing BaseFormNotifications, i.e. a base for registration and enquiry forms notifications config editing.
 * @author K. Benedyczak
 */
public class BaseFormNotificationsEditor extends LayoutEmbeddable
{
	protected final MessageSource msg;
	protected final GroupsManagement groupsMan;
	protected final NotificationsManagement notificationsMan;
	protected final MessageTemplateManagement msgTempMan;
	
	private CheckBox sendAdminCopy;
	private GroupComboBox adminsNotificationGroup;

	private CompatibleTemplatesComboBox rejectedTemplate;
	private CompatibleTemplatesComboBox acceptedTemplate;
	private CompatibleTemplatesComboBox updatedTemplate;
	private CompatibleTemplatesComboBox invitationTemplate;
	private CompatibleTemplatesComboBox invitationProcessedTemplate;

	
	public BaseFormNotificationsEditor(MessageSource msg, GroupsManagement groupsMan,
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
		invitationProcessedTemplate = new CompatibleTemplatesComboBox(InvitationProcessedNotificationTemplateDef.NAME, msgTempMan);
		invitationProcessedTemplate.setCaption(msg.getMessage("RegistrationFormViewer.invitationProcessedTemplate"));
		
		addComponents(sendAdminCopy, adminsNotificationGroup,
				rejectedTemplate, acceptedTemplate, updatedTemplate, invitationTemplate, invitationProcessedTemplate);
	}
	
	protected void setValue(BaseFormNotifications toEdit)
	{
		adminsNotificationGroup.setValue(toEdit.getAdminsNotificationGroup());
		sendAdminCopy.setValue(toEdit.isSendUserNotificationCopyToAdmin());
		rejectedTemplate.setValue(toEdit.getRejectedTemplate());
		acceptedTemplate.setValue(toEdit.getAcceptedTemplate());
		updatedTemplate.setValue(toEdit.getUpdatedTemplate());
		invitationTemplate.setValue(toEdit.getInvitationTemplate());
		invitationProcessedTemplate.setValue(toEdit.getInvitationProcessedTemplate());
	}
	
	protected void fill(BaseFormNotifications notCfg)
	{
		notCfg.setAcceptedTemplate(acceptedTemplate.getValue());
		notCfg.setAdminsNotificationGroup(adminsNotificationGroup.getValue());
		notCfg.setSendUserNotificationCopyToAdmin(sendAdminCopy.getValue());
		notCfg.setRejectedTemplate(rejectedTemplate.getValue());
		notCfg.setUpdatedTemplate(updatedTemplate.getValue());
		notCfg.setInvitationTemplate(invitationTemplate.getValue());
		notCfg.setInvitationProcessedTemplate(invitationProcessedTemplate.getValue());
	}
	
	public List<String> getGroups()
	{
		return adminsNotificationGroup.getAllGroups();
	}
}
