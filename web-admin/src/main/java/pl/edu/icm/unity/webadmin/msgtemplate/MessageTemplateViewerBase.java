/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.msgtemplate;

import java.util.ArrayList;
import java.util.List;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.i18n.I18nLabel;

import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

/**
 * Base for message template viewer 
 * @author P. Piernik
 *
 */
public abstract class MessageTemplateViewerBase extends VerticalLayout
{
	protected UnityMessageSource msg;
	protected List<Component> messages;
	protected FormLayout main;
	protected Label name;
	
	public MessageTemplateViewerBase( UnityMessageSource msg)
	{
		this.msg = msg;
		initUIBase();
	}
	
	protected void initUIBase()
	{
		messages = new ArrayList<Component>();
		main = new CompactFormLayout();
		name = new Label();
		name.setCaption(msg.getMessage("MessageTemplateViewer.name"));
		main.addComponent(name);
		main.setSizeFull();
		main.setMargin(false);
		main.setSpacing(false);
		addComponents(main);
		setSizeFull();
		initUI();			
	}
	
	public void setInput(String nameContent, I18nString subjectContent, I18nString bodyContent)
	{   		
		main.setVisible(true);
		main.setSpacing(true);
		name.setValue(nameContent);
		I18nLabel subject = new I18nLabel(msg, msg.getMessage("MessageTemplateViewer.subject"));
		subject.setValue(subjectContent);
		I18nLabel body = new I18nLabel(msg, msg.getMessage("MessageTemplateViewer.body"));
		body.setValue(bodyContent);
		messages.add(subject);
		messages.add(body);
		main.addComponents(subject, body);
	}

	public void clearContent()
	{
		name.setValue("");
		for (Component c : messages)
		{
			main.removeComponent(c);
		}
		messages.clear();
	}
	
	protected abstract void initUI();
}
