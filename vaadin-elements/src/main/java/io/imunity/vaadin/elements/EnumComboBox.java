/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.elements;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import com.vaadin.flow.component.combobox.ComboBox;



/**
 * {@link ComboBox} allowing to simply select from enum constatnts.
 * 
 * @author K. Benedyczak
 * @param <T>
 */
public class EnumComboBox<T extends Enum<?>> extends ComboBox<T>{
	
	private Function<String, String> msg;
	private String msgPrefix;
	
	public EnumComboBox(Function<String, String> msg, String msgPrefix, Class<T> enumClass, T initialValue)
	{
		init(msg, msgPrefix, enumClass, initialValue, t -> true);
	}
	
	public EnumComboBox(String caption, Function<String, String> msg, String msgPrefix, Class<T> enumClass, 
			T initialValue)
	{
		this(caption, msg, msgPrefix, enumClass, initialValue, t -> true);
	}
	
	public EnumComboBox(String caption, Function<String, String> msg, String msgPrefix, Class<T> enumClass, 
			T initialValue,	Predicate<T> filter)
	{
		super(caption);
		init(msg, msgPrefix, enumClass, initialValue, filter);
	}

	private void init(Function<String, String> msg, String msgPrefix, Class<T> enumClass, T initialValue,
		Predicate<T> filter)
	{
		this.msg = msg;
		this.msgPrefix = msgPrefix;
		List<T> values = new ArrayList<>();
		T[] consts = enumClass.getEnumConstants();
		
		for (T constant: consts)
			if (filter.test(constant))
				values.add(constant);
		setItems(values);
		setItemLabelGenerator(i -> msgPrefix == null ? i.toString() : msg.apply(msgPrefix + i.toString()));
		setValue(initialValue);
	}

	public String getSelectedLabel()
	{
		return msg.apply(msgPrefix + getValue().toString());
	}
}
