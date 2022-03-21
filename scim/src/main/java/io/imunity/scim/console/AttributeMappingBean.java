/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.console;

import java.util.Optional;

import io.imunity.scim.config.AttributeMapping;
import io.imunity.scim.config.ComplexAttributeMapping;
import io.imunity.scim.config.DataArray;
import io.imunity.scim.config.DataValue;
import io.imunity.scim.config.NotDefinedMapping;
import io.imunity.scim.config.ReferenceAttributeMapping;
import io.imunity.scim.config.SimpleAttributeMapping;
import io.imunity.scim.schema.SCIMAttributeType;

public class AttributeMappingBean
{
	private DataValueBean dataValue;
	private DataArrayBean dataArray;
	private ReferenceDataBean dataReference;

	public AttributeMappingBean()
	{
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

	AttributeMapping toConfiguration(AttributeDefinitionBean attributeDefinition)
	{
		if (attributeDefinition.getType().equals(SCIMAttributeType.COMPLEX))
		{
			return ComplexAttributeMapping.builder()
					.withDataArray(!attributeDefinition.isMultiValued() ? Optional.empty() : mapDataArray(dataArray))
					.build();
		} else if (attributeDefinition.getType().equals(SCIMAttributeType.REFERENCE))
		{
			if (dataReference == null)
				return new NotDefinedMapping();

			return ReferenceAttributeMapping.builder()
					.withDataArray(!attributeDefinition.isMultiValued() ? Optional.empty() : mapDataArray(dataArray))
					.withExpression(dataReference.getExpression()).withType(dataReference.getType()).build();

		}

		if (dataValue == null)
		{
			return new NotDefinedMapping();
		}

		return SimpleAttributeMapping.builder()
				.withDataArray(!attributeDefinition.isMultiValued() ? Optional.empty() : mapDataArray(dataArray))
				.withDataValue(DataValue.builder().withType(dataValue.getType())
						.withValue(dataValue.getValue().orElse(null)).build())
				.build();

	}

	private static Optional<DataArray> mapDataArray(DataArrayBean bean)
	{
		return bean == null || bean.getType() == null ? Optional.empty()
				: Optional.of(
						DataArray.builder().withType(bean.getType()).withValue(bean.getValue().orElse(null)).build());
	}

}
