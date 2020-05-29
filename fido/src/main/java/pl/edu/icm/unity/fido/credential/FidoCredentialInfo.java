/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.fido.credential;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.attestation.Attestation;
import com.yubico.webauthn.attestation.Transport;
import com.yubico.webauthn.data.ByteArray;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.Constants;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

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
	/**
	 * Number of times private key was used to sign challenge - stored also on Authenticator
	 */
	private long signatureCount;

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
	private ByteArray aaguid;

	// FIXME investigate if provided data are sufficient. Maybe better to store whole attestation raw data (byte array received from authenticator)
	// Attestation properties
	/**
	 * Indicates if attestation key was confirmed as trusted during registration.
	 */
	private boolean attestationTrusted;
	// TODO clarify meaning and domain
	private String metadataIdentifier;
	// TODO clarify meaning and domain
	private Map<String, String> vendorProperties;
	// TODO clarify meaning and domain
	private Map<String, String> deviceProperties;
	/**
	 * What transport of data is supported by Authenticator.
	 */
	private Set<Transport> transports;

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

	public long getSignatureCount()
	{
		return signatureCount;
	}

	public boolean isUserPresent()
	{
		return userPresent;
	}

	public boolean isUserVerified()
	{
		return userVerified;
	}

	public ByteArray getAaguid()
	{
		return aaguid;
	}

	public boolean isAttestationTrusted()
	{
		return attestationTrusted;
	}

	public String getAttestationFormat()
	{
		return attestationFormat;
	}

	public String getMetadataIdentifier()
	{
		return metadataIdentifier;
	}

	public Map<String, String> getVendorProperties()
	{
		return vendorProperties;
	}

	public Map<String, String> getDeviceProperties()
	{
		return deviceProperties;
	}

	public Set<Transport> getTransports()
	{
		return transports;
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

	public static FidoCredentialInfoBuilder builder()
	{
		return new FidoCredentialInfoBuilder();
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
		private ByteArray aaguid;

		Attestation attestationMetadata;

		private FidoCredentialInfoBuilder()
		{
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

		public FidoCredentialInfoBuilder attestationMetadata(Attestation attestationMetadata)
		{
			this.attestationMetadata = attestationMetadata;
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

		public FidoCredentialInfoBuilder aaguid(ByteArray aaguid)
		{
			this.aaguid = aaguid;
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
			info.attestationFormat = this.attestationFormat.toLowerCase();
			info.aaguid = this.aaguid;
			if (info.attestationFormat.equals("none"))
			{
				info.aaguid = null; // for NONE attestation aaguid is always reset to 0s
			}

			if (nonNull(attestationMetadata))
			{
				info.attestationTrusted = attestationMetadata.isTrusted();
				info.metadataIdentifier = attestationMetadata.getMetadataIdentifier().orElse(null);
				info.vendorProperties = attestationMetadata.getVendorProperties().orElse(null);
				info.deviceProperties = attestationMetadata.getDeviceProperties().orElse(null);
				info.transports = attestationMetadata.getTransports().orElse(null);
			}

			return info;
		}
	}
}
