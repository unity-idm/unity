/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.idp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.AttributeValueConverter;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.translation.out.OutputTranslationActionsRegistry;
import pl.edu.icm.unity.engine.api.userimport.UserImportSerivce;
import pl.edu.icm.unity.engine.translation.out.OutputTranslationEngine;
import pl.edu.icm.unity.engine.translation.out.OutputTranslationProfileRepository;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupContents;

/**
 * IdP engine is responsible for performing common IdP-related functionality. It resolves the information
 * about the user being queried, applies translation profile on the data and exposes it to the endpoint 
 * requiring the data.
 * 
 * @author K. Benedyczak
 */
@Component
@Primary
public class IdPEngineImpl extends IdPEngineImplBase
{
	@Autowired
	public IdPEngineImpl(AttributesManagement attributesMan, 
			@Qualifier("insecure") AttributesManagement insecureAttributesMan,
			@Qualifier("insecure") GroupsManagement groupMan,
			EntityManagement identitiesMan,
			OutputTranslationProfileRepository outputProfileRepo,
			OutputTranslationEngine translationEngine,
			UserImportSerivce userImportService,
			OutputTranslationActionsRegistry actionsRegistry,
			AttributeValueConverter attrValueConverter,
			MessageSource msg)
	{
		super(attributesMan, insecureAttributesMan, identitiesMan, userImportService, 
				new OutputProfileExecutor(outputProfileRepo, 
						translationEngine, actionsRegistry, 
						attrValueConverter, msg, g -> getGroup(groupMan, g)), groupMan);
	}	
	
	
	public static Group getGroup(GroupsManagement groupMan, String g)
	{
		try
		{
			return groupMan.getContents(g, GroupContents.METADATA).getGroup();
		} catch (EngineException e)
		{
			return new Group(g);
		}
	}
}
