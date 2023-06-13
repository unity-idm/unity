/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.translation.form;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.translation.TranslationAction;
import pl.edu.icm.unity.engine.api.translation.form.RegistrationFormTranslationActionGenerator;
import pl.edu.icm.unity.engine.api.translation.form.TranslatedRegistrationRequest.AutomaticRequestAction;
import pl.edu.icm.unity.engine.translation.form.action.AddAttributeActionFactory;
import pl.edu.icm.unity.engine.translation.form.action.AddIdentityActionFactory;
import pl.edu.icm.unity.engine.translation.form.action.AddToGroupActionFactory;
import pl.edu.icm.unity.engine.translation.form.action.AutoProcessActionFactory;
import pl.edu.icm.unity.stdext.identity.IdentifierIdentity;

/**
 * Implementation of {@link RegistrationFormTranslationActionGenerator}
 * 
 * @author P.Piernik
 *
 */
@Component
public class FormTranslationActionGeneratorImpl implements RegistrationFormTranslationActionGenerator
{

	@Override
	public TranslationAction getAddAttributeAction(String attributeName, String group, String attributeValue)
	{
		return new TranslationAction(AddAttributeActionFactory.NAME,
				new String[] { attributeName, group , attributeValue });
	}

	@Override
	public TranslationAction getAutoProcessAction(AutomaticRequestAction action)
	{
		return new TranslationAction(AutoProcessActionFactory.NAME, new String[] { action.toString() });
	}

	@Override
	public TranslationAction getAddIdentifierIndentityAction(String identity)
	{
		return new TranslationAction(AddIdentityActionFactory.NAME,
				new String[] { IdentifierIdentity.ID, identity });
	}

	@Override
	public TranslationAction getAddToGroupAction(String group)
	{
		return new TranslationAction(AddToGroupActionFactory.NAME, new String[] { group });
	}

}
