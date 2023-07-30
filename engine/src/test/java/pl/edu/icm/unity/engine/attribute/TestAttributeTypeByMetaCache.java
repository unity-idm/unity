/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.attribute;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.engine.api.attributes.AttributeMetadataProvidersRegistry;
import pl.edu.icm.unity.stdext.utils.ContactEmailMetadataProvider;
import pl.edu.icm.unity.store.api.AttributeTypeDAO;

@ExtendWith(MockitoExtension.class)
public class TestAttributeTypeByMetaCache
{
	@Mock
	private AttributeTypeDAO attributeTypeDAO;
	@Mock
	private AttributeMetadataProvidersRegistry atMetaProvidersRegistry;
	
	@Test
	public void shouldGetTypeFromCache() throws EngineException
	{
		AttributeTypeByMetaCache attributeTypeByMetaCache = new AttributeTypeByMetaCache(attributeTypeDAO, atMetaProvidersRegistry);
		
		when(atMetaProvidersRegistry.getByName("contactEmail")).thenReturn(new ContactEmailMetadataProvider());
		AttributeType attributeType = new AttributeType();
		attributeType.setName("type");
		attributeType.setMetadata(Map.of(ContactEmailMetadataProvider.NAME, ""));
		
		when(attributeTypeDAO.getAll()).thenReturn(List.of(attributeType));
		
		attributeTypeByMetaCache.getAttributeTypeWithSingeltonMetadata("contactEmail");
		attributeTypeByMetaCache.getAttributeTypeWithSingeltonMetadata("contactEmail");

		verify(attributeTypeDAO, times(1)).getAll();
	}
	
	@Test
	public void shouldNotReturnATAfterClearCache() throws EngineException
	{
		AttributeTypeByMetaCache attributeTypeByMetaCache = new AttributeTypeByMetaCache(attributeTypeDAO, atMetaProvidersRegistry);
		
		when(atMetaProvidersRegistry.getByName("contactEmail")).thenReturn(new ContactEmailMetadataProvider());
		AttributeType attributeType = new AttributeType();
		attributeType.setName("type");
		attributeType.setMetadata(Map.of(ContactEmailMetadataProvider.NAME, ""));
		when(attributeTypeDAO.getAll()).thenReturn(List.of(attributeType));
		attributeTypeByMetaCache.getAttributeTypeWithSingeltonMetadata("contactEmail");
		assertThat(attributeTypeByMetaCache.getAttributeTypeWithSingeltonMetadata("contactEmail")).isEqualTo(attributeType);
	
		attributeTypeByMetaCache.clear();
		
		AttributeType updatedAttributeType = new AttributeType();
		attributeType.setName("type");		
		when(attributeTypeDAO.getAll()).thenReturn(List.of(updatedAttributeType));
		AttributeType attributeTypeWithSingeltonMetadata = attributeTypeByMetaCache.getAttributeTypeWithSingeltonMetadata("contactEmail");
		
		assertThat(attributeTypeWithSingeltonMetadata).isNull();
		
	}
	
	
}
