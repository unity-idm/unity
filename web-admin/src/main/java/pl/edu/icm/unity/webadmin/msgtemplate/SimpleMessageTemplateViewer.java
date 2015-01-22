/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.msgtemplate;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.msgtemplates.MessageTemplate;
import pl.edu.icm.unity.server.api.MessageTemplateManagement;
import pl.edu.icm.unity.server.utils.UnityMessageSource;

import com.vaadin.ui.Label;

/**
 * Simple component allowing to view message template (name, subject, body).
 * @author P. Piernik
 *
 */
public class SimpleMessageTemplateViewer extends MessageTemplateViewerBase
{
	private MessageTemplateManagement msgTempMan;
	private Label notSet;
	
	public SimpleMessageTemplateViewer(String caption, UnityMessageSource msg,
			MessageTemplateManagement msgTempMan)
	{
		super(msg);
		this.msgTempMan = msgTempMan;
		setCaption(caption);
	}
	
	protected void initUI()
	{	
		notSet = new Label();
		notSet.setVisible(false);
		addComponents(notSet);		
	}
	
	public void setInput(String template)
	{
		clearContent();
		notSet.setValue("");
		if (template == null)
		{	
			main.setVisible(false);
			notSet.setValue(msg.getMessage("MessageTemplateViewer.notSet"));
			notSet.setVisible(true);
			return;
		}
		notSet.setVisible(false);	
		try
		{
			MessageTemplate templateC = msgTempMan.getTemplate(template);
			setInput(templateC.getName(), templateC.getMessage().getSubject(), templateC.getMessage().getBody());
			
		} catch (EngineException e)
		{
			notSet.setValue(msg.getMessage("MessageTemplateViewer.errorMissingTemplate", template));
			notSet.setVisible(true);
			main.setVisible(false);
		}
	}
}
