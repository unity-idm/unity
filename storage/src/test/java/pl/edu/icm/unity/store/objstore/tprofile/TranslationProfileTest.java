/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.tprofile;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.base.translation.ProfileType;
import pl.edu.icm.unity.base.translation.TranslationAction;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.base.translation.TranslationRule;
import pl.edu.icm.unity.store.api.generic.InputTranslationProfileDB;
import pl.edu.icm.unity.store.api.generic.NamedCRUDDAOWithTS;
import pl.edu.icm.unity.store.objstore.AbstractNamedWithTSTest;

public class TranslationProfileTest extends AbstractNamedWithTSTest<TranslationProfile>
{
	@Autowired
	private InputTranslationProfileDB dao;
	
	@Override
	protected NamedCRUDDAOWithTS<TranslationProfile> getDAO()
	{
		return dao;
	}

	@Override
	protected TranslationProfile getObject(String id)
	{
		List<TranslationRule> rules = Lists.newArrayList(
				new TranslationRule("condition", new TranslationAction("action", new String[] {"p1"})),
				new TranslationRule("condition2", new TranslationAction("action2", new String[] {})));
		return new TranslationProfile(id, "description", ProfileType.INPUT, rules);
	}

	@Override
	protected TranslationProfile mutateObject(TranslationProfile src)
	{
		List<TranslationRule> rules = Lists.newArrayList(
				new TranslationRule("condition3", new TranslationAction("action3", 
						new String[] {"p3", "p4"})));
		return new TranslationProfile("name-changed", "description2", ProfileType.INPUT, rules);
		// it is not possible to change subtype, so profile type must be left intact.
	}
}
