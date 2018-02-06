/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.vaadin.ui.ComboBox;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;

/**
 * {@link ComboBox} allowing to simply select from enum constatnts.
 * 
 * @author K. Benedyczak
 * @param <T>
 */
public class EnumComboBox<T extends Enum<?>> extends ComboBox<T>{
	
	private UnityMessageSource msg;
	private String msgPrefix;
	
	public EnumComboBox(UnityMessageSource msg, String msgPrefix, Class<T> enumClass, T initialValue)
	{
		init(msg, msgPrefix, enumClass, initialValue, new HashSet<T>());
	}
	
	public EnumComboBox(String caption, UnityMessageSource msg, String msgPrefix, Class<T> enumClass, 
			T initialValue)
	{
		this(caption, msg, msgPrefix, enumClass, initialValue, new HashSet<T>());
	}
	
	public EnumComboBox(String caption, UnityMessageSource msg, String msgPrefix, Class<T> enumClass, 
			T initialValue,	Set<T> hidden)
	{
		super(caption);
		init(msg, msgPrefix, enumClass, initialValue, hidden);
	}

	private void init(UnityMessageSource msg, String msgPrefix, Class<T> enumClass, T initialValue, 
			Set<T> hidden)
	{
		this.msg = msg;
		this.msgPrefix = msgPrefix;
		List<T> values = new ArrayList<>();
		T[] consts = enumClass.getEnumConstants();
		
		for (T constant: consts)
			if (!hidden.contains(constant))
				values.add(constant);
		setEmptySelectionAllowed(false);
		setItems(values);
		setItemCaptionGenerator(i -> msg.getMessage(msgPrefix + i.toString()));
		setValue(initialValue);
	}

	public String getSelectedLabel()
	{
		return msg.getMessage(msgPrefix + getValue().toString());
	}
}
