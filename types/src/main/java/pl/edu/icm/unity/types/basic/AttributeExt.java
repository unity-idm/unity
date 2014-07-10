/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.basic;

import java.util.ArrayList;

/**
 * Extends the basic {@link Attribute} with metadata which is set by the server when returning 
 * attributes.
 * @author K. Benedyczak
 * @param <T>
 */
public class AttributeExt<T> extends Attribute<T>
{
	private boolean direct;
	
	public AttributeExt(Attribute<T> baseAttribute, boolean isDirect)
	{
		super(baseAttribute.getName(), baseAttribute.getAttributeSyntax(), baseAttribute.getGroupPath(), 
				baseAttribute.getVisibility(), new ArrayList<T>(baseAttribute.getValues()));
		this.direct = isDirect;
	}
	
	/**
	 * Cloning constructor. Deep cloning is performed.
	 * @param source
	 */
	public AttributeExt(AttributeExt<T> source)
	{
		this(source, source.isDirect());
	}
	
	/**
	 * @return if true, the attribute is direclt defined in the group of its scope. If false it is an 
	 * implied attribute, assigned by group's attribute statements.
	 */
	public boolean isDirect()
	{
		return direct;
	}

	public void setDirect(boolean direct)
	{
		this.direct = direct;
	}
}
