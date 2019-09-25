/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.idp.console;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.saml.metadata.cfg.MetaToConfigConverterHelper;
import pl.edu.icm.unity.webui.common.grid.FilterableEntry;
import xmlbeans.org.oasis.saml2.metadata.EntitiesDescriptorType;
import xmlbeans.org.oasis.saml2.metadata.EntityDescriptorType;
import xmlbeans.org.oasis.saml2.metadata.SPSSODescriptorType;
import xmlbeans.org.oasis.saml2.metadata.extui.LogoType;
import xmlbeans.org.oasis.saml2.metadata.extui.UIInfoType;

/**
 * Converts SAML IDP metadata to list of {@link SAMLEntityWithLogo} It is
 * convenient to simple read metadata - without additional checks
 * 
 * @author P.Piernik
 *
 */
class SimpleIDPMetaConverter
{
	private UnityMessageSource msg;

	SimpleIDPMetaConverter(UnityMessageSource msg)
	{
		this.msg = msg;
	}

	List<SAMLEntityWithLogo> getEntries(EntitiesDescriptorType meta)
	{

		List<SAMLEntityWithLogo> ret = new ArrayList<>();
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

	private List<SAMLEntityWithLogo> getEntries(EntityDescriptorType meta)
	{
		List<SAMLEntityWithLogo> ret = new ArrayList<>();

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
			Map<String, LogoType> logos = MetaToConfigConverterHelper.getLocalizedLogos(uiInfo);

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
			String logo = null;
			if (!logos.isEmpty())
			{
				logo = logos.get(msg.getLocaleCode()) != null
						? logos.get(msg.getLocaleCode()).getStringValue()
						: null;
				if (logo == null)
				{
					logo = logos.get(msg.getDefaultLocaleCode()) != null
							? logos.get(msg.getDefaultLocaleCode()).getStringValue()
							: null;
				}

				if (logo == null)
				{
					logo = logos.values().iterator().next().getStringValue();
				}
			}
			ret.add(new SAMLEntityWithLogo(meta.getEntityID(), name, logo));
		}

		return ret;

	}

	public static class SAMLEntityWithLogo implements FilterableEntry
	{
		public final String id;
		public final String name;
		public final String logo;

		SAMLEntityWithLogo(String id, String name, String logo)
		{
			this.id = id;
			this.name = name;
			this.logo = logo;
		}

		@Override
		public boolean anyFieldContains(String searched, UnityMessageSource msg)
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
