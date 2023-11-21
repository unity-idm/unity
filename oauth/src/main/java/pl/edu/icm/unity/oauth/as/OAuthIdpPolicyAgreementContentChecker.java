package pl.edu.icm.unity.oauth.as;

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
import pl.edu.icm.unity.oauth.as.webauthz.OAuthAuthzWebEndpoint;

@Component
class OAuthIdpPolicyAgreementContentChecker implements IdpPolicyAgreementContentChecker
{

	private final EndpointManagement endpointManagement;
	private final MessageSource msg;

	OAuthIdpPolicyAgreementContentChecker(@Qualifier("insecure") EndpointManagement endpointManagement,
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
						.equals(OAuthAuthzWebEndpoint.Factory.TYPE.getName()))
				.collect(Collectors.toList()))
		{

			Properties raw = new Properties();
			try
			{
				raw.load(new StringReader(endpoint.getConfiguration()
						.getConfiguration()));
			} catch (IOException e)
			{
				throw new InternalException("Invalid configuration of the oauth idp service", e);
			}
			OAuthASProperties oAuthASProperties = new OAuthASProperties(raw);
			IdpPolicyAgreementsConfiguration policyConfig = IdpPolicyAgreementsConfigurationParser.fromPropoerties(msg,
					oAuthASProperties);
			if (policyConfig.agreements.stream()
					.filter(a -> a.documentsIdsToAccept.contains(policyId))
					.findAny()
					.isPresent())
				return true;

		}

		return false;
	}

}
