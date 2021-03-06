/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.directorySetup.attributeTypes;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.vaadin.simplefiledownloader.SimpleFileDownloader;

import com.vaadin.server.StreamResource;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.attrmetadata.AttributeMetadataHandlerRegistry;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Controller for all attribute type views
 * 
 * @author P.Piernik
 *
 */
@Component
class AttributeTypeController
{
	private MessageSource msg;
	private AttributeTypeManagement attrTypeMan;
	private AttributeHandlerRegistry attrHandlerRegistry;
	private AttributeMetadataHandlerRegistry attrMetaHandlerRegistry;
	private AttributeTypeSupport atSupport;
	private UnityServerConfiguration serverConfig;

	@Autowired
	AttributeTypeController(MessageSource msg, AttributeTypeManagement attrTypeMan,
			AttributeHandlerRegistry attrHandlerRegistry,
			AttributeMetadataHandlerRegistry attrMetaHandlerRegistry, AttributeTypeSupport atSupport,
			UnityServerConfiguration serverConfig)
	{
		this.msg = msg;
		this.attrTypeMan = attrTypeMan;
		this.attrHandlerRegistry = attrHandlerRegistry;
		this.attrMetaHandlerRegistry = attrMetaHandlerRegistry;
		this.atSupport = atSupport;
		this.serverConfig = serverConfig;
	}

	Collection<AttributeTypeEntry> getAttributeTypes() throws ControllerException
	{
		try
		{
			return attrTypeMan.getAttributeTypes().stream().map(at -> new AttributeTypeEntry(msg, at))
					.collect(Collectors.toList());
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("AttributeTypeController.getAllError"), e);
		}
	}

	SimpleFileDownloader getAttributeTypesDownloader(Set<AttributeTypeEntry> items) throws ControllerException
	{
		SimpleFileDownloader downloader = new SimpleFileDownloader();
		StreamResource resource = null;
		try
		{
			if (items.size() == 1)
			{
				AttributeType item = items.iterator().next().attributeType;
				byte[] content = Constants.MAPPER.writeValueAsBytes(item);
				resource = new StreamResource(() -> new ByteArrayInputStream(content),
						item.getName() + ".json");
			} else
			{

				byte[] content = Constants.MAPPER.writeValueAsBytes(
						items.stream().map(at -> at.attributeType).collect(Collectors.toSet()));
				resource = new StreamResource(() -> new ByteArrayInputStream(content),
						"attributeTypes.json");
			}
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("AttributeTypeController.getDownloaderError"), e);
		}

		downloader.setFileDownloadResource(resource);
		return downloader;
	}

	void addAttributeType(AttributeType at) throws ControllerException
	{
		try
		{
			attrTypeMan.addAttributeType(at);
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("AttributeTypeController.addError", at.getName()),
					e);
		}

	}

	void updateAttributeType(AttributeType at) throws ControllerException
	{
		try
		{
			attrTypeMan.updateAttributeType(at);
		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("AttributeTypeController.updateError", at.getName()), e);
		}

	}

	AttributeType getAttributeType(String attributeTypeName) throws ControllerException
	{
		try
		{
			return attrTypeMan.getAttributeType(attributeTypeName);
		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("AttributeTypeController.getError", attributeTypeName), e);
		}
	}

	void removeAttributeTypes(Set<AttributeType> items, boolean deleteInstances) throws ControllerException
	{
		List<String> removed = new ArrayList<>();
		try
		{
			for (AttributeType toRemove : items)
			{
				attrTypeMan.removeAttributeType(toRemove.getName(), deleteInstances);
				removed.add(toRemove.getName());
			}
		} catch (Exception e)
		{
			if (removed.isEmpty())
			{
				throw new ControllerException(msg.getMessage("AttributeTypeController.removeError"), e);
			} else
			{
				throw new ControllerException(msg.getMessage("AttributeTypeController.removeError"),
						msg.getMessage("AttributeTypeController.partiallyRemoved", removed), e);
			}
		}

	}

	void mergeAttributeTypes(Set<AttributeType> toMerge, boolean overwrite) throws ControllerException
	{
		try
		{
			Set<String> existing = attrTypeMan.getAttributeTypesAsMap().keySet();
			for (AttributeType at : toMerge)
			{

				if (!existing.contains(at.getName()))
				{
					attrTypeMan.addAttributeType(at);
				} else if (overwrite)
				{
					attrTypeMan.updateAttributeType(at);
				}
			}
		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("AttributeTypeController.mergeAttributeTypesError"), e);
		}
	}

	ImportAttributeTypeEditor getImportEditor() throws ControllerException
	{
		try
		{
			return new ImportAttributeTypeEditor(msg, getAttributeTypes().stream().map(a -> a.attributeType)
					.collect(Collectors.toSet()), serverConfig, atSupport);
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("AttributeTypeController.getImportEditorError"),
					e);
		}
	}

	AttributeTypeEditor getEditor(AttributeType attributeType)
	{

		return attributeType == null
				? new RegularAttributeTypeEditor(msg, attrHandlerRegistry, attributeType,
						attrMetaHandlerRegistry, atSupport)
				: attributeType.isTypeImmutable() ? new ImmutableAttributeTypeEditor(msg, attributeType)
						: new RegularAttributeTypeEditor(msg, attrHandlerRegistry,
								attributeType, attrMetaHandlerRegistry, atSupport);
	}

	RegularAttributeTypeEditor getRegularAttributeTypeEditor(AttributeType attributeType)
	{

		return new RegularAttributeTypeEditor(msg, attrHandlerRegistry, attributeType, attrMetaHandlerRegistry,
				atSupport);

	}
}
