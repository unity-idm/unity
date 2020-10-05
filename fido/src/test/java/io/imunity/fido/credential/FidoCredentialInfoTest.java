/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.fido.credential;

import com.google.common.collect.ImmutableMap;
import com.yubico.webauthn.attestation.Transport;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.exception.HexException;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test for {@link FidoCredentialInfo} class
 */
public class FidoCredentialInfoTest
{
	private final Random random = new Random();

	@Test
	public void shouldContainsAllProperties() throws HexException
	{
		//given/when
		FidoCredentialInfo fidoCredentialInfo = generateCredential();

		//then
		assertEquals(15, FidoCredentialInfo.serializeList(Collections.singletonList(fidoCredentialInfo)).split(",").length);
	}

	@Test
	public void shouldCopyCredential() throws HexException
	{
		//given
		FidoCredentialInfo fidoCredentialInfo = generateCredential();

		//when
		FidoCredentialInfo copy = fidoCredentialInfo.copyBuilder()
				.build();

		//then
		assertTrue(credentialsAreEqual(fidoCredentialInfo, copy));
	}

	@Test
	public void shouldCopyCredentialWithUpdates() throws HexException
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
		assertFalse(credentialsAreEqual(fidoCredentialInfo, copy1));
		assertTrue(credentialsAreEqual(copy1, copy2));
	}

	@Test
	public void shouldSerializeAndDeserializeProperly() throws HexException
	{
		//given
		FidoCredentialInfo fidoCredentialInfo = generateCredential();

		//when
		String serialized = FidoCredentialInfo.serializeList(Collections.singletonList(fidoCredentialInfo));
		List<FidoCredentialInfo> credentials = FidoCredentialInfo.deserializeList(serialized);

		//then
		assertTrue(credentialsAreEqual(fidoCredentialInfo, credentials.get(0)));
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
				.attestationTrusted(random.nextBoolean())
				.metadataIdentifier("metadataIdentyfier" + random.nextInt(1000))
				.vendorProperties(ImmutableMap.of("k1", "v1" + random.nextInt(1000)))
				.deviceProperties(ImmutableMap.of("k2", "v2" + random.nextInt(1000)))
				.transports(new HashSet<>(Arrays.asList(Transport.USB)))
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

	public static boolean credentialsAreEqual(FidoCredentialInfo o1, FidoCredentialInfo o2)
	{
		if (o1 == o2) return true;
		if (o2 == null) return false;
		return o1.getRegistrationTimestamp() == o2.getRegistrationTimestamp() &&
				o1.isUserPresent() == o2.isUserPresent() &&
				o1.isUserVerified() == o2.isUserVerified() &&
				o1.isAttestationTrusted() == o2.isAttestationTrusted() &&
				o1.getSignatureCount() == o2.getSignatureCount() &&
				Objects.equals(o1.getCredentialId(), o2.getCredentialId()) &&
				Objects.equals(o1.getPublicKeyCose(), o2.getPublicKeyCose()) &&
				Objects.equals(o1.getAttestationFormat(), o2.getAttestationFormat()) &&
				Objects.equals(o1.getAaguid(), o2.getAaguid()) &&
				Objects.equals(o1.getMetadataIdentifier(), o2.getMetadataIdentifier()) &&
				Objects.equals(o1.getVendorProperties(), o2.getVendorProperties()) &&
				Objects.equals(o1.getDeviceProperties(), o2.getDeviceProperties()) &&
				Objects.equals(o1.getTransports(), o2.getTransports()) &&
				Objects.equals(o1.getDescription(), o2.getDescription()) &&
				Objects.equals(o1.getUserHandle(), o2.getUserHandle());
	}
}
