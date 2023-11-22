package pl.edu.icm.unity.saml.idp;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.endpoint.Endpoint;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.engine.api.authn.AuthorizationException;
import pl.edu.icm.unity.engine.api.idp.IdpPolicyAgreementContentChecker;
import pl.edu.icm.unity.engine.api.idp.IdpPolicyAgreementsConfiguration;
import pl.edu.icm.unity.engine.api.idp.IdpPolicyAgreementsConfigurationParser;
import pl.edu.icm.unity.saml.idp.web.SamlIdPWebEndpointFactory;

@Component
class SAMLIdpPolicyAgreementContentChecker implements IdpPolicyAgreementContentChecker
{

	private final EndpointManagement endpointManagement;
	private final MessageSource msg;

	SAMLIdpPolicyAgreementContentChecker(@Qualifier("insecure") EndpointManagement endpointManagement,
			MessageSource msg)
	{

		this.endpointManagement = endpointManagement;
		this.msg = msg;
	}

	@Override
	public boolean isPolicyUsedOnEndpoints(Long policyId) throws AuthorizationException
	{
		for (Endpoint endpoint : endpointManagement.getEndpoints()
				.stream()
				.filter(e -> e.getTypeId()
						.equals(SamlIdPWebEndpointFactory.TYPE.getName()))
				.collect(Collectors.toList()))
		{

			Properties raw = new Properties();
			try
			{
				raw.load(new StringReader(endpoint.getConfiguration()
						.getConfiguration()));
			} catch (IOException e)
			{
				throw new InternalException("Invalid configuration of the saml idp service", e);
			}
			SamlIdpProperties samlProperties = new SamlIdpProperties(raw);
			IdpPolicyAgreementsConfiguration policyConfig = IdpPolicyAgreementsConfigurationParser.fromPropoerties(msg,
					samlProperties);
			if (policyConfig.agreements.stream()
					.filter(a -> a.documentsIdsToAccept.contains(policyId))
					.findAny()
					.isPresent())
				return true;

		}

		return false;
	}
}
