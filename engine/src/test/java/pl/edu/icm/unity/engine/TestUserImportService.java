/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static pl.edu.icm.unity.engine.userimport.UserImportProperties.PREFIX;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.engine.userimport.UserImportProperties;
import pl.edu.icm.unity.engine.userimport.UserImportServiceImpl;
import pl.edu.icm.unity.server.api.userimport.UserImportSPI;
import pl.edu.icm.unity.server.api.userimport.UserImportSPIFactory;
import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.server.authn.remote.RemoteVerificatorUtil;
import pl.edu.icm.unity.server.utils.CacheProvider;
import pl.edu.icm.unity.server.utils.ConfigurationLoader;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;

public class TestUserImportService extends SecuredDBIntegrationTestBase
{
	@Autowired
	private CacheProvider cp;
	
	@Test
	public void importSerivceLoadsConfiguredHandlerAndInvokesIt() throws InterruptedException
	{
		//given
		RemoteVerificatorUtil verificatorUtil = mock(RemoteVerificatorUtil.class);

		UnityServerConfiguration mainCfg = mock(UnityServerConfiguration.class);
		when(mainCfg.getListOfValues(UnityServerConfiguration.IMPORT_PFX)).thenReturn(Lists.newArrayList("f"));

		ConfigurationLoader cfgLoader = mock(ConfigurationLoader.class);
		when(cfgLoader.getProperties("f")).thenReturn(getCfgProperties());

		UserImportSPIFactory factory = mock(UserImportSPIFactory.class);
		UserImportSPI importer = mock(UserImportSPI.class);
		when(importer.importUser("id", "type")).thenReturn(null);		
		when(factory.getInstance(anyObject(), anyObject())).thenReturn(importer);
		when(factory.getName()).thenReturn("mockI");

		List<UserImportSPIFactory> importersF = new ArrayList<>();
		importersF.add(factory);
		
		//when
		UserImportServiceImpl impl = new UserImportServiceImpl(mainCfg, importersF, 
				cp, verificatorUtil, cfgLoader);
		AuthenticationResult importUser = impl.importUser("id", "type");

		//then
		assertThat(importUser.getStatus(), is(Status.notApplicable));
		verify(factory).getInstance(getCfgProperties(), "idp");
		verify(importer).importUser("id", "type");
		
		//again - should cache negative resolve
		reset(importer); //just to have nice 'never'below
		AuthenticationResult importUser2 = impl.importUser("id", "type");
		assertThat(importUser2.getStatus(), is(Status.notApplicable));
		verify(importer, never()).importUser("id", "type");
		
		//again - should expire negative resolve cache
		Thread.sleep(1001);
		AuthenticationResult importUser3 = impl.importUser("id", "type");
		assertThat(importUser3.getStatus(), is(Status.notApplicable));
		verify(importer).importUser("id", "type");
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
