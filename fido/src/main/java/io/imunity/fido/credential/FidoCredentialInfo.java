/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.fido.credential;

import static java.util.Objects.isNull;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.ByteArray;

import pl.edu.icm.unity.base.Constants;


/**
 * Holds information about Fido 2 credential details - represents single public key information.
 * <p>
 * Exposes serialization and deserialization methods of full Fido 2 credential (may contain multiple public keys).
 *
 * @author R. Ledzinski
 */
public class FidoCredentialInfo
{
	private FidoCredentialInfo()
	{
	}

	private static final Logger log = LogManager.getLogger(FidoCredentialInfo.class);
	// Credential properties
	/**
	 * Key creation timestamp
	 */
	private long registrationTimestamp;
	/**
	 * Unique identified of key - stored also on Authenticator
	 */
	private ByteArray credentialId;
	/**
	 * Encoded public key
	 */
	private ByteArray publicKeyCose;

	// Registration details
	/**
	 * Indicates if user presence verified during registration.
	 */
	private boolean userPresent;
	/**
	 * Indicates if user identity verified during registration. E.g. via PIN, fingerprint etc.
	 */
	private boolean userVerified;
	/**
	 * Provide information on structure of given Attestation object
	 */
	private String attestationFormat;
	/**
	 * Fido 2  Authenticator Attestation GUID - identifies model of authenticator. null if NONE authenticationType
	 */
	private String aaguid;

	// Mutable fields
	/**
	 * Number of times private key was used to sign challenge - stored also on Authenticator
	 */
	private long signatureCount;
	/**
	 * Human readable description.
	 */
	private String description;
	/**
	 * Fido user handle. All entities's credentials have the same value.
	 */
	private String userHandle;

	public long getRegistrationTimestamp()
	{
		return registrationTimestamp;
	}

	public ByteArray getCredentialId()
	{
		return credentialId;
	}

	public ByteArray getPublicKeyCose()
	{
		return publicKeyCose;
	}

	public boolean isUserPresent()
	{
		return userPresent;
	}

	public boolean isUserVerified()
	{
		return userVerified;
	}

	public String getAaguid()
	{
		return aaguid;
	}

	public String getAttestationFormat()
	{
		return attestationFormat;
	}

	public long getSignatureCount()
	{
		return signatureCount;
	}

	public String getDescription()
	{
		return description;
	}

	public String getUserHandle()
	{
		return userHandle;
	}

	@JsonIgnore
	public RegisteredCredential getCredentialWithHandle(final ByteArray userHandle)
	{
		return RegisteredCredential.builder()
				.credentialId(credentialId)
				.userHandle(userHandle)
				.publicKeyCose(publicKeyCose)
				.signatureCount(signatureCount)
				.build();
	}

	/**
	 * Coverts single Fido 2 credential (multiple keys) into JSON representation.
	 *
	 * @param list of Fido public keys information
	 * @return JSON representation
	 */
	public static String serializeList(final List<FidoCredentialInfo> list)
	{
		try
		{
			return Constants.MAPPER.writeValueAsString(list);
		} catch (JsonProcessingException e)
		{
			log.error("Failed to serialize fido credential information", e);
			return null;
		}
	}

	/**
	 * Coverts JSON into Fido 2 credential.
	 *
	 * @param credentials JSON representation of Fido 2 credential
	 * @return List of Fido 2 public keys with metadata.
	 */
	public static List<FidoCredentialInfo> deserializeList(final String credentials)
	{
		if (isNull(credentials) || credentials.isEmpty())
		{
			return Collections.emptyList();
		}

		try
		{
			return Constants.MAPPER.readValue(credentials, new TypeReference<List<FidoCredentialInfo>>()
			{
			});
		} catch (IOException e)
		{
			log.error("Failed to deserialize fido credential information: ", e);
			return Collections.emptyList();
		}
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		FidoCredentialInfo that = (FidoCredentialInfo) o;
		return registrationTimestamp == that.registrationTimestamp &&
				userPresent == that.userPresent &&
				userVerified == that.userVerified &&
				signatureCount == that.signatureCount &&
				Objects.equals(credentialId, that.credentialId) &&
				Objects.equals(publicKeyCose, that.publicKeyCose) &&
				Objects.equals(attestationFormat, that.attestationFormat) &&
				Objects.equals(aaguid, that.aaguid) &&
				Objects.equals(description, that.description) &&
				Objects.equals(userHandle, that.userHandle);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(registrationTimestamp, credentialId, publicKeyCose, userPresent, userVerified, 
				attestationFormat, aaguid, 
				signatureCount, description, userHandle);
	}

