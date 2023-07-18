/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.translation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;


import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.translation.ProfileMode;
import pl.edu.icm.unity.base.translation.ProfileType;
import pl.edu.icm.unity.base.translation.TranslationAction;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.base.translation.TranslationRule;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.TranslationProfileManagement;
import pl.edu.icm.unity.engine.translation.in.action.MapAttributeActionFactory;

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

		TranslationProfile tp1Cfg = new TranslationProfile("sys:oidc", "",
				ProfileType.INPUT, ProfileMode.DEFAULT, rules);

		Throwable error = catchThrowable(() -> tprofMan.updateProfile(tp1Cfg));
		assertThat(error).isInstanceOf(IllegalArgumentException.class);
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

		Throwable error = catchThrowable(() -> tprofMan.addProfile(tp1Cfg));
		assertThat(error).isInstanceOf(IllegalArgumentException.class);
	}
	
	@Test
	public void shouldListSystemProfilesFromAllProviders() throws Exception
	{
		assertThat(tprofMan.listInputProfiles()).isNotEmpty();
		assertThat(tprofMan.listOutputProfiles()).isNotEmpty();	
	}
	
	@Test
	public void shouldGetSystemProfile() throws Exception
	{
		assertThat(tprofMan.getInputProfile("sys:oidc")).isNotNull();
		assertThat(tprofMan.getOutputProfile("sys:oidc")).isNotNull();
	}
}
