/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token.access;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.oauth2.sdk.device.UserCode;

import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.exceptions.IllegalTypeException;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.oauth.as.DeviceCodeToken;
import pl.edu.icm.unity.store.api.TokenDAO.TokenNotFoundException;

/**
 * Built on top of generic token storage, handles access to persisted RFC 8628 device codes.
 * <p>
 * A device code is stored under two token records sharing the same expiration: the record itself
 * (keyed by device_code) and an index record (keyed by the normalized user_code, containing just
 * the device_code it points to). The generic token table already enforces a unique (NAME, TYPE)
 * pair, so the index record doubles as an indexed, collision-free lookup path for {@link
 * #findByUserCode(String)} - no linear scan/deserialization of every outstanding code, and no two
 * active codes can ever share a user_code.
 */
@Component
public class DeviceCodeRepository
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, DeviceCodeRepository.class);

	public static final String INTERNAL_DEVICE_TOKEN = "oauth2Device";
	public static final String INTERNAL_DEVICE_USER_CODE_INDEX = "oauth2DeviceUserCode";

	private final TokensManagement tokensMan;

	@Autowired
	public DeviceCodeRepository(TokensManagement tokensMan)
	{
		this.tokensMan = tokensMan;
	}

	/**
	 * Thrown when the given normalized user_code is already claimed by another, still active, index
	 * record. Callers should generate a fresh user_code and retry.
	 */
	public static class UserCodeAlreadyInUseException extends Exception
	{
		UserCodeAlreadyInUseException(Throwable cause)
		{
			super(cause);
		}
	}

	/**
	 * Claims the given normalized user_code for the given device_code, failing if another active
	 * code already claims it. Must be called (and succeed) before {@link #store(String,
	 * DeviceCodeToken, Date, Date)} for the same device_code.
	 */
	public void claimUserCode(String normalizedUserCode, String deviceCode, Date now, Date expiration)
			throws UserCodeAlreadyInUseException
	{
		try
		{
			tokensMan.addToken(INTERNAL_DEVICE_USER_CODE_INDEX, normalizedUserCode,
					deviceCode.getBytes(StandardCharsets.UTF_8), now, expiration);
		} catch (Exception e)
		{
			throw new UserCodeAlreadyInUseException(e);
		}
	}

	/**
	 * Releases a previously {@link #claimUserCode(String, String, Date, Date) claimed} user_code
	 * without a matching device code record ever having been stored (e.g. because storing it
	 * afterward failed). Best effort: an unreleased claim still self-expires.
	 */
	public void releaseClaimedUserCode(String normalizedUserCode)
	{
		removeUserCodeIndexQuietly(normalizedUserCode);
	}

	public void store(String deviceCode, DeviceCodeToken token, Date now, Date expiration)
			throws IllegalTypeException, JsonProcessingException
	{
		tokensMan.addToken(INTERNAL_DEVICE_TOKEN, deviceCode, token.getSerialized(), now, expiration);
	}

	public Optional<Token> getByDeviceCode(String deviceCode)
	{
		try
		{
			return Optional.of(tokensMan.getTokenById(INTERNAL_DEVICE_TOKEN, deviceCode));
		} catch (TokenNotFoundException e)
		{
			return Optional.empty();
		}
	}

	/**
	 * As {@link #getByDeviceCode(String)}, but takes a database row lock held for the rest of the
	 * current transaction. Use this instead of the plain lookup whenever the caller will
	 * conditionally write the record back afterward (approve/deny, or issue-and-remove on
	 * redemption), so the whole sequence is atomic and can't race a concurrent poll or approval
	 * acting on the same device code.
	 */
	public Optional<Token> getByDeviceCodeForUpdate(String deviceCode)
	{
		try
		{
			return Optional.of(tokensMan.getTokenByIdForUpdate(INTERNAL_DEVICE_TOKEN, deviceCode));
		} catch (TokenNotFoundException e)
		{
			return Optional.empty();
		}
	}

	public Optional<Token> findByUserCode(String userCode)
	{
		String normalized = new UserCode(userCode).getStrippedValue();
		Token indexEntry;
		try
		{
			indexEntry = tokensMan.getTokenById(INTERNAL_DEVICE_USER_CODE_INDEX, normalized);
		} catch (TokenNotFoundException e)
		{
			return Optional.empty();
		}
		String deviceCode = new String(indexEntry.getContents(), StandardCharsets.UTF_8);
		return getByDeviceCode(deviceCode);
	}

	public void update(String deviceCode, DeviceCodeToken token, Date expires) throws JsonProcessingException
	{
		tokensMan.updateToken(INTERNAL_DEVICE_TOKEN, deviceCode, expires, token.getSerialized());
	}

	/**
	 * Removes the device code record together with its user_code index entry. {@code userCode} may
	 * be null (e.g. for a record predating this index) - the index removal is then skipped, and the
	 * (nonexistent) index entry has nothing to leak.
	 */
	public void remove(String deviceCode, String userCode)
	{
		tokensMan.removeToken(INTERNAL_DEVICE_TOKEN, deviceCode);
		if (userCode != null)
			removeUserCodeIndexQuietly(new UserCode(userCode).getStrippedValue());
	}

	private void removeUserCodeIndexQuietly(String normalizedUserCode)
	{
		try
		{
			tokensMan.removeToken(INTERNAL_DEVICE_USER_CODE_INDEX, normalizedUserCode);
		} catch (Exception e)
		{
			// best effort - an orphaned index entry still self-expires with the rest of the record
			log.debug("Can not remove the device user_code index entry, it will self-expire", e);
		}
	}
}
