/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation;



/**
 * Describes {@link TranslationAction} parameter.
 * @author K. Benedyczak
 */
public class ActionParameterDesc
{
	public enum Type {UNITY_ATTRIBUTE, EXPRESSION, UNITY_GROUP, UNITY_ID_TYPE, UNITY_CRED_REQ, ENUM, DAYS}
	
	private String name;
	private String descriptionKey;
	private int minValues;
	private int maxValues;
	private Type type;
	private Class<? extends Enum<?>> enumClass;
	
	
	public ActionParameterDesc(String name, String descriptionKey, int minValues,
			int maxValues, Type type)
	{
		super();
		this.name = name;
		this.descriptionKey = descriptionKey;
		this.minValues = minValues;
		this.maxValues = maxValues;
		this.type = type;
	}

	public ActionParameterDesc(String name, String descriptionKey, int minValues,
			int maxValues, Class<? extends Enum<?>> enumClass)
	{
		super();
		this.name = name;
		this.descriptionKey = descriptionKey;
		this.minValues = minValues;
		this.maxValues = maxValues;
		this.type = Type.ENUM;
		this.enumClass = enumClass;
	}

	public String getName()
	{
		return name;
	}

	public String getDescriptionKey()
	{
		return descriptionKey;
	}

	public int getMinValues()
	{
		return minValues;
	}

	public int getMaxValues()
	{
		return maxValues;
	}

	public Type getType()
	{
		return type;
	}

	public Class<? extends Enum<?>> getEnumClass()
	{
		return enumClass;
	}
	
}
