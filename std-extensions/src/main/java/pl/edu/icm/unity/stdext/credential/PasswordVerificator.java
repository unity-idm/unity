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
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.exceptions.IllegalAttributeValueException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.exceptions.IllegalGroupValueException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.server.authn.AbstractLocalVerificator;
import pl.edu.icm.unity.server.authn.AuthenticatedEntity;
import pl.edu.icm.unity.server.authn.EntityWithCredential;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.stdext.utils.CryptoUtils;
import pl.edu.icm.unity.types.authn.CredentialPublicInformation;
import pl.edu.icm.unity.types.authn.LocalCredentialState;

/**
 * Ordinary password credential. Highly configurable: it is possible to set minimal length,
 * what character classes are required, minimum number of character classes, how many previous passwords 
 * should be stored and not repeated after change, how often the password must be changed.
 * <p>
 * Additionally configuration of the credential may allow for password reset feature. Then are stored
 * email verification settings, security question settings, maximum number confirmation code re-sends and 
 * confirmation code length.
 * 
 * @author K. Benedyczak
 */
public class PasswordVerificator extends AbstractLocalVerificator implements PasswordExchange
{ 	
	private static final String[] IDENTITY_TYPES = {UsernameIdentity.ID};
	//200 years should be enough, Long MAX is too much as we would fail on maths
	public static final long MAX_AGE_UNDEF = 200*12*30*24*3600000; 
	private Random random = new Random();
	private int minLength = 8;
	private int historySize = 0;
	private int minClassesNum = 3;
	private boolean denySequences = true;
	private long maxAge = MAX_AGE_UNDEF; 
	private CredentialResetSettings passwordResetSettings = new CredentialResetSettings();

	public PasswordVerificator(String name, String description)
	{
		super(name, description, PasswordExchange.ID, true);
	}

	@Override
	public String getSerializedConfiguration() throws InternalException
	{
		ObjectNode root = Constants.MAPPER.createObjectNode();
		root.put("minLength", minLength);
		root.put("historySize", historySize);
		root.put("minClassesNum", minClassesNum);
		root.put("maxAge", maxAge);
		root.put("denySequences", denySequences);
		
		ObjectNode resetNode = root.putObject("resetSettings");
		passwordResetSettings.serializeTo(resetNode);
		
		try
		{
			return Constants.MAPPER.writeValueAsString(root);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't serialize password credential configuration to JSON", e);
		}
	}

	@Override
	public void setSerializedConfiguration(String json) throws InternalException
	{
		JsonNode root;
		try
		{
			root = Constants.MAPPER.readTree(json);
		} catch (Exception e)
		{
			throw new InternalException("Can't deserialize password credential configuration " +
					"from JSON", e);
		}
		minLength = root.get("minLength").asInt();
		if (minLength <= 0 || minLength > 100)
			throw new InternalException("Minimal password length must be in range [1-100]");
		historySize = root.get("historySize").asInt();
		if (historySize < 0 || historySize > 1000)
			throw new InternalException("History size must be in range [0-1000]");
		minClassesNum = root.get("minClassesNum").asInt();
		if (minClassesNum <= 0 || minClassesNum > 4)
			throw new InternalException("Minimum classes number must be in range [1-4]");
		maxAge = root.get("maxAge").asLong();
		if (maxAge <= 0)
			throw new InternalException("Maximum age must be positive");
		denySequences = root.get("denySequences").asBoolean();
		JsonNode resetNode = root.get("resetSettings");
		if (resetNode != null)
			passwordResetSettings.deserializeFrom((ObjectNode) resetNode);
	}

	/**
	 * The rawCredential must be in JSON format, see {@link PasswordToken} for details.
	 */
	@Override
	public String prepareCredential(String rawCredential, String currentCredential)
			throws IllegalCredentialException, InternalException
	{
		Deque<PasswordInfo> currentPasswords = parseDbCredential(currentCredential).getPasswords();
		
		PasswordToken pToken = PasswordToken.loadFromJson(rawCredential);
		
		verifyNewPassword(pToken.getExistingPassword(), pToken.getPassword(), currentPasswords);
		
		if (passwordResetSettings.isEnabled() && passwordResetSettings.isRequireSecurityQuestion())
		{
			if (pToken.getAnswer() == null || pToken.getQuestion() == -1)
				throw new IllegalCredentialException("The credential must select a security question " +
						"and provide an answer for it");
			if (pToken.getQuestion() < 0 || pToken.getQuestion() >= 
					passwordResetSettings.getQuestions().size())
				throw new IllegalCredentialException("The chosen answer for security question is invalid");
		}
		
		
		String salt = random.nextInt() + "";
		byte[] hashed = CryptoUtils.hash(pToken.getPassword(), salt);

		PasswordInfo currentPassword = new PasswordInfo(hashed, salt);
		if (historySize <= currentPasswords.size() && !currentPasswords.isEmpty())
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
		root.put("outdated", false);
		if (passwordResetSettings.isEnabled() && passwordResetSettings.isRequireSecurityQuestion())
		{
			String question = passwordResetSettings.getQuestions().get(pToken.getQuestion());
			root.put("question", question);
			root.put("answerHash", CryptoUtils.hash(pToken.getAnswer().toLowerCase(), question));
		}
		root.put("resets", 0);
		try
		{
			return Constants.MAPPER.writeValueAsString(root);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't serialize password credential to JSON", e);
		}
	}


