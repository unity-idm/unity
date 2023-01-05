/**********************************************************************
 *                     Copyright (c) 2023, Jirav
 *                        All Rights Reserved
 *
 *         This is unpublished proprietary source code of Jirav.
 *    Reproduction or distribution, in whole or in part, is forbidden
 *          except by express written permission of Jirav, Inc.
 **********************************************************************/
package io.imunity.otp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.Test;

public class TOTPKeyGeneratorTest
{
	@Test
	public void shouldEncodeIssuerAndUserWithSpaces()
	{
		OTPGenerationParams params = new OTPGenerationParams(6, HashFunction.SHA256, 30);
		
		String uri = TOTPKeyGenerator.generateTOTPURI("secret", "some user", "server label", params, Optional.empty());
		
		assertThat(uri).isEqualTo("otpauth://totp/server%20label:some%20user?secret=secret&issuer=server%20label&algorithm=SHA256&digits=6&period=30");
	}
}
