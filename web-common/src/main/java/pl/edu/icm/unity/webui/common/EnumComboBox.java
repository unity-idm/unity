/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import java.util.TreeMap;

import pl.edu.icm.unity.server.utils.UnityMessageSource;

import com.vaadin.data.Property;
import com.vaadin.ui.ComboBox;

/**
 * {@link ComboBox} allowing to simply select from enum constatnts.
 * 
 * @author K. Benedyczak
 * @param <T>
 */
public class EnumComboBox<T extends Enum<?>> extends MapComboBox<T>
{
	private UnityMessageSource msg;
	private String msgPrefix;
	
	public EnumComboBox(UnityMessageSource msg, String msgPrefix, Class<T> enumClass, T initialValue)
	{
		init(msg, msgPrefix, enumClass, initialValue);
	}
	
	public EnumComboBox(String caption, UnityMessageSource msg, String msgPrefix, Class<T> enumClass, T initialValue)
	{
		super(caption);
		init(msg, msgPrefix, enumClass, initialValue);
	}
	
	private void init(UnityMessageSource msg, String msgPrefix, Class<T> enumClass, T initialValue)
	{
		this.msg = msg;
		this.msgPrefix = msgPrefix;
		TreeMap<String, T> values = new TreeMap<String, T>();
		T[] consts = enumClass.getEnumConstants();
		
		for (T constant: consts)
			values.put(msg.getMessage(msgPrefix+constant.toString()), constant);
		super.init(values, msg.getMessage(msgPrefix+initialValue.toString()));
	}
	
	/**
	 * In case of i18n the value might be different
	 */
	public void setEnumValue(T newValue) throws Property.ReadOnlyException 
	{
		String realValue = msg.getMessage(msgPrefix+newValue.toString());
		super.setValue(realValue);
	}

}
