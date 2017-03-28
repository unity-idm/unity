/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes.ext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.stdext.attr.EnumAttributeSyntax;
import pl.edu.icm.unity.types.basic.AttributeValueSyntax;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.SmallTable;
import pl.edu.icm.unity.webui.common.attributes.AttributeSyntaxEditor;
import pl.edu.icm.unity.webui.common.attributes.AttributeValueEditor;
import pl.edu.icm.unity.webui.common.attributes.TextOnlyAttributeHandler;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandlerFactory;

import com.vaadin.server.Sizeable.Unit;
import com.vaadin.server.UserError;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

/**
 * Enum attribute handler for the web
 * @author K. Benedyczak
 */
@org.springframework.stereotype.Component
public class EnumAttributeHandler implements WebAttributeHandler<String>, WebAttributeHandlerFactory
{
	private UnityMessageSource msg;
	
	@Autowired
	public EnumAttributeHandler(UnityMessageSource msg)
	{
		this.msg = msg;
	}

	@Override
	public String getSupportedSyntaxId()
	{
		return EnumAttributeSyntax.ID;
	}
	
	@Override
	public Component getRepresentation(String value, AttributeValueSyntax<String> syntax, RepresentationSize size)
	{
		return new Label(value.toString(), ContentMode.PREFORMATTED);
	}
	
	@Override
	public AttributeValueEditor<String> getEditorComponent(String initialValue, String label,
			AttributeValueSyntax<String> syntax)
	{
		return new EnumValueEditor(initialValue, label, (EnumAttributeSyntax) syntax);
	}
	
	private class EnumValueEditor implements AttributeValueEditor<String>
	{
		private String value;
		private String label;
		private EnumAttributeSyntax syntax;
		private ComboBox field;
		private boolean required;
		
		public EnumValueEditor(String value, String label, EnumAttributeSyntax syntax)
		{
			this.value = value;
			this.syntax = syntax;
			this.label = label;
		}

		@Override
		public ComponentsContainer getEditor(boolean required, boolean adminMode)
		{
			this.required = required;
			field = new ComboBox(label);
			field.setNullSelectionAllowed(!required);
			field.setRequired(required);
			field.setTextInputAllowed(true);
			List<String> sortedAllowed = new ArrayList<>(syntax.getAllowed());
			Collections.sort(sortedAllowed);
			for (String allowed: sortedAllowed)
				field.addItem(allowed);
			if (value != null)
				field.setValue(value);
			else if (required)
				field.setValue(sortedAllowed.get(0));
			return new ComponentsContainer(field);
		}

		@Override
		public String getCurrentValue() throws IllegalAttributeValueException
		{
			String cur = (String)field.getValue();
			if (cur == null && !required)
				return null;
			try
			{
				syntax.validate(cur);
				field.setComponentError(null);
			} catch (IllegalAttributeValueException e)
			{
				field.setComponentError(new UserError(e.getMessage()));
				throw e;
			}
			return cur;
		}

		@Override
		public void setLabel(String label)
		{
			field.setCaption(label);
		}
	}

	@Override
	public WebAttributeHandler<?> createInstance()
	{
		return new EnumAttributeHandler(msg);
	}

	@Override
	public String getValueAsString(String value, AttributeValueSyntax<String> syntax, int limited)
	{
		return TextOnlyAttributeHandler.trimString(value.toString(), limited);
	}

	@Override
	public Component getSyntaxViewer(AttributeValueSyntax<String> syntaxR)
	{
		Table allowedTable = new SmallTable();
		allowedTable.setHeight(12, Unit.EM);
		allowedTable.setWidth(26, Unit.EM);
		allowedTable.addContainerProperty(msg.getMessage("EnumAttributeHandler.allowed"), 
				String.class, null);
		EnumAttributeSyntax syntax = (EnumAttributeSyntax) syntaxR;
		List<String> sortedAllowed = new ArrayList<>(syntax.getAllowed());
		Collections.sort(sortedAllowed);
		for (String allowed: sortedAllowed)
			allowedTable.addItem(new Object[] {allowed}, allowed);
		allowedTable.setReadOnly(true);
		
		VerticalLayout ret = new VerticalLayout();
		ret.addComponent(allowedTable);
		return ret;
	}
	@Override
	public AttributeSyntaxEditor<String> getSyntaxEditorComponent(
			AttributeValueSyntax<String> initialValue)
	{
		return new EnumSyntaxEditor((EnumAttributeSyntax) initialValue);
	}
	
	private class EnumSyntaxEditor implements AttributeSyntaxEditor<String>
	{
		private EnumAttributeSyntax initial;
		private TextField value;
		private Button add;
		private Table current;
		
		public EnumSyntaxEditor(EnumAttributeSyntax initial)
		{
			this.initial = initial;
		}

		@Override
		public Component getEditor()
		{
			VerticalLayout vl = new VerticalLayout();
			vl.setSpacing(true);
			
			HorizontalLayout hl = new HorizontalLayout();
			value = new TextField();
			hl.addComponent(value);
			add = new Button();
			add.setIcon(Images.add.getResource());
			add.setDescription(msg.getMessage("StringAttributeHandler.add"));
			add.addClickListener(new ClickListener()
			{
				@Override
				public void buttonClick(ClickEvent event)
				{
					String v = value.getValue(); 
					if (!v.equals(""))
					{
						current.addItem(new Object[] {v}, v);
						value.setValue("");
					}
				}
			});
			hl.addComponent(add);
			hl.setSpacing(true);
			
			vl.addComponent(hl);
			
			current = new SmallTable();
			current.addContainerProperty(msg.getMessage("EnumAttributeHandler.allowed"), 
					String.class, null);
			current.setHeight(12, Unit.EM);
			current.setWidth(26, Unit.EM);
			current.addActionHandler(new RemoveActionHandler());
			vl.addComponent(current);
			
			if (initial != null)
			{
				List<String> sortedAllowed = new ArrayList<>(initial.getAllowed());
				Collections.sort(sortedAllowed);
				for (String a: sortedAllowed)
					current.addItem(new Object[] {a}, a);
			}
			return vl;
		}

		@Override
		public AttributeValueSyntax<String> getCurrentValue()
				throws IllegalAttributeTypeException
		{
			EnumAttributeSyntax ret = new EnumAttributeSyntax();
			Set<String> allowed = new HashSet<String>();
			for (Object itemId: current.getItemIds())
				allowed.add((String)itemId);
			if (allowed.isEmpty())
				throw new IllegalAttributeTypeException(
						msg.getMessage("EnumAttributeHandler.errorNoValues"));
			ret.setAllowed(allowed);
			return ret;
		}
		
		private class RemoveActionHandler extends SingleActionHandler
		{
			public RemoveActionHandler()
			{
				super(msg.getMessage("EnumAttributeHandler.removeAction"), Images.delete.getResource());
			}

			@Override
			public void handleAction(Object sender, final Object target)
			{
				current.removeItem(target);
			}
		}

	}
	
}
