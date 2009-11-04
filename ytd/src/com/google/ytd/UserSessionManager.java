/* Copyright (c) 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ytd;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gdata.client.http.AuthSubUtil;
import com.google.gdata.util.AuthenticationException;
import com.google.ytd.model.UserSession;

/**
 * Class that manages UserSession objects.
 */
public class UserSessionManager {
  private static final String USER_SESSION_ID_NAME = "YAW_SESSION_ID";

  public static void sendSessionIdCookie(String sessionId, HttpServletResponse response) {
    Cookie cookie = new Cookie(USER_SESSION_ID_NAME, sessionId);
    // cookie lives for a year
    cookie.setMaxAge(31536000);
    response.addCookie(cookie);
  }

  public static void destroySessionIdCookie(HttpServletResponse response) {
    Cookie cookie = new Cookie(USER_SESSION_ID_NAME, "");
    cookie.setMaxAge(0);
    response.addCookie(cookie);
  }

  public static boolean isSessionValid(UserSession session) {

    boolean valid = true;

    String authSubToken = session.getMetaData("authSubToken");

    if (authSubToken != null) {

      try {
        AuthSubUtil.getTokenInfo(authSubToken, null);
      } catch (AuthenticationException e) {
        valid = false;
      } catch (IOException e) {
        valid = false;
      } catch (GeneralSecurityException e) {
        valid = false;
      }
    } else {
      valid = false;

    }
    return valid;
  }

  public static UserSession save(UserSession session) {
    return (UserSession) Util.persistJdo(session);
  }

  public static void delete(UserSession session) {
    Util.removeJdo(session);
  }

  public static UserSession getUserSession(HttpServletRequest request) {

    UserSession userSession = null;

    Cookie[] cookies = request.getCookies();

    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if (USER_SESSION_ID_NAME.equals(cookie.getName())) {
          String sessionId = cookie.getValue();
          PersistenceManager pm = Util.getPersistenceManagerFactory().getPersistenceManager();

          String filters = "id == id_";
          Query query = pm.newQuery(UserSession.class, filters);
          query.declareParameters("String id_");
          List<UserSession> list = (List<UserSession>) query
              .executeWithArray(new Object[] { sessionId });

          if (list.size() > 0) {
            userSession = list.get(0);
            userSession = pm.detachCopy(userSession);
          }

          pm.close();
        }
      }
    }

    return userSession;
  }

}