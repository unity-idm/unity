/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.samlmeta;

import static pl.edu.icm.unity.saml.SamlProperties.METADATA_REFRESH;
import static pl.edu.icm.unity.saml.SamlProperties.METADATA_URL;
import static pl.edu.icm.unity.saml.SamlProperties.PUBLISH_METADATA;
import static pl.edu.icm.unity.saml.idp.SamlIdpProperties.ALLOWED_SP_CERTIFICATE;
import static pl.edu.icm.unity.saml.idp.SamlIdpProperties.ALLOWED_SP_ENCRYPT;
import static pl.edu.icm.unity.saml.idp.SamlIdpProperties.ALLOWED_SP_ENTITY;
import static pl.edu.icm.unity.saml.idp.SamlIdpProperties.ALLOWED_SP_LOGO;
import static pl.edu.icm.unity.saml.idp.SamlIdpProperties.ALLOWED_SP_NAME;
import static pl.edu.icm.unity.saml.idp.SamlIdpProperties.ALLOWED_SP_PREFIX;
import static pl.edu.icm.unity.saml.idp.SamlIdpProperties.ALLOWED_SP_RETURN_URL;
import static pl.edu.icm.unity.saml.idp.SamlIdpProperties.CREDENTIAL;
import static pl.edu.icm.unity.saml.idp.SamlIdpProperties.DEFAULT_GROUP;
import static pl.edu.icm.unity.saml.idp.SamlIdpProperties.GROUP;
import static pl.edu.icm.unity.saml.idp.SamlIdpProperties.ISSUER_URI;
import static pl.edu.icm.unity.saml.idp.SamlIdpProperties.P;
import static pl.edu.icm.unity.saml.idp.SamlIdpProperties.SPMETA_PREFIX;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.saml.SamlProperties;
import pl.edu.icm.unity.saml.idp.SamlIdpProperties;
import pl.edu.icm.unity.saml.metadata.cfg.MetaDownloadManager;
import pl.edu.icm.unity.saml.metadata.cfg.MetaToIDPConfigConverter;
import pl.edu.icm.unity.saml.metadata.cfg.RemoteMetaManager;
import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.server.utils.ExecutorsService;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;

public class TestDownloadManager extends DBIntegrationTestBase
{
	@Autowired
	private ExecutorsService executorsService;

	@Autowired
	private UnityServerConfiguration mainConfig;

	@Autowired
	private PKIManagement pkiManagement;

	@Autowired
	private MetaDownloadManager downloadManager;
	
	@Autowired
	private UnityMessageSource msg;
	
	@Ignore
	@Test
	public void testDownload() throws IOException, EngineException, InterruptedException
	{
		int refreshTime = 10;
		Properties p = new Properties();
		p.setProperty(P + CREDENTIAL, "MAIN");
		p.setProperty(P + PUBLISH_METADATA, "false");
		p.setProperty(P + ISSUER_URI, "me");
		p.setProperty(P + GROUP, "group");
		p.setProperty(P + DEFAULT_GROUP, "group");
		p.setProperty(P + METADATA_REFRESH, String.valueOf(refreshTime));
		p.setProperty(P + ALLOWED_SP_PREFIX + "1." + ALLOWED_SP_ENTITY,
				"https://support.hes-so.ch/shibboleth");
		p.setProperty(P + ALLOWED_SP_PREFIX + "1." + ALLOWED_SP_RETURN_URL, "URL");
		p.setProperty(P + ALLOWED_SP_PREFIX + "1." + ALLOWED_SP_NAME, "Name");
		p.setProperty(P + ALLOWED_SP_PREFIX + "1." + ALLOWED_SP_LOGO, "http://example.com");
		p.setProperty(P + ALLOWED_SP_PREFIX + "1." + ALLOWED_SP_ENCRYPT, "true");
		p.setProperty(P + ALLOWED_SP_PREFIX + "1." + ALLOWED_SP_CERTIFICATE, "MAIN");

		ArrayList<RemoteMetaManager> mans = new ArrayList<RemoteMetaManager>();

		for (int i = 0; i < 10; i++)
		{
			if (i % 2 == 0)
			{
				p.setProperty(P + SPMETA_PREFIX + "1." + METADATA_URL,
						new String("http://metadata.aai.switch.ch/metadata.switchaai.xml"));
			} else
			{
				p.setProperty(P + SPMETA_PREFIX + "1." + METADATA_URL,
						new String("https://www.aai.dfn.de/fileadmin/metadata/DFN-AAI-metadata.xml"));
			}
			SamlIdpProperties configuration = new SamlIdpProperties(p, pkiManagement);

			RemoteMetaManager manager = new RemoteMetaManager(configuration,
					mainConfig, executorsService, pkiManagement,
					new MetaToIDPConfigConverter(pkiManagement, msg),
					downloadManager, SamlIdpProperties.SPMETA_PREFIX);
			mans.add(manager);
		}
		
		for (RemoteMetaManager m : mans)
		{
			m.start();
		}

		Thread.sleep(5 * refreshTime * 500);
	}
	
	@Ignore
	@Test
	public void testDownloadFromHttps() throws IOException, EngineException, InterruptedException
	{
		Properties p = new Properties();
		p.setProperty(P + CREDENTIAL, "MAIN");
		p.setProperty(P + PUBLISH_METADATA, "false");
		p.setProperty(P + ISSUER_URI, "me");
		p.setProperty(P + GROUP, "group");
		p.setProperty(P + DEFAULT_GROUP, "group");
		p.setProperty(P + METADATA_REFRESH, "100");
		p.setProperty(P + SPMETA_PREFIX + "1." + SamlProperties.METADATA_HTTPS_TRUSTSTORE, "EGI");
		p.setProperty(P + SPMETA_PREFIX + "1." + METADATA_URL,
				new String("https://engine.egipilot.lab.surf.net/authentication/proxy/idps-metadata"));

		SamlIdpProperties configuration = new SamlIdpProperties(p, pkiManagement);

		RemoteMetaManager manager = new RemoteMetaManager(configuration,
					mainConfig, executorsService, pkiManagement,
					new MetaToIDPConfigConverter(pkiManagement, msg),
					downloadManager, SamlIdpProperties.SPMETA_PREFIX);
		manager.start();
		Thread.sleep(5 * 1000*3);
	}

}
