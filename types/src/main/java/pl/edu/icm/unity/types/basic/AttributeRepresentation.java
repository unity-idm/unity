/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.basic;


/**
 * AttributeExt wrapper, preparing it for JSON serialization. Type details are removed, 
 * values are properly serialized. Used whenever the attribute is going to be returned.
 * @author K. Benedyczak
 */
public class AttributeRepresentation extends AttributeParamRepresentation
{
	private boolean direct;
	private String syntax;
	
	public AttributeRepresentation(AttributeExt<?> orig)
	{
		super(orig);
		this.direct = orig.isDirect();
		this.syntax = orig.getAttributeSyntax().getValueSyntaxId();
	}

	public AttributeRepresentation()
	{
	}

	public String getSyntax()
	{
		return syntax;
	}

	public boolean isDirect()
	{
		return direct;
	}
}