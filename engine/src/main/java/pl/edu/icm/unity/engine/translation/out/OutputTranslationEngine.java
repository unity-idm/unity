/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation.out;

import java.util.Collection;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeDefinition;
import pl.edu.icm.unity.engine.api.identity.IdentityTypesRegistry;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.translation.out.TranslationInput;
import pl.edu.icm.unity.engine.api.translation.out.TranslationResult;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.DynamicAttribute;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;

/**
 * Applies all mappings which were recorded by profile's actions.
 * <p>
 * Important: the instance is running without authorization, the object can not be exposed to direct operation.
 * @author K. Benedyczak
 */
@Component
public class OutputTranslationEngine
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_TRANSLATION, OutputTranslationEngine.class);
	private EntityManagement idsMan;
	private AttributesManagement attrMan;
	private IdentityTypesRegistry idTypesReg;
	private AttributeTypeSupport atSupport;
	private UnityMessageSource msg;
	
	@Autowired
	public OutputTranslationEngine(@Qualifier("insecure") EntityManagement idsMan, 
			@Qualifier("insecure") AttributesManagement attrMan, IdentityTypesRegistry idTypesReg,
			AttributeTypeSupport atSupport,
			UnityMessageSource msg)
	{
		this.idsMan = idsMan;
		this.attrMan = attrMan;
		this.idTypesReg = idTypesReg;
		this.atSupport = atSupport;
		this.msg = msg;
	}

	
	/**
	 * Entry point.
	 * @param result
	 * @throws EngineException
	 */
	public void process(TranslationInput input, TranslationResult result) throws EngineException
	{
		persistIdentities(input, result);
		persistAttributes(input, result);
		resolveDynamicAttributes(result);
	}
	
	private void persistIdentities(TranslationInput input, TranslationResult result)
			throws EngineException
	{
		EntityParam parent = new EntityParam(input.getEntity().getId());
		Collection<IdentityParam> ids = result.getIdentitiesToPersist();
		boolean skip = false;
		for (IdentityParam id : ids)
		{
			skip = false;
			Entity entity = idsMan.getEntity(parent);
			for (Identity iden : entity.getIdentities())
			{			
				skip = checkEqualIds(iden, id);
			}
			
			if (!skip)
			{
				log.debug("Adding identity: " + id);
				idsMan.addIdentity(id, parent, false);
			} else
			{
				log.debug("Identity: " + id + " already exist, skip add");
			}
		}
	}
	
	private boolean checkEqualIds(Identity id1, IdentityParam id)
			throws IllegalIdentityValueException
	{
		if (id1.getTypeId().equals(id.getTypeId()))
		{
			IdentityTypeDefinition idType = idTypesReg.getByName(id1.getTypeId());

			String cValue1 = idType.getComparableValue(id.getValue(), null, null);
			String cValue2 = idType.getComparableValue(id1.getValue(), null, null);
			if (cValue1.equals(cValue2))
			{
				return true;
			}
		}
		return false;
	}
	
	private void persistAttributes(TranslationInput input, TranslationResult result) throws EngineException
	{
		EntityParam parent = new EntityParam(input.getEntity().getId());
		
		for (Attribute a: result.getAttributesToPersist())
		{
			log.debug("Adding attribute: " + a + " for " + parent);
			attrMan.setAttribute(parent, a);
		}
	}
	
	private void resolveDynamicAttributes(TranslationResult result)
	{
		for (DynamicAttribute attr: result.getAttributes())
			resolveAttribute(attr);
	}
	
	private void resolveAttribute(DynamicAttribute dat)
	{
		Attribute at = dat.getAttribute();
		AttributeType attributeType = dat.getAttributeType();
		if (attributeType == null)
		{
			try
			{
				attributeType = atSupport.getType(at);
			} catch (IllegalArgumentException e)
			{
				// can happen for dynamic attributes from output translation profile
				attributeType = new AttributeType(at.getName(), StringAttributeSyntax.ID);
			}
		}
		String name = getAttributeDisplayedName(dat, attributeType);
		String desc = getAttributeDescription(dat, attributeType);
		dat.setDescription(desc);
		dat.setDisplayedName(name);
		dat.setAttributeType(attributeType);
	}

	private String getAttributeDescription(DynamicAttribute dat, AttributeType attributeType)
	{
		String attrDescription = dat.getDescription();
		if (attrDescription == null || attrDescription.isEmpty())
		{
			attrDescription = attributeType.getDescription() != null
					? attributeType.getDescription().getValue(msg)
					: dat.getAttribute().getName();
		}
		
		return attrDescription;
	}

	private String getAttributeDisplayedName(DynamicAttribute dat, AttributeType attributeType)
	{
		String attrDisplayedName = dat.getDisplayedName();
		if (attrDisplayedName == null || attrDisplayedName.isEmpty())
		{
			attrDisplayedName = attributeType.getDisplayedName() != null
					? attributeType.getDisplayedName().getValue(msg)
					: dat.getAttribute().getName();
		}
		
		return attrDisplayedName;
	}
}
