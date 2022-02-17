/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.console;

//TODO
public class AttributeMappingBean
{
	private DataValue dataValue;
	private DataArray dataArray;

	public AttributeMappingBean()
	{
		
	}
	
	
	public static class DataValue
	{
		public enum DataValueType
		{
			ATTRIBUTE, IDENTITIE, MEMBERSHIP, REFERENCE, ARRAY, MVEL
		}

	}

	public static class DataArray
	{
		public enum DataArrayType
		{
			ATTRIBUTES, IDENTITIES, MEMBERSHIPS
		}

		private DataArrayType type;
		private String value;
	}
}
