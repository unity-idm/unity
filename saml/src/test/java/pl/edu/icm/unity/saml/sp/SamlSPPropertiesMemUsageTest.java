/*
 * Copyright (c) 2022 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp;

import static pl.edu.icm.unity.saml.SamlProperties.METADATA_URL;
import static pl.edu.icm.unity.saml.sp.SAMLSPProperties.CREDENTIAL;
import static pl.edu.icm.unity.saml.sp.SAMLSPProperties.DEF_SIGN_REQUEST;
import static pl.edu.icm.unity.saml.sp.SAMLSPProperties.IDPMETA_PREFIX;
import static pl.edu.icm.unity.saml.sp.SAMLSPProperties.METADATA_PATH;
import static pl.edu.icm.unity.saml.sp.SAMLSPProperties.P;
import static pl.edu.icm.unity.saml.sp.SAMLSPProperties.REQUESTER_ID;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.UnityIntegrationTest;
import pl.edu.icm.unity.saml.sp.config.TrustedIdPs;

@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource(properties = { "unityConfig: src/test/resources/unityServer.conf" })
@UnityIntegrationTest
public class SamlSPPropertiesMemUsageTest
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CORE, SamlSPPropertiesMemUsageTest.class);
	@Autowired
	private SAMLVerificator.Factory verificatorFactory;
	
	/* 
	 * Requires to download and configure metadata file of a large federation
	 */
	@Test
	@Ignore
	public void measureMemUse() throws IOException, InterruptedException
	{
		String stringCfg = configWithTrustedFederation();
		SAMLVerificator verificator = (SAMLVerificator) verificatorFactory.newInstance();
		verificator.setSerializedConfiguration(stringCfg);
		List<TrustedIdPs> configs = new ArrayList<>();
		log.info("Waiting for init");
		Thread.sleep(10_000);
		
		verificator.getTrustedIdPs();
		System.gc();
		long startingMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		log.info("Mem usage start {}kB", startingMem/1000); 
		for (int i=0; i<100; i++)
		{
			configs.add(verificator.getTrustedIdPs());
			System.gc();
			long usage = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			log.info("Mem usage after {} allocations: {}kB (inc: {}kB)", i, 
					usage / 1000, (usage-startingMem)/1000);
		}
		Thread.sleep(Duration.ofMinutes(10).toMillis());
	}

	private String configWithTrustedFederation() throws IOException
	{
		Properties p = new Properties();
		p.setProperty(P+REQUESTER_ID, "foo");
		p.setProperty(P+DEF_SIGN_REQUEST, "true");
		p.setProperty(P+CREDENTIAL, "MAIN");
		p.setProperty(P+METADATA_PATH, "meta");
		 //SET URL to locally downloaded big federation meta file.
		p.setProperty(P+IDPMETA_PREFIX+"m1."+METADATA_URL, "file:///../../../../unity/tmp/edugain.xml");
		ByteArrayOutputStream sos = new ByteArrayOutputStream(); 
		p.store(sos, "");
		return sos.toString();
	}
}
