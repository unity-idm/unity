/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.translation;




/**
 * Describes parameter of a translation action.
 * @author K. Benedyczak
 */
public class ActionParameterDefinition
{
	public enum Type {UNITY_ATTRIBUTE, EXPRESSION, UNITY_GROUP, UNITY_ID_TYPE, UNITY_CRED_REQ, ENUM, DAYS,
		LARGE_TEXT, I18N_TEXT, TEXT, BOOLEAN, UNITY_INPUT_TRANSLATION_PROFILE, UNITY_OUTPUT_TRANSLATION_PROFILE,
		USER_MESSAGE_TEMPLATE, REGISTRATION_FORM}
	
	private String name;
	private String descriptionKey;
	private Type type;
	private boolean mandatory;
	private Class<? extends Enum<?>> enumClass;
	
	public ActionParameterDefinition(String name, String descriptionKey, Type type, boolean mandatory)
	{
		this.name = name;
		this.descriptionKey = descriptionKey;
		this.type = type;
		this.mandatory = mandatory;
	}

	public ActionParameterDefinition(String name, String descriptionKey, Class<? extends Enum<?>> enumClass, boolean mandatory)
	{
		this.name = name;
		this.descriptionKey = descriptionKey;
		this.type = Type.ENUM;
		this.enumClass = enumClass;
		this.mandatory = mandatory;
	}

	public String getName()
	{
		return name;
	}

	public String getDescriptionKey()
	{
		return descriptionKey;
	}

	public Type getType()
	{
		return type;
	}

	public Class<? extends Enum<?>> getEnumClass()
	{
		return enumClass;
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
