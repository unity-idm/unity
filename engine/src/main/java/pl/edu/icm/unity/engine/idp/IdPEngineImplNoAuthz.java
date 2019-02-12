/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.idp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.translation.out.OutputTranslationActionsRegistry;
import pl.edu.icm.unity.engine.api.userimport.UserImportSerivce;
import pl.edu.icm.unity.engine.attribute.AttributeValueConverter;
import pl.edu.icm.unity.engine.translation.out.OutputTranslationEngine;
import pl.edu.icm.unity.engine.translation.out.OutputTranslationProfileRepository;

/**
 * Same as {@link IdPEngineImpl}, with a sole but very important difference: this variant 
 * is using authorization-less attribute and identities managers, therefore allowing the user of 
 * this class to skip authorization when resolving user information. 
 * <p>
 * This class in general should not be used unless in rare cases when authorization is provided
 * by the client code. 
 * 
 * @author K. Benedyczak
 */
@Component
@Qualifier("insecure")
public class IdPEngineImplNoAuthz extends IdPEngineImplBase
{
	@Autowired
	public IdPEngineImplNoAuthz(@Qualifier("insecure") AttributesManagement attributesMan, 
			@Qualifier("insecure") EntityManagement identitiesMan,
			OutputTranslationProfileRepository outputProfileRepo,
			OutputTranslationEngine translationEngine,
			UserImportSerivce userImportService,
			OutputTranslationActionsRegistry actionsRegistry,
			AttributeValueConverter attrValueConverter,
			UnityMessageSource msg)
	{
		super(attributesMan, attributesMan, identitiesMan, userImportService, 
				new OutputProfileExecutor(outputProfileRepo, 
						translationEngine, actionsRegistry, 
						attrValueConverter, msg));
	}
}
