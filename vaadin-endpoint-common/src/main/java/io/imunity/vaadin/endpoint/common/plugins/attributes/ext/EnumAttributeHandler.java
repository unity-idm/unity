/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.attributes.ext;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;

import io.imunity.vaadin.elements.grid.GridWithActionColumn;
import io.imunity.vaadin.elements.grid.SingleActionHandler;
import io.imunity.vaadin.endpoint.common.plugins.ComponentsContainer;
import io.imunity.vaadin.endpoint.common.plugins.attributes.*;
import io.imunity.vaadin.endpoint.common.plugins.attributes.components.GenericElementsTable;

import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.attribute.IllegalAttributeTypeException;
import pl.edu.icm.unity.base.attribute.IllegalAttributeValueException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.stdext.attr.EnumAttributeSyntax;

import java.util.*;

class EnumAttributeHandler implements WebAttributeHandler
{
	private final MessageSource msg;
	private final EnumAttributeSyntax syntax;
	
	EnumAttributeHandler(MessageSource msg, EnumAttributeSyntax syntax)
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
		private final String value;
		private final String label;
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
		private final EnumAttributeSyntax initial;
		private final MessageSource msg;
		private TextField value;
		private GridWithActionColumn<String> current;

		public EnumSyntaxEditor(EnumAttributeSyntax initial, MessageSource msg)
		{
			this.initial = initial;
			this.msg = msg;
		}

		@Override
		public Optional<Component>  getEditor()
		{
			VerticalLayout vl = new VerticalLayout();
			vl.setSpacing(false);
			vl.setMargin(false);
			vl.setPadding(false);
			
			HorizontalLayout hl = new HorizontalLayout();
			value = new TextField();
			hl.add(value);
			Button add = new Button();
			add.setIcon(VaadinIcon.PLUS_CIRCLE_O.create());
			add.setTooltipText(msg.getMessage("StringAttributeHandler.add"));
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
			hl.setPadding(false);
			
			vl.add(hl);
	
			current = new GridWithActionColumn<String>(msg::getMessage, List.of(getDeleteAction()));
			current.addColumn(v -> v).setHeader(msg.getMessage("EnumAttributeHandler.value"));
			current.refreshActionColumn();
			current.setHeight(12, Unit.EM);
			current.setWidth(26, Unit.EM);
			
			vl.add(current);
			
			if (initial != null)
			{
				current.setItems(new ArrayList<>(initial.getAllowed()));
				
			}
			return Optional.of(vl);
		}

		@Override
		public AttributeValueSyntax<String> getCurrentValue()
				throws IllegalAttributeTypeException
		{
			EnumAttributeSyntax ret = new EnumAttributeSyntax();
			Set<String> allowed = new HashSet<>(current.getElements());
			
			if (allowed.isEmpty())
				throw new IllegalAttributeTypeException(
						msg.getMessage("EnumAttributeHandler.errorNoValues"));
			ret.setAllowed(allowed);
			return ret;
		}
		
		private SingleActionHandler<String> getDeleteAction()
		{
			return SingleActionHandler.builder4Delete(msg::getMessage, String.class)
					.withHandler(this::deleteHandler)
					.build();
		}
		
		private void deleteHandler(Set<String> items)
		{	
			current.removeElement(items.iterator().next());
		}
	}
	
	@org.springframework.stereotype.Component
	static class EnumAttributeHandlerFactory implements WebAttributeHandlerFactory
	{
		private final MessageSource msg;

		@Autowired
		EnumAttributeHandlerFactory(MessageSource msg)
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
