/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.fido;

import com.yubico.webauthn.CredentialRepository;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.types.authn.CredentialPublicInformation;
import pl.edu.icm.unity.types.authn.FidoCredentialInfo;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.UserHandle;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class UnityFidoRegistrationStorage implements CredentialRepository
{
	private static final org.apache.logging.log4j.Logger log = Log.getLogger(Log.U_SERVER_REST, UnityFidoRegistrationStorage.class);

	private FidoEntityHelper entityHelper;

	public UnityFidoRegistrationStorage(final FidoEntityHelper entityHelper)
	{
		this.entityHelper = entityHelper;
	}

	@Override
	public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(final String username)
	{
		log.debug("Enter getCredentialIdsForUsername({})", username);
		return getFidoCredentialInfoForUsername(username).stream()
				.map(i -> PublicKeyCredentialDescriptor.builder()
						.id(i.getCredentialId())
						.build())
				.collect(Collectors.toSet());
	}

	@Override
	public Optional<String> getUsernameForUserHandle(final ByteArray userHandle)
	{
		UserHandle uh = new UserHandle(userHandle.getBytes());
		log.debug("getUsernameForUserHandle({})", uh.asString());
		return entityHelper.getUsernameForUserHandle(uh.asString());
	}

	@Override
	public Optional<ByteArray> getUserHandleForUsername(final String username)
	{
		log.debug("getUserHandleForUsername({})", username);
		return entityHelper.getUserHandleForUsername(username).map(uh -> new ByteArray(UserHandle.fromString(uh).getBytes()));
	}

	List<FidoCredentialInfo> getFidoCredentialInfoForUserHandle(final String userHandle)
	{
		log.debug("getFidoCredentialInfoForUserHandle({})", userHandle);
		return getFidoCredentialInfoForEntity(entityHelper.getEntityByUserHandle(userHandle));
	}

	private List<FidoCredentialInfo> getFidoCredentialInfoForUsername(final String username)
	{
		log.debug("getFidoCredentialInfoForUsername({})", username);
		return getFidoCredentialInfoForEntity(entityHelper.getEntityByUsername(username));
	}

	private List<FidoCredentialInfo> getFidoCredentialInfoForEntity(final Entity entity)
	{
		if (isNull(entity))
			return Collections.emptyList();

		Map<String, CredentialPublicInformation> creds = entity.getCredentialInfo().getCredentialsState();
		CredentialPublicInformation info = creds.get("fido"); // FIXME - need to be proper credential

		if (nonNull(info))
			return FidoCredentialInfo.deserializeList(info.getExtraInformation());

		return Collections.emptyList();
	}

	@Override
	public Optional<RegisteredCredential> lookup(final ByteArray credentialId, final ByteArray userHandle)
	{
		log.debug("Enter lookup()");
		return getFidoCredentialInfoForUserHandle(new UserHandle(userHandle.getBytes()).asString()).stream()
				.filter(info -> info.getCredentialId().equals(credentialId))
				.map(info -> info.getCredentialWithHandle(userHandle))
				.findFirst();
	}

	@Override
	public Set<RegisteredCredential> lookupAll(final ByteArray credentialId)
	{
		log.debug("Enter lookupAll()");
		// FIXME used to make sure no other credential with given ID exists
		return Collections.EMPTY_SET;
	}

}
