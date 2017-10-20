/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation;

import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.TranslationProfileManagement;
import pl.edu.icm.unity.engine.translation.in.action.MapAttributeActionFactory;
import pl.edu.icm.unity.types.translation.ProfileMode;
import pl.edu.icm.unity.types.translation.ProfileType;
import pl.edu.icm.unity.types.translation.TranslationAction;
import pl.edu.icm.unity.types.translation.TranslationProfile;
import pl.edu.icm.unity.types.translation.TranslationRule;

public class TestSystemTranslationProfiles extends DBIntegrationTestBase
{
	@Autowired
	private TranslationProfileManagement tprofMan;

	
	@Test
	public void shouldNotUpdateSystemProfile() throws Exception
	{
		List<TranslationRule> rules = new ArrayList<>();
		TranslationAction action = (TranslationAction) new TranslationAction(
				MapAttributeActionFactory.NAME,
				new String[] { "test", "/", "'val'", "CREATE_OR_UPDATE" });
		rules.add(new TranslationRule("true", action));

		TranslationProfile tp1Cfg = new TranslationProfile("sys:test", "",
				ProfileType.INPUT, ProfileMode.DEFAULT, rules);

		catchException(tprofMan).updateProfile(tp1Cfg);
		assertThat(caughtException(), isA(IllegalArgumentException.class));
	}
	
	@Test
	public void shouldNotAddReadOnlyProfile() throws Exception
	{
		List<TranslationRule> rules = new ArrayList<>();
		TranslationAction action = (TranslationAction) new TranslationAction(
				MapAttributeActionFactory.NAME,
				new String[] { "test", "/", "'val'", "CREATE_OR_UPDATE" });
		rules.add(new TranslationRule("true", action));

		TranslationProfile tp1Cfg = new TranslationProfile("demo", "",
				ProfileType.INPUT, ProfileMode.READ_ONLY, rules);

		catchException(tprofMan).addProfile(tp1Cfg);
		assertThat(caughtException(), isA(IllegalArgumentException.class));
	}
	
	@Test
	public void shouldListSystemProfiles() throws Exception
	{
		assertThat(tprofMan.listInputProfiles().size(), is(1));
		assertThat(tprofMan.listOutputProfiles().size(), is(1));		
	}
	
	@Test
	public void shouldGetSystemProfile() throws Exception
	{
		assertThat(tprofMan.getInputProfile("sys:test").getRules().size(), is(2));
		assertThat(tprofMan.getOutputProfile("sys:test").getRules().size(), is(1));		
	}
	
	
}
