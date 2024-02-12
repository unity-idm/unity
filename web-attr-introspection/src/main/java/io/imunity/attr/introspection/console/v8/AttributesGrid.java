/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.attr.introspection.console.v8;

import java.util.List;
import java.util.stream.Collectors;

import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;

import io.imunity.attr.introspection.config.Attribute;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.common.GridWithEditor;

class AttributesGrid extends CustomField<List<Attribute>>
{
	private GridWithEditor<AttributeEntryBean> grid;

	AttributesGrid(MessageSource msg)
	{
		this.grid = new GridWithEditor<>(msg, AttributeEntryBean.class, t -> false, true, false, "");

		grid.addTextColumn(s -> s.getName(), (t, v) -> t.setName(v), msg.getMessage("AttributesGrid.name"), 30, true);

		grid.addTextColumn(s -> s.getDescription(), (t, v) -> t.setDescription(v),
				msg.getMessage("AttributesGrid.description"), 50, false);

		grid.addCheckBoxColumn(s -> s.isMandatory(), (t, v) -> t.setMandatory(v),
				msg.getMessage("AttributesGrid.mandatory"), 10);
		grid.addValueChangeListener(e -> fireEvent(new ValueChangeEvent<List<Attribute>>(this, getValue(), true)));
		
	}

	@Override
	public List<Attribute> getValue()
	{
		return grid.getValue().stream().map(a -> new Attribute(a.getName(), a.getDescription(), a.isMandatory()))
				.collect(Collectors.toList());
	}

	@Override
	protected Component initContent()
	{
		return grid;
	}

	@Override
	protected void doSetValue(List<Attribute> value)
	{
		grid.setValue(value == null ? null
				: value.stream().map(a -> new AttributeEntryBean(a.name, a.description, a.mandatory))
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
