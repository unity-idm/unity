/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.attr.introspection.console;

import java.util.List;
import java.util.stream.Collectors;

import com.vaadin.flow.component.customfield.CustomField;

import io.imunity.attr.introspection.config.Attribute;
import io.imunity.vaadin.elements.grid.EditableGrid;
import pl.edu.icm.unity.base.message.MessageSource;

class AttributesGrid extends CustomField<List<Attribute>>
{
	private EditableGrid<AttributeEntryBean> grid;

	AttributesGrid(MessageSource msg)
	{
		this.grid = new EditableGrid<>(msg::getMessage, AttributeEntryBean::new);

		grid.addColumn(s -> s.getName(), (t, v) -> t.setName(v), false)
				.setHeader(msg.getMessage("AttributesGrid.name"));

		grid.addColumn(s -> s.getDescription(), (t, v) -> t.setDescription(v), false)
				.setHeader(msg.getMessage("AttributesGrid.description"));

		grid.addCheckboxColumn(s -> s.isMandatory(), (t, v) -> t.setMandatory(v))
				.setHeader(msg.getMessage("AttributesGrid.mandatory"));
		grid.addValueChangeListener(e -> fireEvent(new ComponentValueChangeEvent<>(this, this, getValue(), e.isFromClient())));
		grid.setSizeFull();
		add(grid);
		setSizeFull();
	}

	@Override
	public List<Attribute> getValue()
	{
		return grid.getValue()
				.stream()
				.map(a -> new Attribute(a.getName(), a.getDescription(), a.isMandatory()))
				.collect(Collectors.toList());
	}


	@Override
	protected List<Attribute> generateModelValue()
	{
		return getValue();
	}

	@Override
	protected void setPresentationValue(List<Attribute> value)
	{
		grid.setValue(value == null ? null
				: value.stream()
						.map(a -> new AttributeEntryBean(a.name, a.description, a.mandatory))
						.collect(Collectors.toList()));

	}

	public static class AttributeEntryBean
	{
		private String name;
		private String description;
		private boolean mandatory;

		public AttributeEntryBean()
		{
		}

		public AttributeEntryBean(String name, String description, Boolean mandatory)
		{
			this.name = name;
			this.mandatory = mandatory;
			this.description = description;
		}

		public String getName()
		{
			return name;
		}

		public void setName(String name)
		{
			this.name = name;
		}

		public String getDescription()
		{
			return description;
		}

		public void setDescription(String description)
		{
			this.description = description;
		}

		public boolean isMandatory()
		{
			return mandatory;
		}

		public void setMandatory(boolean mandatory)
		{
			this.mandatory = mandatory;
		}
	}

}