	public static FidoCredentialInfoBuilder builder()
	{
		return new FidoCredentialInfoBuilder();
	}

	public FidoCredentialInfoBuilder copyBuilder()
	{
		return new FidoCredentialInfoBuilder(this);
	}

	public static final class FidoCredentialInfoBuilder
	{
		private long registrationTimestamp;
		private ByteArray credentialId;
		private ByteArray publicKeyCose;
		private long signatureCount;

		private boolean userPresent;
		private boolean userVerified;
		private String attestationFormat;
		private String aaguid;
		private String description;

		private String userHandle;

		private FidoCredentialInfoBuilder()
		{
		}

		private FidoCredentialInfoBuilder(FidoCredentialInfo credentialInfo)
		{
			this.registrationTimestamp = credentialInfo.registrationTimestamp;
			this.credentialId = credentialInfo.credentialId;
			this.publicKeyCose = credentialInfo.publicKeyCose;
			this.signatureCount = credentialInfo.signatureCount;
			this.userPresent = credentialInfo.userPresent;
			this.userVerified = credentialInfo.userVerified;
			this.attestationFormat = credentialInfo.attestationFormat;
			this.aaguid = credentialInfo.aaguid;
			this.description = credentialInfo.description;
			this.userHandle = credentialInfo.userHandle;;
		}

		public static FidoCredentialInfoBuilder aFidoCredentialInfo()
		{
			return new FidoCredentialInfoBuilder();
		}

		public FidoCredentialInfoBuilder credentialId(ByteArray credentialId)
		{
			this.credentialId = credentialId;
			return this;
		}

		public FidoCredentialInfoBuilder publicKeyCose(ByteArray publicKeyCose)
		{
			this.publicKeyCose = publicKeyCose;
			return this;
		}

		public FidoCredentialInfoBuilder signatureCount(long signatureCount)
		{
			this.signatureCount = signatureCount;
			return this;
		}

		public FidoCredentialInfoBuilder registrationTime(long registrationTime)
		{
			this.registrationTimestamp = registrationTime;
			return this;
		}

		public FidoCredentialInfoBuilder userPresent(final boolean userPresent)
		{
			this.userPresent = userPresent;
			return this;
		}

		public FidoCredentialInfoBuilder userVerified(final boolean userVerified)
		{
			this.userVerified = userVerified;
			return this;
		}

		public FidoCredentialInfoBuilder attestationFormat(String attestationFormat)
		{
			this.attestationFormat = attestationFormat;
			return this;
		}

		public FidoCredentialInfoBuilder aaguid(String aaguid)
		{
			this.aaguid = aaguid;
			return this;
		}

		public FidoCredentialInfoBuilder description(String description)
		{
			this.description = description;
			return this;
		}

		public FidoCredentialInfoBuilder userHandle(String userHandle)
		{
			this.userHandle = userHandle;
			return this;
		}

		public FidoCredentialInfo build()
		{
			FidoCredentialInfo info = new FidoCredentialInfo();

			info.registrationTimestamp = this.registrationTimestamp;
			info.credentialId = this.credentialId;
			info.publicKeyCose = publicKeyCose;
			info.signatureCount = this.signatureCount;

			info.userPresent = this.userPresent;
			info.userVerified = this.userVerified;
			info.attestationFormat = Optional.ofNullable(this.attestationFormat).map(String::toLowerCase).orElse(null);
			info.aaguid = this.aaguid;
			info.userHandle = this.userHandle;
			if (Optional.ofNullable(info.attestationFormat).filter(f -> f.equals("none")).isPresent())
			{
				info.aaguid = null; // for NONE attestation aaguid is always reset to 0s
			}

			info.description = this.description;

			return info;
		}
	}
}
