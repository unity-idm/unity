/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.signup_and_enquiry;

import com.vaadin.flow.component.checkbox.Checkbox;
import io.imunity.console.views.directory_browser.group_details.GroupComboBox;
import io.imunity.console_utils.tprofile.LayoutEmbeddable;
import io.imunity.vaadin.endpoint.common.message_templates.CompatibleTemplatesComboBox;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.msg_template.reg.*;
import pl.edu.icm.unity.base.registration.BaseFormNotifications;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.NotificationsManagement;

import java.util.List;

import static io.imunity.vaadin.elements.CSSVars.TEXT_FIELD_MEDIUM;

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
	
	private Checkbox sendAdminCopy;
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
		sendAdminCopy = new Checkbox(msg.getMessage("BaseFormNotificationsEditor.sendAdminCopy"));
		
		adminsNotificationGroup = new GroupComboBox(
				msg.getMessage("RegistrationFormViewer.adminsNotificationsGroup"), groupsMan);
		adminsNotificationGroup.setInput("/", true);
		adminsNotificationGroup.setWidth(TEXT_FIELD_MEDIUM.value());
		
		rejectedTemplate =  new CompatibleTemplatesComboBox(RejectRegistrationTemplateDef.NAME, msgTempMan);
		rejectedTemplate.setLabel(msg.getMessage("RegistrationFormViewer.rejectedTemplate"));
		rejectedTemplate.setWidth(TEXT_FIELD_MEDIUM.value());
		acceptedTemplate =  new CompatibleTemplatesComboBox(AcceptRegistrationTemplateDef.NAME, msgTempMan);
		acceptedTemplate.setLabel(msg.getMessage("RegistrationFormViewer.acceptedTemplate"));
		acceptedTemplate.setWidth(TEXT_FIELD_MEDIUM.value());
		updatedTemplate =  new CompatibleTemplatesComboBox(UpdateRegistrationTemplateDef.NAME, msgTempMan);
		updatedTemplate.setLabel(msg.getMessage("RegistrationFormViewer.updatedTemplate"));
		updatedTemplate.setWidth(TEXT_FIELD_MEDIUM.value());
		invitationTemplate =  new CompatibleTemplatesComboBox(InvitationTemplateDef.NAME, msgTempMan);
		invitationTemplate.setLabel(msg.getMessage("RegistrationFormViewer.invitationTemplate"));
		invitationTemplate.setWidth(TEXT_FIELD_MEDIUM.value());
		invitationProcessedTemplate = new CompatibleTemplatesComboBox(InvitationProcessedNotificationTemplateDef.NAME, msgTempMan);
		invitationProcessedTemplate.setLabel(msg.getMessage("RegistrationFormViewer.invitationProcessedTemplate"));
		invitationProcessedTemplate.setWidth(TEXT_FIELD_MEDIUM.value());

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