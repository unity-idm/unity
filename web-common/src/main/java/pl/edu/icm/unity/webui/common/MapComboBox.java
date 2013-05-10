/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import java.util.Map;

import com.vaadin.data.util.BeanContainer;
import com.vaadin.data.util.BeanItem;
import com.vaadin.ui.ComboBox;

/**
 * Simple {@link ComboBox} which can be initialized to show a contents of a 
 * map. Each map key is used as a select label, but selected value can be retrieved as a domain object,
 * i.e. the map's value for the key.
 * @author K. Benedyczak
 */
public class MapComboBox<T> extends ComboBox
{
	public MapComboBox(Map<String, T> values, String initialValue)
	{
		init(values, initialValue);
	}
	
	public MapComboBox(String caption, Map<String, T> values, String initialValue)
	{
		super(caption);
		init(values, initialValue);
	}

	protected MapComboBox(String caption)
	{
		super(caption);
	}

	protected MapComboBox()
	{
	}
	
	protected final void init(Map<String, T> values, String initialValue)
	{
		BeanContainer<String, HolderBean> container = new BeanContainer<String, HolderBean>(HolderBean.class);
		setContainerDataSource(container);
		for (Map.Entry<String, T> constant: values.entrySet())
		{
			HolderBean bean = new HolderBean(constant.getKey(), constant.getValue()); 
			container.addItem(constant.getKey(), bean);
		}
		if (initialValue != null)
			setValue(initialValue);
		setNullSelectionAllowed(false);
	}
	
	public T getSelectedValue()
	{
		Object itemId = getValue();
		if (itemId == null)
			return null;
		@SuppressWarnings("unchecked")
		BeanItem<HolderBean> bean = ((BeanContainer<String, HolderBean>)getContainerDataSource()).getItem(itemId);
		return bean.getBean().getWrapped();
	}

	public String getSelectedLabel()
	{
		Object itemId = getValue();
		if (itemId == null)
			return null;
		@SuppressWarnings("unchecked")
		BeanItem<HolderBean> bean = ((BeanContainer<String, HolderBean>)getContainerDataSource()).getItem(itemId);
		return bean.getBean().toString();
	}
	
	private class HolderBean
	{
		private T wrapped;
		private String label;

		public HolderBean(String label, T wrapped)
		{
			this.wrapped = wrapped;
			this.label = label;
		}

		public T getWrapped()
		{
			return wrapped;
		}
		
		public String toString()
		{
			return label;
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((label == null) ? 0 : label.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			@SuppressWarnings("unchecked")
			HolderBean other = (HolderBean) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (label == null)
			{
				if (other.label != null)
					return false;
			} else if (!label.equals(other.label))
				return false;
			return true;
		}

		@SuppressWarnings("rawtypes")
		private MapComboBox getOuterType()
		{
			return MapComboBox.this;
		}
	}
}
