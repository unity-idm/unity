/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.msgtemplate;

import java.util.ArrayList;
import java.util.List;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.msgtemplates.MessageTemplate;
import pl.edu.icm.unity.server.api.MessageTemplateManagement;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.common.i18n.I18nLabel;

import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

/**
 *  Simple component allowing to view message template (name, subject, body).
 * @author P. Piernik
 *
 */
public class SimpleMessageTemplateViewer extends VerticalLayout
{

	protected UnityMessageSource msg;
	protected MessageTemplateManagement msgTempMan;
	protected List<Component> messages;
	protected FormLayout main;
	protected Label notSet;
	protected Label name;
	
	
	public SimpleMessageTemplateViewer(String caption, UnityMessageSource msg,
			MessageTemplateManagement msgTempMan)
	{
		super();
		this.msg = msg;
		this.msgTempMan = msgTempMan;
		initUI(caption);
		
	}
	
	protected void initUI(String caption)
	{
		
		if (caption !=null)
		{
			setCaption(caption);
		}
		messages = new ArrayList<Component>();
		main = new FormLayout();
		name = new Label();
		name.setCaption(msg.getMessage("MessageTemplateViewer.name"));
		main.addComponent(name);
		main.setSizeFull();
		main.setMargin(false);
		main.setSpacing(false);
		notSet = new Label(msg.getMessage("MessageTemplateViewer.notSet"));
		notSet.setVisible(false);
		addComponents(notSet, main);
		setSizeFull();
				
	}
	
	public void setInput(String template)
	{
		setEmpty();
		if (template == null)
		{	
			main.setVisible(false);
			notSet.setVisible(true);
			return;
		}
		notSet.setVisible(false);
		main.setVisible(true);
		name.setValue(template);
		try
		{
			MessageTemplate templateC = msgTempMan.getTemplate(template);
			
			I18nLabel subject = new I18nLabel(msg, msg.getMessage("MessageTemplateViewer.subject"));
			subject.setValue(templateC.getMessage().getSubject());
			I18nLabel body = new I18nLabel(msg, msg.getMessage("MessageTemplateViewer.body"));
			body.setValue(templateC.getMessage().getBody());
			body.setReadOnly(true);
			main.addComponents(subject, body);
			messages.add(subject);
			messages.add(body);
			
		} catch (EngineException e)
		{
			notSet.setValue(msg.getMessage("MessageTemplateViewer.errorMissingTemplate", template));
			notSet.setVisible(true);
			main.setVisible(false);
		}
	}

	protected void setEmpty()
	{
		name.setValue("");
		for (Component c : messages)
		{
			main.removeComponent(c);
		}
		messages.clear();
	}
}
