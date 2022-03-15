/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.console;

public class AttributeMappingBean
{
	private DataValueBean dataValue;
	private DataArrayBean dataArray;
	private ReferenceDataBean dataReference;

	public AttributeMappingBean()
	{
		dataReference = null;//new ReferenceDataBean();
		dataValue = null;//new DataValueBean();
		dataArray = null; //new DataArrayBean();
	}

	public DataValueBean getDataValue()
	{
		return dataValue;
	}

	public void setDataValue(DataValueBean dataValue)
	{
		this.dataValue = dataValue;
	}

	public DataArrayBean getDataArray()
	{
		return dataArray;
	}

	public void setDataArray(DataArrayBean dataArray)
	{
		this.dataArray = dataArray;
	}

	public ReferenceDataBean getDataReference()
	{
		return dataReference;
	}

	public void setDataReference(ReferenceDataBean dataReference)
	{
		this.dataReference = dataReference;
	}

	@Override
	protected AttributeMappingBean clone()
	{
		AttributeMappingBean clone = new AttributeMappingBean();
		clone.setDataArray(dataArray);
		clone.setDataValue(dataValue);
		clone.setDataReference(dataReference);
		return clone;
	}
}
