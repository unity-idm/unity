/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.notifications;

import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.notifications.NotificationTemplate;
import pl.edu.icm.unity.notifications.TemplatesStore;
import pl.edu.icm.unity.server.utils.UnityMessageSource;

import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.VerticalLayout;

/**
 * Simple component allowing to view notification template contents. 
 * @author K. Benedyczak
 */
public class TemplateViewer extends CustomComponent
{
	private UnityMessageSource msg;
	private TemplatesStore store;
	private Label name;
	private TextArea contents;
	private Label notSet;
	
	public TemplateViewer(String caption, TemplatesStore store, UnityMessageSource msg)
	{
		this.msg = msg;
		this.store = store;
		initUI(caption);
	}
	
	private void initUI(String caption)
	{
		VerticalLayout main = new VerticalLayout();
		FormLayout top = new FormLayout();
		notSet = new Label(msg.getMessage("TemplateViewer.notSet"));
		name = new Label();
		name.setCaption(msg.getMessage("TemplateViewer.templateName"));
		top.addComponents(notSet, name);
		contents = new TextArea();
		contents.setWidth(100, Unit.PERCENTAGE);
		contents.setHeight(5, Unit.EM);
		contents.setReadOnly(true);
		main.addComponents(top, contents);
		setCaption(caption);
		setCompositionRoot(main);
	}
	
	public void setInput(String template)
	{
		if (template == null)
		{
			name.setVisible(false);
			contents.setVisible(false);
			notSet.setVisible(true);
			return;
		}
		name.setVisible(true);
		contents.setVisible(true);
		notSet.setVisible(false);
		name.setValue(template);
		
		try
		{
			NotificationTemplate templateC = store.getTemplate(template);
			String contentsS = msg.getMessage("TemplateViewer.subject") + ": " +
					templateC.getRawSubject() + "\n\n" + templateC.getRawBody(); 
			contents.setReadOnly(false);
			contents.setValue(contentsS);
			contents.setReadOnly(true);
		} catch (WrongArgumentException e)
		{
			contents.setValue(msg.getMessage("TemplateViewer.errorMissingTemplate", template));
		}
	}
}
