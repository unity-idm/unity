/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin23.shared.endpoint.plugins.attributes.ext;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import io.imunity.vaadin23.shared.endpoint.components.GenericElementsTable;
import io.imunity.vaadin23.shared.endpoint.components.SingleActionHandler;
import io.imunity.vaadin23.shared.endpoint.components.ComponentsContainer;
import io.imunity.vaadin23.shared.endpoint.plugins.attributes.*;
import org.springframework.beans.factory.annotation.Autowired;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.stdext.attr.EnumAttributeSyntax;

import java.util.*;

public class EnumAttributeHandler implements WebAttributeHandler
{
	private MessageSource msg;
	private EnumAttributeSyntax syntax;
	
	public EnumAttributeHandler(MessageSource msg, EnumAttributeSyntax syntax)
	{
		this.msg = msg;
		this.syntax = syntax;
	}

	@Override
	public Component getRepresentation(String value, AttributeViewerContext context)
	{
		return AttributeHandlerHelper.getRepresentation(value, context);
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
			field.setAllowCustomValue(true);
			field.setRequired(!required);
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
				field.setInvalid(false);
			} catch (IllegalAttributeValueException e)
			{
				field.setInvalid(true);
				field.setErrorMessage(e.getMessage());
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
				field.setLabel(label);
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
		ret.add(allowedTable);
		return ret;
	}
	
	private static class EnumSyntaxEditor implements AttributeSyntaxEditor<String>
	{
		private EnumAttributeSyntax initial;
		private TextField value;
		private Button add;
		private GenericElementsTable<String> current;
		private MessageSource msg;
		
		public EnumSyntaxEditor(EnumAttributeSyntax initial, MessageSource msg)
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
			hl.add(value);
			add = new Button();
			add.setIcon(VaadinIcon.PLUS_CIRCLE_O.create());
			add.getElement().setProperty("title", msg.getMessage("StringAttributeHandler.add"));
			add.addClickListener(event ->  {
					String v = value.getValue(); 
					if (!v.equals(""))
					{
						if (current.getElements().contains(v))
							return;
						current.addElement(v);
						value.setValue("");
					}
				}
			);
			hl.add(add);
			hl.setSpacing(true);
			hl.setMargin(false);
			
			vl.add(hl);
	
			current = new GenericElementsTable<>(msg.getMessage("EnumAttributeHandler.allowed"));
			current.setHeight(12, Unit.EM);
			current.setWidth(26, Unit.EM);
			current.addActionHandler(getDeleteAction());
			
			vl.add(current);
			
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
	public static class EnumAttributeHandlerFactoryV23 implements WebAttributeHandlerFactory
	{
		private MessageSource msg;

		@Autowired
		public EnumAttributeHandlerFactoryV23(MessageSource msg)
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
