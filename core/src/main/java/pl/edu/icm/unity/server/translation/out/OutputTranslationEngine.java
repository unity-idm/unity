/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation.out;

import java.util.Collection;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.EntityParam;
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
	private IdentitiesManagement idsMan;
	private AttributesManagement attrMan;
	
	@Autowired
	public OutputTranslationEngine(@Qualifier("insecure") IdentitiesManagement idsMan, 
			@Qualifier("insecure") AttributesManagement attrMan)
	{
		this.idsMan = idsMan;
		this.attrMan = attrMan;
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
	
	private void processIdentities(TranslationInput input, TranslationResult result) throws EngineException
	{
		EntityParam parent = new EntityParam(input.getEntity().getId());
		Collection<IdentityParam> ids = result.getIdentitiesToPersist();
		for (IdentityParam id: ids)
		{
			log.debug("Adding identity: " + id);
			idsMan.addIdentity(id, parent, false);
		}
	}
	
	private void processAttributes(TranslationInput input, TranslationResult result) throws EngineException
	{
		EntityParam parent = new EntityParam(input.getEntity().getId());
		
		for (Attribute<?> a: result.getAttributesToPersist())
		{
			log.debug("Adding attribute: " + a + " for " + parent);
			attrMan.setAttribute(parent, a, true);
		}
	}
}