	@Override
	public String invalidate(String currentCredential)
	{
		ObjectNode root;
		try
		{
			root = (ObjectNode) Constants.MAPPER.readTree(currentCredential);
			root.put("outdated", true);
			return Constants.MAPPER.writeValueAsString(root);
		} catch (Exception e)
		{
			throw new InternalException("Can't deserialize password credential from JSON", e);
		}
	}
	
	private CredentialDBState parseDbCredential(String raw) throws InternalException
	{
		if (raw == null || raw.length() == 0)
			return new CredentialDBState(new LinkedList<PasswordInfo>(), false, null, null, 0);
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
			boolean outdated = root.get("outdated").asBoolean();
			JsonNode qn = root.get("question");
			String question = qn == null ? null : qn.asText();
			JsonNode an = root.get("answerHash");
			byte[] answerHash = an == null ? null : an.binaryValue();
			JsonNode rn = root.get("resets");
			int resetTries = rn == null ? 0 : rn.asInt();
			return new CredentialDBState(ret, outdated, question, answerHash, resetTries);
		} catch (Exception e)
		{
			throw new InternalException("Can't deserialize password credential from JSON", e);
		}

	}


	@Override
	public CredentialPublicInformation checkCredentialState(String currentCredential) throws InternalException
	{
		CredentialDBState parsedCred = parseDbCredential(currentCredential);
		Deque<PasswordInfo> currentPasswords = parsedCred.getPasswords();
		if (currentPasswords.isEmpty())
			return new CredentialPublicInformation(LocalCredentialState.notSet, "");
		
		PasswordInfo currentPassword = currentPasswords.getFirst();
		PasswordExtraInfo pei = new PasswordExtraInfo(currentPassword.getTime(), 
				parsedCred.getSecurityQuestion(), parsedCred.getResetTries());
		String extraInfo = pei.toJson();
		
		if (isCurrentCredentialOutdated(parsedCred))
			return new CredentialPublicInformation(LocalCredentialState.outdated, extraInfo);
		return new CredentialPublicInformation(LocalCredentialState.correct, extraInfo);
	}

	/**
	 * Checks if the provided password is valid. If it is then it is checked if it is still 
	 * fulfilling all actual rules of the credential's configuration. If not then it is returned that 
	 * credential state is outdated. 
	 */
	@Override
	public AuthenticatedEntity checkPassword(String username, String password) throws EngineException
	{
		EntityWithCredential resolved = identityResolver.resolveIdentity(username, 
				IDENTITY_TYPES, credentialName);
		String dbCredential = resolved.getCredentialValue();
		CredentialDBState credState = parseDbCredential(dbCredential);
		Deque<PasswordInfo> credentials = credState.getPasswords();
		if (credentials.isEmpty())
			throw new IllegalCredentialException("The entity has no password set");
		PasswordInfo current = credentials.getFirst();
		byte[] testedHash = CryptoUtils.hash(password, current.getSalt());
		if (!Arrays.areEqual(testedHash, current.getHash()))
			throw new IllegalCredentialException("The password is incorrect");
		boolean isOutdated = isCurrentPasswordOutdated(password, credState, resolved);
		return new AuthenticatedEntity(resolved.getEntityId(), username, isOutdated);
	}


	@Override
	public CredentialResetSettings getCredentialResetSettings()
	{
		return passwordResetSettings;
	}

	/**
	 * As {@link #isCurrentCredentialOutdated(CredentialDBState)} but also
	 * checks if the provided password is fulfilling the actual rules of the credential
	 * (what can not be checked with the hashed in-db version of the credential).
	 * <p>
	 * Additionally, if it is detected that the credential is outdated by checking the password, 
	 * this newly received information is stored in DB: credential is updated to be manually outdated.
	 *   
	 * @param password
	 * @throws IllegalGroupValueException 
	 * @throws IllegalAttributeTypeException 
	 * @throws IllegalTypeException 
	 * @throws IllegalAttributeValueException 
	 */
	private boolean isCurrentPasswordOutdated(String password, CredentialDBState credState, 
			EntityWithCredential resolved) throws IllegalAttributeValueException, IllegalTypeException, 
			IllegalAttributeTypeException, IllegalGroupValueException
	{
		if (isCurrentCredentialOutdated(credState))
			return true;

		PasswordValidator validator = getPasswordValidator();
		RuleResult result = validator.validate(new PasswordData(new Password(password)));
		if (!result.isValid())
		{
			String invalidated = invalidate(resolved.getCredentialValue());
			identityResolver.updateCredential(resolved.getEntityId(), resolved.getCredentialName(), 
						invalidated);
			return true;
		}
		return false;
	}

	/**
	 * Checks if the provided credential state is fulfilling the rules of the credential,
	 * if it was expired manually and if password did expire on its own.  
	 *  
	 * @param password
	 * @return true if credential is invalid
	 */
	private boolean isCurrentCredentialOutdated(CredentialDBState credState)
	{
		if (credState.isOutdated())
			return true;
		if (credState.getSecurityQuestion() == null && 
				passwordResetSettings.isEnabled() && 
				passwordResetSettings.isRequireSecurityQuestion())
			return true;
		PasswordInfo current = credState.getPasswords().getFirst();
		Date validityEnd = new Date(current.getTime().getTime() + maxAge);
		if (new Date().after(validityEnd))
			return true;
		return false;
	}
	
	private void verifyNewPassword(String existingPassword, String password, 
			Deque<PasswordInfo> currentCredentials) throws IllegalCredentialException
	{
		PasswordValidator validator = getPasswordValidator();
		
		RuleResult result = validator.validate(new PasswordData(new Password(password)));
		//TODO when i18n is added this should be extended to provide better information.
		if (!result.isValid())
			throw new IllegalCredentialException("Password is too weak");
		
		for (PasswordInfo pi: currentCredentials)
		{
			byte[] newHashed = CryptoUtils.hash(password, pi.getSalt());
			if (Arrays.areEqual(newHashed, pi.getHash()))
				throw new IllegalCredentialException("The same password was recently used");
		}
		/*
		if (!currentCredentials.isEmpty())
		{
			PasswordInfo cur = currentCredentials.getFirst();
			byte[] existingHashed = hash(existingPassword, cur.getSalt());
			if (!Arrays.areEqual(existingHashed, cur.getHash()))
				throw new IllegalCredentialException("The current password value is wrong");
		}
		*/
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

	public int getMinLength()
	{
		return minLength;
	}

	public void setMinLength(int minLength)
	{
		this.minLength = minLength;
	}

	public int getHistorySize()
	{
		return historySize;
	}

	public void setHistorySize(int historySize)
	{
		this.historySize = historySize;
	}

	public int getMinClassesNum()
	{
		return minClassesNum;
	}

	public void setMinClassesNum(int minClassesNum)
	{
		this.minClassesNum = minClassesNum;
	}

	public boolean isDenySequences()
	{
		return denySequences;
	}

	public void setDenySequences(boolean denySequences)
	{
		this.denySequences = denySequences;
	}

	public long getMaxAge()
	{
		return maxAge;
	}

	public void setMaxAge(long maxAge)
	{
		this.maxAge = maxAge;
	}

	public CredentialResetSettings getPasswordResetSettings()
	{
		return passwordResetSettings;
	}

	public void setPasswordResetSettings(CredentialResetSettings passwordResetSettings)
	{
		this.passwordResetSettings = passwordResetSettings;
	}



	/**
	 * In DB representation of the credential state.
	 * @author K. Benedyczak
	 */
	private static class CredentialDBState
	{
		private Deque<PasswordInfo> passwords;
		private boolean outdated;
		private String securityQuestion;
		private byte[] answerHash;
		private int resetTries;

		public CredentialDBState(Deque<PasswordInfo> passwords, boolean outdated,
				String securityQuestion, byte[] answerHash, int resetTries)
		{
			this.passwords = passwords;
			this.outdated = outdated;
			this.securityQuestion = securityQuestion;
			this.answerHash = answerHash;
			this.resetTries = resetTries;
		}
		public Deque<PasswordInfo> getPasswords()
		{
			return passwords;
		}
		public boolean isOutdated()
		{
			return outdated;
		}
		public String getSecurityQuestion()
		{
			return securityQuestion;
		}
		public byte[] getAnswerHash()
		{
			return answerHash;
		}
		public int getResetTries()
		{
			return resetTries;
		}
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





