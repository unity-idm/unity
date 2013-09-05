/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import com.vaadin.server.Resource;
import com.vaadin.server.UserError;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.themes.Reindeer;

/**
 * Field Component composed of a TextField with a Button.
 *  
 * @author K. Benedyczak
 */
public class TextFieldWithButton extends CustomField<String>
{
	private ButtonHandler handler;
	private TextField editor;
	private Resource buttonIcon;
	private String buttonDescription;
	
	public TextFieldWithButton(String caption, Resource buttonIcon,	String buttonDescription,
			ButtonHandler handler)
	{
		setCaption(caption);
		this.handler = handler;
		this.buttonDescription = buttonDescription;
		this.buttonIcon = buttonIcon;
	}
	
	@Override
	protected Component initContent()
	{
		HorizontalLayout ret = new HorizontalLayout();
		ret.setSpacing(true);
		editor = new TextField();
		ret.addComponent(editor);
		
		Button b = new Button();
		b.addStyleName(Reindeer.BUTTON_SMALL);
		b.setIcon(buttonIcon);
		b.setDescription(buttonDescription);
		b.addClickListener(new ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				String value = editor.getValue();
				String error = handler.validate(value);
				if (error == null)
				{
					setComponentError(null);
					if (handler.perform(value))
						editor.setValue("");
				} else
				{
					setComponentError(new UserError(error));
				}
			}
		});

		ret.addComponents(editor, b);
		return ret;
	}

	@Override
	public Class<? extends String> getType()
	{
		return String.class;
	}
	
	public interface ButtonHandler
	{
		/**
		 * @param value
		 * @return null if valid, error message otherwise
		 */
		public String validate(String value);
		
		/**
		 * @param value
		 * @return true if should clean the editor.
		 */
		public boolean perform(String value);
	}
	
}
