/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.tprofile;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.store.api.generic.GenericObjectsDAO;
import pl.edu.icm.unity.store.api.generic.TranslationProfileDB;
import pl.edu.icm.unity.store.objstore.AbstractObjStoreTest;
import pl.edu.icm.unity.types.translation.ProfileType;
import pl.edu.icm.unity.types.translation.TranslationAction;
import pl.edu.icm.unity.types.translation.TranslationProfile;
import pl.edu.icm.unity.types.translation.TranslationRule;

public class TranslationProfileTest extends AbstractObjStoreTest<TranslationProfile>
{
	@Autowired
	private TranslationProfileDB dao;
	
	@Override
	protected GenericObjectsDAO<TranslationProfile> getDAO()
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
		return new TranslationProfile("name-changed", "description2", ProfileType.OUTPUT, rules);
	}

	@Override
	protected void assertAreEqual(TranslationProfile obj, TranslationProfile cmp)
	{
		assertThat(obj, is(cmp));
	}
}
