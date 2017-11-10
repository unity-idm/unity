/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.msgtemplate;

import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.MessageTemplate;
import pl.edu.icm.unity.types.basic.MessageType;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.i18n.I18nLabelWithPreview;

/**
 * Base for message template viewer 
 * @author P. Piernik
 *
 */
public abstract class MessageTemplateViewerBase extends VerticalLayout
{
	protected UnityMessageSource msg;
	protected FormLayout main;
	protected Label name;
	
	public MessageTemplateViewerBase(UnityMessageSource msg)
	{
		this.msg = msg;
		initUIBase();
	}
	
	protected void initUIBase()
	{
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
	
	public void setInput(MessageTemplate template)
	{   		
		String nameContent = template.getName(); 
		I18nString subjectContent = template.getMessage().getSubject();
		I18nString bodyContent = template.getMessage().getBody();
		
		main.setVisible(true);
		main.setSpacing(true);
		name.setValue(nameContent);
		
		I18nLabelWithPreview body = I18nLabelWithPreview.builder(msg, msg.getMessage("MessageTemplateViewer.body"))
				.withMode(template.getType() == MessageType.HTML ? 
						ContentMode.HTML : ContentMode.PREFORMATTED)
				.buildWithValue(bodyContent);
		I18nLabelWithPreview subject = I18nLabelWithPreview.builder(msg, msg.getMessage("MessageTemplateViewer.subject"))
				.buildWithValue(subjectContent);
		main.addComponents(subject, body);
	}

	public void clearContent()
	{
		removeAllComponents();
		initUIBase();
	}
	
	protected abstract void initUI();
}
