/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import com.google.common.html.HtmlEscapers;
import com.vaadin.server.Resource;
import com.vaadin.server.UserError;
import com.vaadin.shared.Registration;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TextField;
import com.vaadin.ui.CustomField;

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
	private Button button;
	private HorizontalLayout fieldWithButton;
	private String initVal;
	
	public TextFieldWithButton(String caption, Resource buttonIcon,	String buttonDescription,
			ButtonHandler handler)
	{
		if (caption != null)
			setCaption(caption);
		this.handler = handler;
		this.buttonDescription = buttonDescription;
		this.buttonIcon = buttonIcon;
	}
	
	public TextFieldWithButton(String caption, Resource buttonIcon,	String buttonDescription,
			ButtonHandler handler, String initVal)
	{
		this(caption, buttonIcon, buttonDescription, handler);
		this.initVal = initVal;
		
	}

	@Override
	protected Component initContent()
	{
		System.out.println("INITTTTT \n\n\n\n");
		
		fieldWithButton = new HorizontalLayout();
		fieldWithButton.setSpacing(true);
		fieldWithButton.setMargin(false);
		editor = new TextField();
		if (initVal != null)
			editor.setValue(initVal);
		fieldWithButton.addComponent(editor);
		
		button = new Button();
		button.addStyleName(Styles.vButtonSmall.toString());
		button.setIcon(buttonIcon);
		button.setDescription(HtmlEscapers.htmlEscaper().escape(buttonDescription));
		button.addClickListener(event -> {
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
		});

		fieldWithButton.addComponents(editor, button);
		return fieldWithButton;
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

	@Override
	public String getValue()
	{
		return editor.getValue();
	}

	@Override
	public void setId(String id)
	{
		if (editor != null)
			editor.setId(id);
	}
	
	@Override
	protected void doSetValue(String value)
	{
		editor.setValue(value);
		
	}
	
	@Override
	public Registration addValueChangeListener(ValueChangeListener<String> listener)
	{
		if (editor != null)
			return editor.addValueChangeListener(listener);
		return null;
	}
	
	public Button getButton()
	{
		return button;
	}

	public void removeButton()
	{
		fieldWithButton.removeComponent(button);		
	}
	
	
	
}
