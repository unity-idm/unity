/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.types.basic;

/**
 * Wrapper for {@link Attribute}. Contains attribute and additional metadata: displayedName, 
 * description and indicator whether attribute is mandatory.
 * This additional meta informations are filled after output translation profile processing 
 * and used when attributes are showing on consent screen: displayName
 * as attribute name and description as tooltip. If attribute is mandatory user cannot hide it 
 * on consent screen.   
 * 
 * @author P.Piernik
 */
public class DynamicAttribute
{
	private Attribute attribute;
	private AttributeType attributeType;
	private String displayedName;
	private String description;
	private boolean mandatory;

	
	public DynamicAttribute(Attribute attribute, String displayedName, String description, 
			boolean mandatory)
	{
		this.attribute = attribute;
		this.displayedName = displayedName;
		this.description = description;
		this.mandatory = mandatory;
	}
	
	public DynamicAttribute(Attribute attribute, AttributeType attributeType, String displayedName, String description, 
			boolean mandatory)
	{
		this(attribute, displayedName, description, mandatory);
		this.attributeType = attributeType;
	}
	
	public DynamicAttribute(Attribute attribute)
	{
		this.attribute = attribute;
		this.mandatory = false;
	}
	
	public DynamicAttribute(Attribute attribute, AttributeType attributeType)
	{
		this(attribute);
		this.attributeType = attributeType;
	}
	

	public Attribute getAttribute()
	{
		return attribute;
	}

	public void setAttribute(Attribute attribute)
	{
		this.attribute = attribute;
	}

	public String getDisplayedName()
	{
		return displayedName;
	}

	public void setDisplayedName(String displayedName)
	{
		this.displayedName = displayedName;
	}

	public String getDescription()
	{
		return description;
	}
	 
	public String getDescription(AttributeType t)
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
	
	public AttributeType getAttributeType()
	{
		return attributeType;
	}

	public void setAttributeType(AttributeType attributeType)
	{
		this.attributeType = attributeType;
	}

	@Override
	public DynamicAttribute clone()
	{
		return new DynamicAttribute(attribute.clone(), attributeType, displayedName, description, mandatory);
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(attribute.toString());
		sb.append(" with meta [");
		sb.append(displayedName);
		sb.append(", ");
		sb.append(description);
		sb.append(", ");
		sb.append(mandatory);
		sb.append("]");
		return sb.toString();
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		DynamicAttribute other = (DynamicAttribute) obj;
		if (attribute == null)
		{
			if (other.attribute != null)
				return false;
		} else if (!attribute.equals(other.attribute))
			return false;
		if (displayedName == null)
		{
			if (other.displayedName != null)
				return false;
		} else if (!displayedName.equals(other.displayedName))
			return false;
		if (description == null)
		{
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;

		if (!(mandatory == other.mandatory))
			return false;

		if (attributeType == null)
		{
			if (other.attributeType != null)
				return false;
		} else if (!attributeType.equals(other.attributeType))
			return false;
		
		
		return true;
	}
}
