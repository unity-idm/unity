/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.fido.credential;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.exception.HexException;

/**
 * Test for {@link FidoCredentialInfo} class
 */
public class FidoCredentialInfoTest
{
	private final Random random = new Random();

	@Test
	public void generateCredentialShouldSetAllFidoCredentialProperties() throws HexException
	{
		//given/when
		FidoCredentialInfo fidoCredentialInfo = generateCredential();

		//then
		assertEquals("Null value detected. Make sure FidoCredentialInfoTest::generateCredential() initialize all fields of credential.",
				-1, FidoCredentialInfo.serializeList(Collections.singletonList(fidoCredentialInfo)).indexOf("null"));
	}

	@Test
	public void copyBuilderShouldSetAllFidoCredentialProperties() throws HexException
	{
		//given/when
		FidoCredentialInfo fidoCredentialInfo = generateCredential();

		//then
		FidoCredentialInfo copy = fidoCredentialInfo.copyBuilder().build();
		assertEquals("Null value detected. Make sure FidoCredentialInfo::copyBuilder() initialize all fields of credential.",
				-1, FidoCredentialInfo.serializeList(Collections.singletonList(copy)).indexOf("null"));
	}

	@Test
	public void newFidoCredentialMatchesOriginalAfterUsingCopyBuilder() throws HexException
	{
		//given
		FidoCredentialInfo fidoCredentialInfo = generateCredential();

		//when
		FidoCredentialInfo copy = fidoCredentialInfo.copyBuilder()
				.build();

		//then
		assertEquals(fidoCredentialInfo, copy);
	}

	@Test
	public void copyBuilderAllowToChangeValuesOfFidoCredentials() throws HexException
	{
		//given
		FidoCredentialInfo fidoCredentialInfo = generateCredential();
		String desc = "New description " + random.nextInt(1000);
		long signCount = fidoCredentialInfo.getSignatureCount() + 1;

		//when
		FidoCredentialInfo copy1 = fidoCredentialInfo.copyBuilder()
				.description(desc)
				.signatureCount(signCount)
				.build();
		FidoCredentialInfo copy2 = fidoCredentialInfo.copyBuilder()
				.description(desc)
				.signatureCount(signCount)
				.build();

		//then
		assertNotEquals(fidoCredentialInfo, copy1);
		assertEquals(copy1, copy2);
	}

	@Test
	public void deserializedFidoCredentialListsMatchesSerialized() throws HexException
	{
		//given
		FidoCredentialInfo fidoCredentialInfo = generateCredential();

		//when
		String serialized = FidoCredentialInfo.serializeList(Collections.singletonList(fidoCredentialInfo));
		List<FidoCredentialInfo> credentials = FidoCredentialInfo.deserializeList(serialized);

		//then
		assertEquals(fidoCredentialInfo, credentials.get(0));
	}

	private FidoCredentialInfo generateCredential() throws HexException
	{
		return FidoCredentialInfo.builder()
				.registrationTime(System.currentTimeMillis())
				.credentialId(randomByteArray())
				.publicKeyCose(randomByteArray())
				.userPresent(random.nextBoolean())
				.userVerified(random.nextBoolean())
				.attestationFormat("android")
				.aaguid("123456789012345678" + random.nextInt(1000))
				.signatureCount(random.nextInt(1000))
				.description("Description " + random.nextInt(1000))
				.userHandle("a1bec3d4e5f" + random.nextInt(1000))
				.build();
	}

	private ByteArray randomByteArray() {
		byte[] value = new byte[32];
		random.nextBytes(value);
		return new ByteArray(value);
	}
}
