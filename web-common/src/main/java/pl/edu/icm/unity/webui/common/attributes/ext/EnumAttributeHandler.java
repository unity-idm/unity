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

import com.vaadin.server.Sizeable.Unit;
import com.vaadin.server.UserError;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.stdext.attr.EnumAttributeSyntax;
import pl.edu.icm.unity.webui.common.ComponentsContainer;
import pl.edu.icm.unity.webui.common.GenericElementsTable;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.attributes.AttributeSyntaxEditor;
import pl.edu.icm.unity.webui.common.attributes.AttributeViewerContext;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandlerFactory;
import pl.edu.icm.unity.webui.common.attributes.edit.AttributeEditContext;
import pl.edu.icm.unity.webui.common.attributes.edit.AttributeValueEditor;

/**
 * Enum attribute handler for the web
 * @author K. Benedyczak
 */
public class EnumAttributeHandler implements WebAttributeHandler
{
	private UnityMessageSource msg;
	private EnumAttributeSyntax syntax;
	
	public EnumAttributeHandler(UnityMessageSource msg, EnumAttributeSyntax syntax)
	{
		this.msg = msg;
		this.syntax = syntax;
	}

	@Override
	public Component getRepresentation(String value, AttributeViewerContext context)
	{
		Label label = new Label(value.toString(), ContentMode.PREFORMATTED);
		if (context.isCustomWidth())
			label.setWidth(context.getCustomWidth(), context.getCustomWidthUnit());
		return label;
	}
	
	@Override
	public AttributeValueEditor getEditorComponent(String initialValue, String label)
	{
		return new EnumValueEditor(initialValue, label);
	}
	
	private class EnumValueEditor implements AttributeValueEditor
	{
		private String value;
		private String label;
		private ComboBox<String> field;
		private boolean required;
		private AttributeEditContext context;
		
		public EnumValueEditor(String value, String label)
		{
			this.value = value;
			this.label = label;
		}

		@Override
		public ComponentsContainer getEditor(AttributeEditContext context)
		{
			this.required = context.isRequired();
			this.context = context;
			field = new ComboBox<>();
			setLabel(label);
			field.setRequiredIndicatorVisible(required);
			field.setTextInputAllowed(true);
			field.setEmptySelectionAllowed(!required);
			List<String> sortedAllowed = new ArrayList<>(syntax.getAllowed());
			Collections.sort(sortedAllowed);
			field.setItems(sortedAllowed);	
			if (value != null)
				field.setValue(value);
			else if (required)
				field.setValue(sortedAllowed.get(0));
			if (context.isCustomWidth())
				field.setWidth(context.getCustomWidth(), context.getCustomWidthUnit());
			return new ComponentsContainer(field);
		}

		@Override
		public String getCurrentValue() throws IllegalAttributeValueException
		{	
			String cur = field.getValue();
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
			if (context.isShowLabelInline())
				field.setPlaceholder(label);
			else
				field.setCaption(label);
		}
	}

	@Override
	public String getValueAsString(String value)
	{
		return value;
	}

	@Override
	public Component getSyntaxViewer()
	{	
		GenericElementsTable<String> allowedTable = new GenericElementsTable<>(
				msg.getMessage("EnumAttributeHandler.allowed"));
		allowedTable.setHeight(12, Unit.EM);
		allowedTable.setWidth(26, Unit.EM);
		allowedTable.setInput(syntax.getAllowed());
		 
		VerticalLayout ret = new VerticalLayout();
		ret.setMargin(false);
		ret.setSpacing(false);
		ret.addComponent(allowedTable);
		return ret;
	}
	
	private static class EnumSyntaxEditor implements AttributeSyntaxEditor<String>
	{
		private EnumAttributeSyntax initial;
		private TextField value;
		private Button add;
		private GenericElementsTable<String> current;
		private UnityMessageSource msg;
		
		public EnumSyntaxEditor(EnumAttributeSyntax initial, UnityMessageSource msg)
		{
			this.initial = initial;
			this.msg = msg;
		}

		@Override
		public Component getEditor()
		{
			VerticalLayout vl = new VerticalLayout();
			vl.setSpacing(true);
			vl.setMargin(false);
			
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
						if (current.getElements().contains(v))
							return;
						current.addElement(v);
						value.setValue("");
					}
				}
			});
			hl.addComponent(add);
			hl.setSpacing(true);
			hl.setMargin(false);
			
			vl.addComponent(hl);
	
			current = new GenericElementsTable<>(msg.getMessage("EnumAttributeHandler.allowed"));
			current.setHeight(12, Unit.EM);
			current.setWidth(26, Unit.EM);
			current.addActionHandler(getDeleteAction());
			
			vl.addComponent(current);
			
			if (initial != null)
			{
				current.setInput(initial.getAllowed());
				
			}
			return vl;
		}

		@Override
		public AttributeValueSyntax<String> getCurrentValue()
				throws IllegalAttributeTypeException
		{
			EnumAttributeSyntax ret = new EnumAttributeSyntax();
			Set<String> allowed = new HashSet<>();
			allowed.addAll(current.getElements());
			
			if (allowed.isEmpty())
				throw new IllegalAttributeTypeException(
						msg.getMessage("EnumAttributeHandler.errorNoValues"));
			ret.setAllowed(allowed);
			return ret;
		}
		
		private SingleActionHandler<String> getDeleteAction()
		{
			return SingleActionHandler.builder4Delete(msg, String.class)
					.withHandler(this::deleteHandler)
					.build();
		}
		
		private void deleteHandler(Set<String> items)
		{	
			current.removeElement(items.iterator().next());
		}
	}
	
	@org.springframework.stereotype.Component
	public static class EnumAttributeHandlerFactory implements WebAttributeHandlerFactory
	{
		private UnityMessageSource msg;

		@Autowired
		public EnumAttributeHandlerFactory(UnityMessageSource msg)
		{
			this.msg = msg;
		}

		@Override
		public String getSupportedSyntaxId()
		{
			return EnumAttributeSyntax.ID;
		}

		@Override
		public WebAttributeHandler createInstance(AttributeValueSyntax<?> syntax)
		{
			return new EnumAttributeHandler(msg, (EnumAttributeSyntax) syntax);
		}

		@Override
		public AttributeSyntaxEditor<String> getSyntaxEditorComponent(
				AttributeValueSyntax<?> initialValue)
		{
			return new EnumSyntaxEditor((EnumAttributeSyntax) initialValue, msg);
		}
	}
}
