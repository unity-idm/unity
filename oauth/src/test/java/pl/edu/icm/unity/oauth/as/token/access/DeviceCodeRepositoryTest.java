/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token.access;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.nimbusds.oauth2.sdk.device.UserCode;

import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.oauth.as.DeviceCodeStatus;
import pl.edu.icm.unity.oauth.as.DeviceCodeToken;
import pl.edu.icm.unity.oauth.as.MockTokensMan;
import pl.edu.icm.unity.oauth.as.OAuthToken;
import pl.edu.icm.unity.oauth.as.token.access.DeviceCodeRepository.UserCodeAlreadyInUseException;

public class DeviceCodeRepositoryTest
{
	private DeviceCodeRepository tested;

	@BeforeEach
	void setUp()
	{
		tested = new DeviceCodeRepository(new MockTokensMan());
	}

	private void storeWithUserCode(String deviceCode, String userCode, Date now, Date expiration) throws Exception
	{
		tested.claimUserCode(new UserCode(userCode).getStrippedValue(), deviceCode, now, expiration);
		OAuthToken oauthToken = new OAuthToken();
		oauthToken.setEffectiveScope(List.of());
		DeviceCodeToken token = new DeviceCodeToken();
		token.setOauthToken(oauthToken);
		token.setUserCode(userCode);
		token.setDeviceCodeStatus(DeviceCodeStatus.PENDING);
		tested.store(deviceCode, token, now, expiration);
	}

	@Test
	void shouldFindByUserCodeCaseAndDashInsensitively() throws Exception
	{
		Date now = new Date();
		storeWithUserCode("device-code-1", "WDJB-MJHT", now, new Date(now.getTime() + 60_000));

		Optional<Token> found = tested.findByUserCode("wdjbmjht");

		assertThat(found).isPresent();
		assertThat(found.get().getValue()).isEqualTo("device-code-1");
	}

	@Test
	void shouldNotFindUnknownUserCode() throws Exception
	{
		Optional<Token> found = tested.findByUserCode("does-not-exist");

		assertThat(found).isEmpty();
	}

	@Test
	void shouldNotFindAfterRemoval() throws Exception
	{
		Date now = new Date();
		storeWithUserCode("device-code-2", "ABCD-EFGH", now, new Date(now.getTime() + 60_000));

		tested.remove("device-code-2", "ABCD-EFGH");

		assertThat(tested.findByUserCode("ABCD-EFGH")).isEmpty();
		assertThat(tested.getByDeviceCode("device-code-2")).isEmpty();
	}

	@Test
	void shouldRejectClaimingAlreadyActiveUserCode() throws Exception
	{
		Date now = new Date();
		Date expiration = new Date(now.getTime() + 60_000);
		tested.claimUserCode("SAME-CODE", "device-code-3", now, expiration);

		assertThatThrownBy(() -> tested.claimUserCode("SAME-CODE", "device-code-4", now, expiration))
				.isInstanceOf(UserCodeAlreadyInUseException.class);
	}

	@Test
	void shouldAllowReclaimingAfterRelease() throws Exception
	{
		Date now = new Date();
		Date expiration = new Date(now.getTime() + 60_000);
		tested.claimUserCode("RELEASE-ME", "device-code-5", now, expiration);

		tested.releaseClaimedUserCode("RELEASE-ME");

		// does not throw: the code is free again
		tested.claimUserCode("RELEASE-ME", "device-code-6", now, expiration);
	}
}
