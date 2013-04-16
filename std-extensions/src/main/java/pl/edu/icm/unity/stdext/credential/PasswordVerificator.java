/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.credential;

import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.util.Arrays;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import edu.vt.middleware.password.AlphabeticalSequenceRule;
import edu.vt.middleware.password.CharacterCharacteristicsRule;
import edu.vt.middleware.password.DigitCharacterRule;
import edu.vt.middleware.password.LengthRule;
import edu.vt.middleware.password.LowercaseCharacterRule;
import edu.vt.middleware.password.NonAlphanumericCharacterRule;
import edu.vt.middleware.password.NumericalSequenceRule;
import edu.vt.middleware.password.Password;
import edu.vt.middleware.password.PasswordData;
import edu.vt.middleware.password.PasswordValidator;
import edu.vt.middleware.password.QwertySequenceRule;
import edu.vt.middleware.password.RepeatCharacterRegexRule;
import edu.vt.middleware.password.Rule;
import edu.vt.middleware.password.RuleResult;
import edu.vt.middleware.password.UppercaseCharacterRule;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.RuntimeEngineException;
import pl.edu.icm.unity.server.authn.AbstractLocalVerificator;
import pl.edu.icm.unity.server.authn.AuthenticatedEntity;
import pl.edu.icm.unity.server.authn.EntityWithCredential;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.authn.LocalCredentialState;

/**
 * Ordinary password credential. Highly configurable: it is possible to set minimal length,
 * what character classes are required, minimum number of character classes, how many previous passwords 
 * should be stored and not repeated after change, how often the password must be changed.
 * 
 * @author K. Benedyczak
 */
public class PasswordVerificator extends AbstractLocalVerificator implements PasswordExchange
{ 	
	private static final String[] IDENTITY_TYPES = {UsernameIdentity.ID};

	private Random random = new Random();
	private int minLength;
	private int historySize;
	private int minClassesNum;
	private boolean denySequences;
	private long maxAge;


	public PasswordVerificator(String name, String description)
	{
		super(name, description, PasswordExchange.ID);
	}

	@Override
	public String getSerializedConfiguration()
	{
		ObjectNode root = Constants.MAPPER.createObjectNode();
		root.put("minLength", minLength);
		root.put("historySize", historySize);
		root.put("minClassesNum", minClassesNum);
		root.put("maxAge", maxAge);
		root.put("denySequences", denySequences);
		try
		{
			return Constants.MAPPER.writeValueAsString(root);
		} catch (JsonProcessingException e)
		{
			throw new RuntimeEngineException("Can't serialize password credential configuration to JSON", e);
		}
	}

	@Override
	public void setSerializedConfiguration(String json)
	{
		JsonNode root;
		try
		{
			root = Constants.MAPPER.readTree(json);
		} catch (Exception e)
		{
			throw new RuntimeEngineException("Can't deserialize password credential configuration " +
					"from JSON", e);
		}
		minLength = root.get("minLength").asInt();
		historySize = root.get("historySize").asInt();
		minClassesNum = root.get("minClassesNum").asInt();
		maxAge = root.get("maxAge").asLong();
		denySequences = root.get("denySequences").asBoolean();
	}


	@Override
	public String prepareCredential(String rawCredential, String currentCredential)
			throws IllegalCredentialException
	{
		Deque<PasswordInfo> currentPasswords = parseDbCredential(currentCredential);
		verifyNewPassword(rawCredential, currentPasswords);

		String salt = random.nextInt() + "";
		byte[] hashed = hash(rawCredential, salt);

		PasswordInfo currentPassword = new PasswordInfo(hashed, salt);
		if (historySize <= currentPasswords.size())
			currentPasswords.removeLast();
		currentPasswords.addFirst(currentPassword);

		ObjectNode root = Constants.MAPPER.createObjectNode();
		ArrayNode passwords = root.putArray("passwords");
		for (PasswordInfo pi: currentPasswords)
		{
			ObjectNode entry = passwords.addObject();
			entry.put("hash", pi.getHash());
			entry.put("salt", pi.getSalt());
			entry.put("time", pi.getTime().getTime());
		}
		try
		{
			return Constants.MAPPER.writeValueAsString(root);
		} catch (JsonProcessingException e)
		{
			throw new RuntimeEngineException("Can't serialize password credential to JSON", e);
		}
	}

