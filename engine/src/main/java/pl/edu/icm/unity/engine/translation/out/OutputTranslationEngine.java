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
import pl.edu.icm.unity.engine.api.identity.IdentityTypeDefinition;
import pl.edu.icm.unity.engine.api.identity.IdentityTypesRegistry;
import pl.edu.icm.unity.engine.api.translation.out.TranslationInput;
import pl.edu.icm.unity.engine.api.translation.out.TranslationResult;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.Attribute;
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
	
	@Autowired
	public OutputTranslationEngine(@Qualifier("insecure") EntityManagement idsMan, 
			@Qualifier("insecure") AttributesManagement attrMan, IdentityTypesRegistry idTypesReg)
	{
		this.idsMan = idsMan;
		this.attrMan = attrMan;
		this.idTypesReg = idTypesReg;
	}

	
	/**
	 * Entry point.
	 * @param result
	 * @throws EngineException
	 */
	public void process(TranslationInput input, TranslationResult result) throws EngineException
	{
		processIdentities(input, result);
		processAttributes(input, result);
	}
	
	private void processIdentities(TranslationInput input, TranslationResult result)
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
				if (iden.getTypeId().equals(id.getTypeId()))
				{
					IdentityTypeDefinition idType = idTypesReg
							.getByName(iden.getTypeId());

					String cValue1 = idType.getComparableValue(id.getValue(),
							null, null);
					String cValue2 = idType.getComparableValue(iden.getValue(),
							null, null);
					if (cValue1.equals(cValue2))
					{
						skip = true;
					}
				}
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
	
	private void processAttributes(TranslationInput input, TranslationResult result) throws EngineException
	{
		EntityParam parent = new EntityParam(input.getEntity().getId());
		
		for (Attribute a: result.getAttributesToPersist())
		{
			log.debug("Adding attribute: " + a + " for " + parent);
			attrMan.setAttribute(parent, a, true);
		}
	}
}
