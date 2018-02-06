/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static pl.edu.icm.unity.engine.userimport.UserImportProperties.PREFIX;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.junit.Test;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteAuthnResultProcessor;
import pl.edu.icm.unity.engine.api.config.ConfigurationLoader;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.userimport.UserImportSPI;
import pl.edu.icm.unity.engine.api.userimport.UserImportSPIFactory;
import pl.edu.icm.unity.engine.api.userimport.UserImportSerivce.ImportResult;
import pl.edu.icm.unity.engine.api.userimport.UserImportSpec;
import pl.edu.icm.unity.engine.api.utils.CacheProvider;
import pl.edu.icm.unity.engine.userimport.UserImportProperties;
import pl.edu.icm.unity.engine.userimport.UserImportServiceImpl;

public class TestUserImportService
{
	@Test
	public void importSerivceLoadsConfiguredHandlerAndInvokesIt() throws InterruptedException
	{
		//given
		RemoteAuthnResultProcessor verificatorUtil = mock(RemoteAuthnResultProcessor.class);

		UnityServerConfiguration mainCfg = mock(UnityServerConfiguration.class);
		when(mainCfg.getSortedListKeys(UnityServerConfiguration.IMPORT_PFX))
			.thenReturn(Lists.newArrayList("key"));
		when(mainCfg.getValue(UnityServerConfiguration.IMPORT_PFX + "key"))
			.thenReturn("f");
		ConfigurationLoader cfgLoader = mock(ConfigurationLoader.class);
		when(cfgLoader.getProperties("f")).thenReturn(getCfgProperties());

		UserImportSPIFactory factory = mock(UserImportSPIFactory.class);
		UserImportSPI importer = mock(UserImportSPI.class);
		when(importer.importUser("id", "type")).thenReturn(null);		
		when(factory.getInstance(any(), any())).thenReturn(importer);
		when(factory.getName()).thenReturn("mockI");

		List<UserImportSPIFactory> importersF = new ArrayList<>();
		importersF.add(factory);
		CacheProvider cp = new CacheProvider();
		
		//when
		UserImportServiceImpl impl = new UserImportServiceImpl(mainCfg, importersF, 
				cp, verificatorUtil, cfgLoader);
		List<ImportResult> importUser = impl.importUser(
				Lists.newArrayList(new UserImportSpec("key", "id", "type")));

		//then
		assertThat(importUser.size(), is(1));
		assertThat(importUser.get(0).authenticationResult.getStatus(), is(Status.notApplicable));
		assertThat(importUser.get(0).importerKey, is("key"));
		verify(factory).getInstance(getCfgProperties(), "idp");
		verify(importer).importUser("id", "type");
		
		//again - should cache negative resolve
		reset(importer); //just to have nice 'never'below
		List<ImportResult> importUser2 = impl.importUser(
				Lists.newArrayList(new UserImportSpec("key", "id", "type")));
		assertThat(importUser2.size(), is(1));
		assertThat(importUser2.get(0).authenticationResult.getStatus(), is(Status.notApplicable));
		verify(importer, never()).importUser("id", "type");
		
		//again - should expire negative resolve cache
		Thread.sleep(1001);
		List<ImportResult> importUser3 = impl.importUser(
				Lists.newArrayList(new UserImportSpec("key", "id", "type")));
		assertThat(importUser3.size(), is(1));
		assertThat(importUser3.get(0).authenticationResult.getStatus(), is(Status.notApplicable));
		verify(importer).importUser("id", "type");
	}
	
	@Test
	public void importSerivceInvokesAllImporters() throws InterruptedException
	{
		//given
		RemoteAuthnResultProcessor verificatorUtil = mock(RemoteAuthnResultProcessor.class);

		UnityServerConfiguration mainCfg = mock(UnityServerConfiguration.class);
		when(mainCfg.getSortedListKeys(UnityServerConfiguration.IMPORT_PFX))
			.thenReturn(Lists.newArrayList("key1", "key2"));
		when(mainCfg.getValue(UnityServerConfiguration.IMPORT_PFX + "key1"))
			.thenReturn("f1");
		when(mainCfg.getValue(UnityServerConfiguration.IMPORT_PFX + "key2"))
			.thenReturn("f2");
		ConfigurationLoader cfgLoader = mock(ConfigurationLoader.class);
		when(cfgLoader.getProperties("f1")).thenReturn(getCfgProperties());
		when(cfgLoader.getProperties("f2")).thenReturn(getCfgProperties());

		UserImportSPIFactory factory = mock(UserImportSPIFactory.class);
		UserImportSPI importer = mock(UserImportSPI.class);
		when(importer.importUser("id", "type")).thenReturn(null);		
		when(factory.getInstance(any(), any())).thenReturn(importer);
		when(factory.getName()).thenReturn("mockI");

		List<UserImportSPIFactory> importersF = new ArrayList<>();
		importersF.add(factory);
		CacheProvider cp = new CacheProvider();
		
		//when
		UserImportServiceImpl impl = new UserImportServiceImpl(mainCfg, importersF, 
				cp, verificatorUtil, cfgLoader);
		List<ImportResult> importUser = impl.importUser(
				Lists.newArrayList(UserImportSpec.withAllImporters("id", "type")));

		//then
		assertThat(importUser.size(), is(2));
		assertThat(importUser.get(0).importerKey, is("key1"));
		assertThat(importUser.get(0).authenticationResult.getStatus(), 
				is(Status.notApplicable));
		assertThat(importUser.get(1).importerKey, is("key2"));
		verify(factory, times(2)).getInstance(getCfgProperties(), "idp");
		verify(importer, times(2)).importUser("id", "type");
	}

	
	private Properties getCfgProperties()
	{
		Properties ret = new Properties();
		ret.setProperty(PREFIX + UserImportProperties.TYPE, "mockI");
		ret.setProperty(PREFIX + UserImportProperties.REMOTE_IDP_NAME, "idp");
		ret.setProperty(PREFIX + UserImportProperties.TRANSLATION_PROFILE, "tp");
		ret.setProperty(PREFIX + UserImportProperties.NEGATIVE_CACHE, "1");
		ret.setProperty(PREFIX + UserImportProperties.POSITIVE_CACHE, "1");
		return ret;
	}
}
