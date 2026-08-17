/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token.access;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.oauth.as.DeviceCodeStatus;
import pl.edu.icm.unity.oauth.as.MockTokensMan;
import pl.edu.icm.unity.oauth.as.OAuthToken;

public class DeviceCodeRepositoryTest
{
	private DeviceCodeRepository tested;

	@BeforeEach
	void setUp()
	{
		tested = new DeviceCodeRepository(new MockTokensMan());
	}

	@Test
	void shouldFindByUserCodeCaseAndDashInsensitively() throws Exception
	{
		OAuthToken token = new OAuthToken();
		token.setUserCode("WDJB-MJHT");
		token.setDeviceCodeStatus(DeviceCodeStatus.PENDING);
		token.setEffectiveScope(List.of());
		Date now = new Date();
		tested.store("device-code-1", token, now, new Date(now.getTime() + 60_000));

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
		OAuthToken token = new OAuthToken();
		token.setUserCode("ABCD-EFGH");
		token.setDeviceCodeStatus(DeviceCodeStatus.PENDING);
		token.setEffectiveScope(List.of());
		Date now = new Date();
		tested.store("device-code-2", token, now, new Date(now.getTime() + 60_000));

		tested.remove("device-code-2");

		assertThat(tested.findByUserCode("ABCD-EFGH")).isEmpty();
		assertThat(tested.getByDeviceCode("device-code-2")).isEmpty();
	}
}
