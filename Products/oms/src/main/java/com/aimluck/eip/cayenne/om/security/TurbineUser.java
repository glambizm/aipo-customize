/*
 * Aipo is a groupware program developed by TOWN, Inc.
 * Copyright (C) 2004-2015 TOWN, Inc.
 * http://www.aipo.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aimluck.eip.cayenne.om.security;

import org.apache.cayenne.ObjectId;

import com.aimluck.eip.cayenne.om.security.auto._TurbineUser;

public class TurbineUser extends _TurbineUser {

  public static final String LOGIN_NAME_COLUMN = "LOGIN_NAME";

  public static final String FIRST_NAME_COLUMN = "FIRST_NAME";

  public static final String LAST_NAME_COLUMN = "LAST_NAME";

  public static final String EMAIL_COLUMN = "EMAIL";

  public static final String CELLULAR_MAIL_COLUMN = "CELLULAR_MAIL";

  public static final String PHOTO_COLUMN = "PHOTO";

  public static final String HAS_PHOTO_COLUMN = "HAS_PHOTO";

  public static final String PHOTO_MODIFIED_COLUMN = "PHOTO_MODIFIED";

  public static final String PHOTO_SMARTPHONE_COLUMN = "PHOTO_SMARTPHONE";

  public static final String HAS_PHOTO_SMARTPHONE_COLUMN =
    "HAS_PHOTO_SMARTPHONE";

  public static final String PHOTO_MODIFIED_SMARTPHONE_COLUMN =
    "PHOTO_MODIFIED_SMARTPHONE";

// <#01> --- S
  public static final String EMPLOYEE_ROLL_MARK_ADMIN = "$";

  public static final String EMPLOYEE_MSGBOARD_CATEGORY_MARK_EMPLOYEE = "[";

  public static final String EMPLOYEE_NAME_MARK_OTHER = "#";
  public static final String EMPLOYEE_ROLL_MARK_OTHER = "#";

  public static final Integer EMPLOYEE_TYPE_EMPLOYEE = 101;

  public static final Integer EMPLOYEE_TYPE_OTHER = -101;
// <#01> --- E

  public Integer getUserId() {
    if (getObjectId() != null && !getObjectId().isTemporary()) {
      Object obj = getObjectId().getIdSnapshot().get(USER_ID_PK_COLUMN);
      if (obj instanceof Long) {
        Long value = (Long) obj;
        return Integer.valueOf(value.intValue());
      } else {
        return (Integer) obj;
      }
    } else {
      return null;
    }
  }

  public void setUserId(String id) {
    setObjectId(new ObjectId("TurbineUser", USER_ID_PK_COLUMN, Integer
      .valueOf(id)));
  }

// <#01> --- S
  public Integer getEmployeeType() {
    String firstChar = getLastName().substring(0, 1);
    
    if (firstChar.equals(EMPLOYEE_NAME_MARK_OTHER)) {
      // 社員以外
      return EMPLOYEE_TYPE_OTHER;
    } else {
      // 社員
      return EMPLOYEE_TYPE_EMPLOYEE;
    }
  }
// <#01> --- E
}
