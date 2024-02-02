/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.idp.console.v8;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.saml.metadata.cfg.MetaToConfigConverterHelper;
import pl.edu.icm.unity.webui.common.grid.FilterableEntry;
import xmlbeans.org.oasis.saml2.metadata.EntitiesDescriptorType;
import xmlbeans.org.oasis.saml2.metadata.EntityDescriptorType;
import xmlbeans.org.oasis.saml2.metadata.SPSSODescriptorType;
import xmlbeans.org.oasis.saml2.metadata.extui.UIInfoType;

/**
 * Converts SAML IDP metadata to list of {@link SAMLEntity} It is
 * convenient to simple read metadata - without additional checks
 * 
 * @author P.Piernik
 *
 */
class SimpleIDPMetaConverter
{
	private MessageSource msg;

	SimpleIDPMetaConverter(MessageSource msg)
	{
		this.msg = msg;
	}

	List<SAMLEntity> getEntries(EntitiesDescriptorType meta)
	{

		List<SAMLEntity> ret = new ArrayList<>();
		EntitiesDescriptorType[] nested = meta.getEntitiesDescriptorArray();
		if (nested != null)
		{
			for (EntitiesDescriptorType nestedD : nested)
				ret.addAll(getEntries(nestedD));
		}
		EntityDescriptorType[] entities = meta.getEntityDescriptorArray();

		if (entities != null)
		{
			for (EntityDescriptorType entity : entities)
			{
				ret.addAll(getEntries(entity));
			}
		}
		return ret;
	}

	private List<SAMLEntity> getEntries(EntityDescriptorType meta)
	{
		List<SAMLEntity> ret = new ArrayList<>();

		SPSSODescriptorType[] spDefs = meta.getSPSSODescriptorArray();
		for (SPSSODescriptorType spDef : spDefs)
		{
			if (!MetaToConfigConverterHelper.supportsSaml2(spDef))
			{
				continue;
			}
		
			UIInfoType uiInfo = MetaToConfigConverterHelper.parseMDUIInfo(spDef.getExtensions(),
					meta.getEntityID());
			Map<String, String> names = MetaToConfigConverterHelper.getLocalizedNames(msg, uiInfo, spDef,
					meta);

			String name = null;
			if (!names.isEmpty())
			{
				name = names.get(msg.getLocaleCode());
				if (name == null)
				{
					name = names.get(msg.getDefaultLocaleCode());
				}

				if (name == null)
				{
					name = names.values().iterator().next();
				}
			}
			ret.add(new SAMLEntity(meta.getEntityID(), name));
		}

		return ret;

	}

	public static class SAMLEntity implements FilterableEntry
	{
		public final String id;
		public final String name;

		SAMLEntity(String id, String name)
		{
			this.id = id;
			this.name = name;
		}

		@Override
		public boolean anyFieldContains(String searched, MessageSource msg)
		{
			String textLower = searched.toLowerCase();

			if (name != null && name.toLowerCase().contains(textLower))
				return true;

			if (id != null && id.toLowerCase().contains(textLower))
				return true;

			return false;
		}

	}

}
