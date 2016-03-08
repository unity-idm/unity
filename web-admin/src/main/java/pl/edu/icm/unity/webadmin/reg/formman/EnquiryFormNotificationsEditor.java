/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.formman;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.api.MessageTemplateManagement;
import pl.edu.icm.unity.server.api.NotificationsManagement;
import pl.edu.icm.unity.server.api.registration.NewEnquiryTemplateDef;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.registration.EnquiryFormNotifications;
import pl.edu.icm.unity.webui.common.CompatibleTemplatesComboBox;

import com.vaadin.ui.ComboBox;

/**
 * Editor of {@link EnquiryFormNotifications}
 * @author K. Benedyczak
 */
public class EnquiryFormNotificationsEditor extends BaseFormNotificationsEditor
{
	private ComboBox enquiryToFillTemplate;
	
	public EnquiryFormNotificationsEditor(UnityMessageSource msg,
			GroupsManagement groupsMan, NotificationsManagement notificationsMan,
			MessageTemplateManagement msgTempMan) throws EngineException
	{
		super(msg, groupsMan, notificationsMan, msgTempMan);
		initMyUI();
	}

	private void initMyUI() throws EngineException
	{
		enquiryToFillTemplate =  new CompatibleTemplatesComboBox(NewEnquiryTemplateDef.NAME, msgTempMan);
		enquiryToFillTemplate.setCaption(msg.getMessage("EnquiryFormViewer.enquiryToFillTemplate"));
		addComponents(enquiryToFillTemplate);
	}
	
	public void setValue(EnquiryFormNotifications toEdit)
	{
		super.setValue(toEdit);
		
		enquiryToFillTemplate.setValue(toEdit.getEnquiryToFillTemplate());
	}
	
	public EnquiryFormNotifications getValue()
	{
		EnquiryFormNotifications notCfg = new EnquiryFormNotifications();
		super.fill(notCfg);
		notCfg.setEnquiryToFillTemplate((String) enquiryToFillTemplate.getValue());
		return notCfg;
	}
}