	private Deque<PasswordInfo> parseDbCredential(String raw)
	{
		if (raw == null || raw.length() == 0)
			return new LinkedList<PasswordInfo>();
		JsonNode root;
		try
		{
			root = Constants.MAPPER.readTree(raw);

			JsonNode passwords = root.get("passwords");
			Deque<PasswordInfo> ret = new LinkedList<PasswordInfo>();
			for (int i=0; i<passwords.size(); i++)
			{
				JsonNode rawPasswd = passwords.get(i);
				ret.add(new PasswordInfo(rawPasswd.get("hash").binaryValue(),
						rawPasswd.get("salt").asText(),
						rawPasswd.get("time").asLong()));
			}
			return ret;
		} catch (Exception e)
		{
			throw new RuntimeEngineException("Can't deserialize password credential from JSON", e);
		}

	}


	@Override
	public LocalCredentialState checkCredentialState(String currentCredential)
	{
		Deque<PasswordInfo> currentPasswords = parseDbCredential(currentCredential);
		if (currentPasswords.isEmpty())
			return LocalCredentialState.notSet;
		PasswordInfo currentPassword = currentPasswords.getFirst();
		Date validityEnd = new Date(currentPassword.getTime().getTime() + maxAge);
		if (new Date().after(validityEnd))
			return LocalCredentialState.outdated;
		return LocalCredentialState.correct;
	}

	@Override
	public AuthenticatedEntity checkPassword(String username, String password)
			throws IllegalIdentityValueException, IllegalCredentialException
	{
		EntityWithCredential resolved = identityResolver.resolveIdentity(username, 
				IDENTITY_TYPES, credentialName);
		String dbCredential = resolved.getCredentialValue();
		Deque<PasswordInfo> credentials = parseDbCredential(dbCredential);
		if (credentials.isEmpty())
			throw new IllegalCredentialException("The entity has no password set");
		PasswordInfo current = credentials.getFirst();
		byte[] testedHash = hash(password, current.getSalt());
		if (!Arrays.areEqual(testedHash, current.getHash()))
			throw new IllegalCredentialException("The password is incorrect");
		return new AuthenticatedEntity(resolved.getEntityId(), resolved.getLocalAuthnState(),
				username);
	}

	private void verifyNewPassword(String password, Deque<PasswordInfo> currentCredentials)
	{
		PasswordValidator validator = getPasswordValidator();
		
		RuleResult result = validator.validate(new PasswordData(new Password(password)));
		//TODO when i18n is added this should be extended to provide better information.
		if (!result.isValid())
			throw new IllegalCredentialException("Password is too weak");
		
		for (PasswordInfo pi: currentCredentials)
		{
			byte[] newHashed = hash(password, pi.getSalt());
			if (Arrays.areEqual(newHashed, pi.getHash()))
				throw new IllegalCredentialException("The same password was recently used");
		}
	}

	private PasswordValidator getPasswordValidator()
	{
		List<Rule> ruleList = new ArrayList<Rule>();
		ruleList.add(new LengthRule(minLength, 512));
		CharacterCharacteristicsRule charRule = new CharacterCharacteristicsRule();
		charRule.getRules().add(new DigitCharacterRule(1));
		charRule.getRules().add(new NonAlphanumericCharacterRule(1));
		charRule.getRules().add(new UppercaseCharacterRule(1));
		charRule.getRules().add(new LowercaseCharacterRule(1));
		charRule.setNumberOfCharacteristics(minClassesNum);
		ruleList.add(charRule);
		if (denySequences)
		{
			ruleList.add(new AlphabeticalSequenceRule());
			ruleList.add(new NumericalSequenceRule(3, true));
			ruleList.add(new QwertySequenceRule());
			ruleList.add(new RepeatCharacterRegexRule(4));
		}
		return new PasswordValidator(ruleList);
	}

	private byte[] hash(String password, String salt)
	{
		SHA256Digest digest = new SHA256Digest();
		int size = digest.getDigestSize();
		byte[] salted = (salt+password).getBytes(Constants.UTF);
		digest.update(salted, 0, salted.length);
		byte[] hashed = new byte[size];
		digest.doFinal(hashed, 0);
		return hashed;
	}

	
	/**
	 * In DB representation of the credential state is a list of objects as the one described in this class.
	 * @author K. Benedyczak
	 */
	private static class PasswordInfo
	{
		private byte[] hash;
		private String salt;
		private Date time;

		public PasswordInfo(byte[] hash, String salt)
		{
			this.hash = hash;
			this.salt = salt;
			this.time = new Date();
		}
		public PasswordInfo(byte[] hash, String salt, long time)
		{
			this.hash = hash;
			this.salt = salt;
			this.time = new Date(time);
		}
		public byte[] getHash()
		{
			return hash;
		}
		public String getSalt()
		{
			return salt;
		}
		public Date getTime()
		{
			return time;
		}
	}
}





