// Copyright (c) 2018, Yubico AB
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice, this
//    list of conditions and the following disclaimer.
//
// 2. Redistributions in binary form must reproduce the above copyright notice,
//    this list of conditions and the following disclaimer in the documentation
//    and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
// FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
// DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
// CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
// OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package pl.edu.icm.unity.engine.fido;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.attestation.Attestation;
import com.yubico.webauthn.data.UserIdentity;

import java.time.Instant;
import java.util.Optional;

// FIXME to be replaced Unity storage
public class CredentialRegistration {

    long signatureCount;

    public long getSignatureCount() {
        return signatureCount;
    }

    public UserIdentity getUserIdentity() {
        return userIdentity;
    }

    public Optional<String> getCredentialNickname() {
        return credentialNickname;
    }

    public Instant getRegistrationTime() {
        return registrationTime;
    }

    public RegisteredCredential getCredential() {
        return credential;
    }

    public Optional<Attestation> getAttestationMetadata() {
        return attestationMetadata;
    }

    UserIdentity userIdentity;
    Optional<String> credentialNickname;

    Instant registrationTime;
    RegisteredCredential credential;

    Optional<Attestation> attestationMetadata;

    @JsonProperty("registrationTime")
    public String getRegistrationTimestamp() {
        return registrationTime.toString();
    }

    public String getUsername() {
        return userIdentity.getName();
    }

    CredentialRegistration withSignatureCount(long signatureCount) {
        this.signatureCount = signatureCount;
        return this;
    }

    public static CredentialRegistrationBuilder builder() {
    	return new CredentialRegistrationBuilder();
	}

	public static final class CredentialRegistrationBuilder {

		long signatureCount;
		UserIdentity userIdentity;
		Optional<String> credentialNickname;
		Instant registrationTime;
		RegisteredCredential credential;
		Optional<Attestation> attestationMetadata;

		private CredentialRegistrationBuilder() {
		}

		public static CredentialRegistrationBuilder aCredentialRegistration() {
			return new CredentialRegistrationBuilder();
		}

		public CredentialRegistrationBuilder signatureCount(long signatureCount) {
			this.signatureCount = signatureCount;
			return this;
		}

		public CredentialRegistrationBuilder userIdentity(UserIdentity userIdentity) {
			this.userIdentity = userIdentity;
			return this;
		}

		public CredentialRegistrationBuilder credentialNickname(Optional<String> credentialNickname) {
			this.credentialNickname = credentialNickname;
			return this;
		}

		public CredentialRegistrationBuilder registrationTime(Instant registrationTime) {
			this.registrationTime = registrationTime;
			return this;
		}

		public CredentialRegistrationBuilder credential(RegisteredCredential credential) {
			this.credential = credential;
			return this;
		}

		public CredentialRegistrationBuilder attestationMetadata(Optional<Attestation> attestationMetadata) {
			this.attestationMetadata = attestationMetadata;
			return this;
		}

		public CredentialRegistration build() {
			CredentialRegistration credentialRegistration = new CredentialRegistration();
			credentialRegistration.credential = this.credential;
			credentialRegistration.userIdentity = this.userIdentity;
			credentialRegistration.signatureCount = this.signatureCount;
			credentialRegistration.registrationTime = this.registrationTime;
			credentialRegistration.attestationMetadata = this.attestationMetadata;
			credentialRegistration.credentialNickname = this.credentialNickname;
			return credentialRegistration;
		}
	}
}
