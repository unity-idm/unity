/*
 * Copyright (c) 2022 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.metadata.cfg;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static pl.edu.icm.unity.saml.sp.config.TrustedIdPKey.metadataEntity;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.IOException;
import java.time.Duration;

import org.apache.xmlbeans.XmlException;
import org.junit.jupiter.api.Test;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.saml.SamlProperties.Binding;
import pl.edu.icm.unity.saml.sp.config.BaseSamlConfiguration.RemoteMetadataSource;
import pl.edu.icm.unity.saml.sp.config.TrustedIdPConfiguration;
import pl.edu.icm.unity.saml.sp.config.TrustedIdPs;
import xmlbeans.org.oasis.saml2.metadata.EntitiesDescriptorDocument;

public class MetadataToSPConfigConverterTest
{
	private static final String LOGO = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAFAAAAA8CAIAAAB+RarbAAAC0GlDQ1BJQ0NQcm9maWxlAAB4nI2Uz0sUYRjHv7ONGChBYGZ7iKFDSKhMFmVE5a6/2LRtWX+UEsTs7Lu7k7Oz08zsmiIRXjpm0T0qDx76Azx46JSXwsAsAuluUUSCl5LteWfG3RHtxwsz83mfH9/ned/hfYEaWTFNPSQBecOxkn1R6fromFT7ESEcQR3CqFNU24wkEgOgwWOxa2y+h8C/K617+/866tK2mgeE/UDoR5rZKrDvF9kLWWoEELlew4RjOsT3OFue/THnlMfzrn0o2UW8SHxANS0e/5q4Q80paaBGJG7JBmJSAc7rRdXv5yA99cwYHqTvcerpLrN7fBZm0kp3P3Eb8ec06+7hmsTzGa03RtxMz1rG6h32WDihObEhj0Mjhh4f8LnJSMWv+pqi6UST2/p2abBn235LuZwgDhMnxwv9PKaRcjunckPXPBb0qVxX3Od3VjHJ6x6jmDlTd/8X9RZ6hVHoYNBg0NuAhCT6EEUrTFgoIEMejSI0sjI3xiK2Mb5npI5EgCXyr1POuptzG0XK5lkjiMYx01JRkOQP8ld5VX4qz8lfZsPF5qpnxrqpqcsPvpMur7yt63v9njx9lepGyKsjS9Z8ZU12oNNAdxljNlxV4jXY/fhmYJUsUKkVKVdp3K1Ucn02vSOBan/aPYpdml5sqtZaFRdurNQvTe/Yq8KuVbHKqnbOq3HBfCYeFU+KMbFDPAdJvCR2ihfFbpqdFwcqGcOkomHCVbKhUJaBSfKaO/6ZFwvvrLmjoY8ZzNJUiZ//hFXIaDoLHNF/uP9z8HvFo7Ei8MIGDp+u2jaS7h0iNC5Xbc4V4MI3ug/eVm3NdB4OPQEWzqhFq+RLC8IbimZ3HD7pKpiTlpbNOVK7LJ+VInQlMSlmqG0tkqLrkuuyJYvZzCqxdBvszKl2T6WedqXmU7m8Qeev9hGw9bBc/vmsXN56Tj2sAS/138C8/UXN/ALEAAAJI2lUWHRYTUw6Y29tLmFkb2JlLnhtcAAAAAAAPD94cGFja2V0IGJlZ2luPSLvu78iIGlkPSJXNU0wTXBDZWhpSHpyZVN6TlRjemtjOWQiPz4KPHg6eG1wbWV0YSB4bWxuczp4PSJhZG9iZTpuczptZXRhLyIgeDp4bXB0az0iWE1QIENvcmUgNC40LjAiPgogICA8cmRmOlJERiB4bWxuczpyZGY9Imh0dHA6Ly93d3cudzMub3JnLzE5OTkvMDIvMjItcmRmLXN5bnRheC1ucyMiPgogICAgICA8cmRmOkRlc2NyaXB0aW9uIHJkZjphYm91dD0iIi8+CiAgIDwvcmRmOlJERj4KPC94OnhtcG1ldGE+CiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIAogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgCiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIAogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgCiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIAogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgCiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIAogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgCiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIAogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgCiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIAogICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgCiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAKICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgIAogICAgICAgICAgICAgICAgICAgICAgICAgICAKPD94cGFja2V0IGVuZD0idyI/Pqfd9JIAAAAhdEVYdFNvZnR3YXJlAEdyYXBoaWNDb252ZXJ0ZXIgKEludGVsKXeH+hkAAAbvSURBVHic7JdZTFRnFMevwfhQTaPUhgfeWm1kLVBNNFRtDPpgQ2360PIgwYYH1PBCYmIV4vLSRCVYdCxg6bDJrrLJqqCoICiIOyCI7DAMi8CMgDC0v+HD25EhRAtt0+s9ubn57vnOd77z/87ynSv98Z6R9F8b8G+TCljppAJWOqmAlU4qYKWTCljppAJWOqmAlU4qYKWTCljp9P4BLixrTclvSC1o5GGQWdI0OjYhT09MTBiNxr6+vtbW1mfPnvFpraK5ufnp06e8Eaivr9fr9d3d3U+ePGlsbKytrYXPWgQ6Ozvb2tpgog0x+LKGa9euRUREZGRkjI2NITk+Pv4PAnb5LkVyOC05nzE/jpoVG7W6vpdiDtPv3btXVVV1e4ru3LljMBisVQQEBCxdunT//v3btm2ztbW9cOGCr6+vu7s7Yw8Pj3Xr1gUFBS1btuzYsWO7d+92cHDIz8+3s7NjLJZfunSJKQ4oMTExJiZGo9EMDg7Oauvk5KTJZJovYA+fdMktQvoiyvy4R9p5xfX0TwPu7++vrKysek1g7unpsVZRVla2ePFizPX391+9ejVmBQYGpqamAjgqKmrXrl04dtWqVQkJCQcOHECgoaGB4+jq6hLLDx8+3NvbK+946NCh48ePh4eHM+YshH40JCUlEQWzGrBggEdHR+/evWsJ+Pnz59YqysvLbWxsvL29XV1dnZycXr16RWRiIl5NS0tjjMy+ffs2bNiwdu1aYsHHx8fPz0+s5XSOHDkyPDwsawOwTqcDXklJyYkTJ+DEx8cTaEePHm1vb58nWjNgtx/SJNdfgWp+Po/4eEusDBhrHj9+TCSDljfefvToEcwZKvDDokWLsOngwYP29vbCXQ8ePFiyZAk+EfLV1dWSJHl5ee3cuZNBdna2vBx/cpQMWFhcXIxvyWEELl++HBoaCv/s2bM1NTWnTp0aGhpaAMCbf8xc7hm9fJPW/Hz5+2ffJOkHRuRp6lBFRQWAAUCl6ejomJFF4NmzZ4+joyOJumPHDhcXF0IX/t69e52dnbdu3Uqt4hMM27dvJ0WpT56engMDA7IGYiE4OBicISEhN27cSE5ORvjq1aucYEpKysmTJ9GMGaSMZSD8fcBdemNL51BL17D56Rxu1xlMpr98yKGSNhTqWeuzoNE3iZCWmSMjI3JEAEOMhYAlwaGSCzxiI45VCLM7s4znMODdAC+Ilv8RqYDfnUSIimosaEb4WX7KYkQp+ULYi08GJLN8z1uWRnLBUrlMhD1TIokg0kcwLRfKY/YSZki5pc3x2XUJOfU88dn16UWNI687LePIeEpBQ/zUlHk2s7amTm+9MSWN6l1QUCCqGtdsUVERhbelpUX0Z1euXOEupcHiar1+/Trtl26KuLToq5qamrjtKBOUKO4C5GnCuOq4lih4aEYhNYwaDuwXL17QxlFByW1uB5RQ57jzKeksqaurwwzeyPBGAJMwAw3cc0xxHJIzndaa05LTGfPjoFlu0Wm1dRs+WB8Nc3r2k19+Cq+wBsyWGKfVarlLzp07B1pUM05PT+ezsLAQTm5u7sWLF7EAVGFhYRwB4AGMoVyz9+/fRw9nQaMSHR2dmZlZWlqak5ODBhayigtZHBmf58+fz8vLo5ijHCVsTfHn6mJH9mJrtqCkoyErKwslcXFxzDJFh2cGPEfjQcVesTnGfD+LWUdNyJnb1oA5dRThAbyBcx4+fMgbU/AexwwTB9It8saBNFgI4DqCFi/hK8RevjTviAa8jTdYBZOrC2H04EY8xhJikmhCD1NsgTaCVvicdOANn1l0EibcaixBJwN2ZACTyF8AwNiExaLfME3RDAG2wTmWHAwFGwNSwPKWIg8BIH+yCkm6PdFjyQk/H5Lcvn+j01pp0WkB+MONWpjTs2tOB2sqrVXQmXC6BAzxSTfGJyfK582bN2/dukXIccbELXx+lXA1KUBHhTAei4yMRIZgZrZ4iljFEjjkLcFMsSGwYRLSRL5YiwCrWMJZsBH1glnrFnB2wFv8s1Zu1K78Ksb8bNI6fJssd1qdeuOnXyfCFLMfrf/t5+hqaxXAwwhSiLJBqjAgzei3SCFaQpG6ZB1/UcAgPuEjRuwRluQkmUxFISd5M0V6x8bGsgqcqMXhIkspBMQweYty+HyikwFHydlRt97S/xL+7NAZOnqmHp2hq9cod1oTpkkwy7M4fNAw+/VAJSSqxd1ALSUOSSc6JziEOlPcNzhHBDapi3G4V5iIJO4iCYkOsZwowLEI88ZvxDPL+VdFIcVclGuhGQHe8GekzFyA31Ju/rRQveE8Se20lE4qYKWTCljppAJWOqmAlU4qYKWTCljppAJWOqmAlU4qYKXTewf4TwAAAP//AwDTJKa92O1TGAAAAABJRU5ErkJggg==";
	private final TranslationProfile translationProfile1 = mock(TranslationProfile.class);
	
	@Test
	public void shouldParseSwitchAAIMetadata() throws EngineException
	{
		PKIManagement pkiManagement = mock(PKIManagement.class);
		when(pkiManagement.getCertificate(any())).thenThrow(IllegalArgumentException.class);
		MetadataToSPConfigConverter converter = new MetadataToSPConfigConverter(pkiManagement , "en");
		EntitiesDescriptorDocument metadata = loadMetadata("src/test/resources/metadata.switchaai.xml");
		RemoteMetadataSource metadataSrc = RemoteMetadataSource.builder()
				.withRegistrationForm("regForm")
				.withTranslationProfile(translationProfile1)
				.withUrl("dummy")
				.withRefreshInterval(Duration.ZERO)
				.build();
		
		TrustedIdPs trustedIdps = converter.convertToTrustedIdPs(metadata, metadataSrc);
		
		TrustedIdPConfiguration trustedIdP = trustedIdps.get(metadataEntity("https://aai-login.fh-htwchur.ch/idp/shibboleth", 1));
		assertThat(trustedIdP.idpEndpointURL).isEqualTo("https://aai-login.fh-htwchur.ch/idp/profile/SAML2/Redirect/SSO");
		assertThat(trustedIdP.binding).isEqualTo(Binding.HTTP_REDIRECT);
		assertThat(trustedIdP.certificateNames).allMatch(certName -> certName.contains("_SP_METADATA_CERT_"));
		assertThat(trustedIdP.groupMembershipAttribute).isNull();
		assertThat(trustedIdP.samlId).isEqualTo("https://aai-login.fh-htwchur.ch/idp/shibboleth");
		assertThat(trustedIdP.logoURI.getDefaultValue()).isEqualTo(LOGO);
		assertThat(trustedIdP.name.getValue("en")).isEqualTo("HTW Chur - University of Applied Sciences HTW Chur");
		assertThat(trustedIdP.name.getValue("de")).isEqualTo("HTW Chur - Hochschule für Technik und Wirtschaft");
		assertThat(trustedIdP.registrationForm).isEqualTo("regForm");
		assertThat(trustedIdP.requestedNameFormat).isNull();
		assertThat(trustedIdP.signRequest).isEqualTo(false);
		assertThat(trustedIdP.translationProfile).isEqualTo(translationProfile1);
		assertThat(trustedIdP.logoutEndpoints).isEmpty();
	}
	
	@Test	
	public void shouldSkipFilteredIdpByEntityID() throws EngineException
	{
		PKIManagement pkiManagement = mock(PKIManagement.class);
		when(pkiManagement.getCertificate(any())).thenThrow(IllegalArgumentException.class);
		MetadataToSPConfigConverter converter = new MetadataToSPConfigConverter(pkiManagement , "en");
		EntitiesDescriptorDocument metadata = loadMetadata("src/test/resources/metadata.switchaai.xml");
		RemoteMetadataSource metadataSrc = RemoteMetadataSource.builder()
				.withRegistrationForm("regForm")
				.withTranslationProfile(translationProfile1)
				.withUrl("dummy")
				.withRefreshInterval(Duration.ZERO)
				.withFederationIdpsFilter("entityID!=\"https://aai.unifr.ch/idp/shibboleth\"")
				.build();
		
		TrustedIdPs trustedIdps = converter.convertToTrustedIdPs(metadata, metadataSrc);
		assertThrows(IllegalArgumentException.class, () -> trustedIdps.get(metadataEntity("https://aai.unifr.ch/idp/shibboleth", 1)));
		
	}
	
	@Test	
	public void shouldParseMetadataWithoutNameSet() throws EngineException
	{
		PKIManagement pkiManagement = mock(PKIManagement.class);
		when(pkiManagement.getCertificate(any())).thenThrow(IllegalArgumentException.class);
		MetadataToSPConfigConverter converter = new MetadataToSPConfigConverter(pkiManagement , "en");
		EntitiesDescriptorDocument metadata = loadMetadata("src/test/resources/metadata.switchaai-one-no-name.xml");
		RemoteMetadataSource metadataSrc = RemoteMetadataSource.builder()
				.withRegistrationForm("regForm")
				.withTranslationProfile(translationProfile1)
				.withUrl("dummy")
				.withRefreshInterval(Duration.ZERO)
				.build();
		
		TrustedIdPs trustedIdps = converter.convertToTrustedIdPs(metadata, metadataSrc);
		TrustedIdPConfiguration trustedIdP = trustedIdps.get(metadataEntity("https://fake.idp.eu", 1));
		assertThat(trustedIdP.name.getValue("en")).isEqualTo("Unnamed Identity Provider");	
	}
	
	@Test
	public void shouldSkipFilteredIdpByAttribute() throws EngineException
	{
		PKIManagement pkiManagement = mock(PKIManagement.class);
		when(pkiManagement.getCertificate(any())).thenThrow(IllegalArgumentException.class);
		MetadataToSPConfigConverter converter = new MetadataToSPConfigConverter(pkiManagement , "en");
		EntitiesDescriptorDocument metadata = loadMetadata("src/test/resources/DFN-AAI-metadata-2certs.xml");
		RemoteMetadataSource metadataSrc = RemoteMetadataSource.builder()
				.withRegistrationForm("regForm")
				.withTranslationProfile(translationProfile1)
				.withUrl("dummy")
				.withRefreshInterval(Duration.ZERO)
				.withFederationIdpsFilter("attributes['http://macedir.org/entity-category'][0]!=\"http://aai.dfn.de/category/bwidm-member\"")
				.build();
		
		TrustedIdPs trustedIdps = converter.convertToTrustedIdPs(metadata, metadataSrc);
		assertThrows(IllegalArgumentException.class, () -> trustedIdps.get(metadataEntity("https://idp.scc.kit.edu/idp/shibboleth", 1)));
	}
	
	private EntitiesDescriptorDocument loadMetadata(String path)
	{
		try
		{
			return EntitiesDescriptorDocument.Factory.parse(new File(path));
		} catch (XmlException | IOException e)
		{
			throw new RuntimeException("Can't load test XML", e);
		}
	}
}
