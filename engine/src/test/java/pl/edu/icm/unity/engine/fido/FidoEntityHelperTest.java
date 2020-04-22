/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.fido;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.FidoException;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.identity.UserHandleIdentity;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityInformation;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.UserHandle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Test class for {@link FidoEntityHelper}.
 *
 * @author R. Ledzinski
 */
public class FidoEntityHelperTest
{

    private static final String USERNAME = "username";
    private static final String EMAIL = "user@example.com";
    private static final String DISPLAY_NAME = "User display name";
    private static final UserHandle USER_HANDLE = UserHandle.create();
    private static final Identity USERHANDLE_IDENTITY = new Identity(UserHandleIdentity.ID, USER_HANDLE.asString(), 1, USER_HANDLE.asString());
    private static final EntityParam USERHANDLE_IDENTITY_PARAMS = new EntityParam(new IdentityParam(UserHandleIdentity.ID, USERHANDLE_IDENTITY.getValue()));
    private static final EntityParam USERNAME_IDENTITY_PARAMS = new EntityParam(new IdentityParam(UsernameIdentity.ID, USERNAME));
    private static final EntityParam EMAIL_IDENTITY_PARAMS = new EntityParam(new IdentityParam(EmailIdentity.ID, "user@example.com"));
    private static final Entity entity = new Entity(
            Arrays.asList(new Identity(UsernameIdentity.ID, USERNAME, 1, USERNAME),
                    new Identity(EmailIdentity.ID, EMAIL, 1, EMAIL),
                    USERHANDLE_IDENTITY),
            new EntityInformation(1L),
            null);

    @Mock
    private EntityManagement entityMan;

    @Mock
    private UnityMessageSource msg;

    private FidoEntityHelper helper;

    @Before
    public void setup() throws EngineException
    {
        initMocks(this);
        helper = new FidoEntityHelper(entityMan, msg);

        when(entityMan.getEntity(eq(USERHANDLE_IDENTITY_PARAMS))).thenReturn(entity);
        when(entityMan.getEntity(eq(USERNAME_IDENTITY_PARAMS))).thenReturn(entity);
        when(entityMan.getEntity(eq(EMAIL_IDENTITY_PARAMS))).thenReturn(entity);
        when(entityMan.getEntity(eq(new EntityParam(1L)))).thenReturn(entity);
        when(entityMan.getEntityLabel(eq(new EntityParam(1L)))).thenReturn(DISPLAY_NAME);
    }

    @Test
    public void shouldReturnDisplayName() throws FidoException
    {
        //given/when
        String dn = helper.getDisplayName(entity);
        //then
        assertEquals(DISPLAY_NAME, dn);
    }

    @Test
    public void shouldReturnDefaultDisplayName() throws FidoException
    {
        //given/when
        String dn = helper.getDisplayName(new Entity(null, new EntityInformation(2L), null));
        //then
        assertEquals("Entity [2]", dn);
    }

    @Test
    public void shouldReturnUsernameForEmailIdentity() throws EngineException
    {
        //given
        Entity entity = new Entity(
                Collections.singletonList(new Identity(EmailIdentity.ID, EMAIL, 1, EMAIL)),
                new EntityInformation(1L),
                null);

        //when
        String un = helper.getUsername(entity);

        // then
        assertEquals(EMAIL, un);
    }

    @Test
    public void shouldReturnUsernameForTwoIdentity() throws EngineException
    {
        //given/when
        String un = helper.getUsername(entity);

        // then
        assertEquals(USERNAME, un);
    }

    @Test
    public void shouldReturnUserHandle() throws EngineException
    {
        //given/when
        String uh = helper.getUserHandle(entity);

        // then
        assertEquals(USERHANDLE_IDENTITY.getValue(), uh);
    }

    @Test(expected = FidoException.class)
    public void shouldThrowForMissingUserHandle() throws EngineException
    {
        //given
        Entity entity = new Entity(
                Collections.emptyList(),
                new EntityInformation(1L),
                null);
        // when
        helper.getUserHandle(entity);
    }

    @Test
    public void shouldCreateUserHandle() throws EngineException
    {
        //given
        Entity entity = new Entity(
                new ArrayList<Identity>(Collections.singletonList(new Identity(EmailIdentity.ID, EMAIL, 1, EMAIL))),
                new EntityInformation(1L),
                null);
        UserHandle userHandle = UserHandle.create();
        when(entityMan.addIdentity(any(), any())).thenReturn(new Identity(UserHandleIdentity.ID, userHandle.asString(), 1, userHandle.asString()));

        //when
        String uh = helper.getOrCreateUserHandle(entity);

        // then
        verify(entityMan).addIdentity(any(), any());
        assertEquals(userHandle.asString(), uh);
        assertTrue(entity.getIdentities().stream().anyMatch(i -> i.getValue().equals(uh)));
    }

    @Test
    public void shouldNotCreateUserHandleWhenExists() throws EngineException
    {
        //given/when
        String uh = helper.getOrCreateUserHandle(entity);

        // then
        verify(entityMan, never()).addIdentity(any(), any());
        assertEquals(USER_HANDLE.asString(), uh);
    }

    @Test(expected = FidoException.class)
    public void shouldThrowExceptionForMissingIdentities() throws EngineException
    {
        //given
        Entity entity = new Entity(
                Collections.emptyList(),
                new EntityInformation(1L),
                null);

        //when
        helper.getUsername(entity);
    }

    @Test
    public void shouldGetUserHandleForUsername() throws EngineException
    {
        //given/when
        Optional<String> uh = helper.getUserHandleForUsername(USERNAME);

        // then
        assertEquals(USERHANDLE_IDENTITY.getValue(), uh.get());
    }

    @Test
    public void shouldGetUsernameForUserHandle() throws EngineException
    {
        //given/when
        Optional<String> un = helper.getUsernameForUserHandle(USERHANDLE_IDENTITY.getValue());

        // then
        assertEquals(USERNAME, un.get());
    }

    @Test
    public void shouldFindEntityByUserHandle() throws EngineException
    {
        //given/when
        Entity entity = helper.getEntityByUserHandle(USERHANDLE_IDENTITY.getValue());

        // then
        assertEquals(1, (long) entity.getId());
    }

    @Test
    public void shouldFindEntityByUsername() throws EngineException
    {
        //given/when
        Entity entity = helper.getEntityByUsername(USERNAME);

        // then
        assertEquals(1, (long) entity.getId());
    }

    @Test
    public void shouldFindEntityByEmail() throws EngineException
    {
        //given/when
        Entity entity = helper.getEntityByUsername(EMAIL);

        // then
        assertEquals(1, (long) entity.getId());
    }

    @Test
    public void shouldFindEntityByUsernameOrEntityId() throws EngineException
    {
        //given/when
        Entity entity = helper.getEntityOrThrow(1L, null);

        // then
        assertEquals(1, (long) entity.getId());
    }

    @Test
    public void shouldFindEntityByUsernameOrEntityId2() throws EngineException
    {
        //given/when
        Entity entity = helper.getEntityOrThrow(null, USERNAME);

        // then
        assertEquals(1, (long) entity.getId());
    }

    @Test(expected = FidoException.class)
    public void shouldThrowWhenMissingEntity() throws EngineException
    {
        //given/when
        Entity entity = helper.getEntityOrThrow(2L, USERNAME);
    }
}
