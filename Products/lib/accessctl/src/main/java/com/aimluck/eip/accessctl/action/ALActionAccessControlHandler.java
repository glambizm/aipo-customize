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
package com.aimluck.eip.accessctl.action;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.DataRow;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.turbine.util.RunData;

import com.aimluck.eip.cayenne.om.account.EipTAclPortletFeature;
import com.aimluck.eip.cayenne.om.account.EipTAclRole;
import com.aimluck.eip.cayenne.om.account.EipTAclUserRoleMap;
import com.aimluck.eip.cayenne.om.security.TurbineUser;
import com.aimluck.eip.common.ALEipManager;
import com.aimluck.eip.common.ALEipUser;
import com.aimluck.eip.orm.Database;
import com.aimluck.eip.orm.query.SQLTemplate;
import com.aimluck.eip.services.accessctl.ALAccessControlConstants;
import com.aimluck.eip.services.accessctl.ALAccessControlHandler;
import com.aimluck.eip.util.ALEipUtils;                                         // <#01>

public class ALActionAccessControlHandler extends ALAccessControlHandler {

  @SuppressWarnings("unused")
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(ALActionAccessControlHandler.class.getName());

  @SuppressWarnings("unchecked")
  @Override
  public boolean hasAuthority(int userId, String featureName, int aclType) {

    Map<String, EipTAclRole> roleMap =
      ALEipManager.getInstance().getAclRoleMap(userId);
    EipTAclRole role = roleMap.get(featureName);

    if (role == null) {
      return false;
    }

    int dbAclType = role.getAclType().intValue();

    return ((dbAclType & aclType) == aclType);
  }

  @Override
  public List<Integer> getAcceptUserIdsExceptLoginUser(int uid, String feat,
      int acl_type) {
    StringBuffer sb = new StringBuffer();
    sb.append("SELECT ");
    sb.append(TurbineUser.USER_ID_PK_COLUMN);
    sb.append(" FROM turbine_user WHERE (");
    sb.append(TurbineUser.USER_ID_PK_COLUMN);
    sb.append(" IN (SELECT ");
    sb.append(TurbineUser.USER_ID_PK_COLUMN);
    sb.append(" FROM eip_t_acl_user_role_map WHERE ");
    sb.append(EipTAclUserRoleMap.ROLE_ID_COLUMN);
    sb.append(" IN (SELECT ");
    sb.append(EipTAclUserRoleMap.ROLE_ID_COLUMN);
    sb.append(" FROM eip_t_acl_role WHERE ((");
    sb.append(EipTAclRole.ACL_TYPE_COLUMN);
    sb.append(" & ");
    sb.append(Integer.toString(acl_type));
    sb.append(") = ");
    sb.append(Integer.toString(acl_type));
    sb.append(") AND (");
    sb.append(EipTAclPortletFeature.FEATURE_ID_PK_COLUMN);
    sb.append(" IN (SELECT ");
    sb.append(EipTAclPortletFeature.FEATURE_ID_PK_COLUMN);
    sb.append(" FROM eip_t_acl_portlet_feature WHERE ");
    sb.append(EipTAclPortletFeature.FEATURE_NAME_COLUMN);
    sb.append("='");
    sb.append(feat.trim());
    sb.append("'))))) AND (");
    sb.append(TurbineUser.USER_ID_PK_COLUMN);
    sb.append(" != ");
    sb.append(Integer.toString(uid));
    sb.append(")");

    List<Integer> userIds = new ArrayList<Integer>();
    String sqlString = sb.toString();

    List<TurbineUser> list =
      Database.sql(TurbineUser.class, sqlString).fetchList();

    for (TurbineUser tuser : list) {
      userIds.add(tuser.getUserId());
    }
    return userIds;
  }

  @Override
  public List<Integer> getAcceptUserIdsInListExceptLoginUser(int uid,
      String feat, int acl_type, List<ALEipUser> ulist) {
    List<Integer> userIds = new ArrayList<Integer>();
    int u_size;
    if ((ulist == null) || (u_size = ulist.size()) < 1) {
      return userIds;
    }

    StringBuffer sb = new StringBuffer();
    sb.append("SELECT ");
    sb.append(TurbineUser.USER_ID_PK_COLUMN);
    sb.append(" FROM turbine_user WHERE (");
    sb.append(TurbineUser.USER_ID_PK_COLUMN);
    sb.append(" IN (SELECT ");
    sb.append(TurbineUser.USER_ID_PK_COLUMN);
    sb.append(" FROM turbine_user_group_role WHERE ");
    sb.append(EipTAclUserRoleMap.ROLE_ID_COLUMN);
    sb.append(" IN (SELECT ");
    sb.append(EipTAclUserRoleMap.ROLE_ID_COLUMN);
    sb.append(" FROM eip_t_acl_role WHERE ((");
    sb.append(EipTAclRole.ACL_TYPE_COLUMN);
    sb.append(" & ");
    sb.append("#bind($aclType)");
    sb.append(") = ");
    sb.append("#bind($aclType)");
    sb.append(") AND (");
    sb.append(EipTAclPortletFeature.FEATURE_ID_PK_COLUMN);
    sb.append(" IN (SELECT ");
    sb.append(EipTAclPortletFeature.FEATURE_ID_PK_COLUMN);
    sb.append(" FROM eip_t_acl_portlet_feature WHERE ");
    sb.append(EipTAclPortletFeature.FEATURE_NAME_COLUMN);
    sb.append("= ");
    sb.append("#bind($feat)");
    sb.append("))))) AND (");
    sb.append(TurbineUser.USER_ID_PK_COLUMN);
    sb.append(" in (");

    for (int i = 0; i < u_size; i++) {
      sb.append("#bind($").append(Integer.toString(i)).append(")");
      if (i + 1 < u_size) {
        sb.append(",");
      }
    }

    sb.append(")) AND (");
    sb.append(TurbineUser.USER_ID_PK_COLUMN);
    sb.append(" != ");
    sb.append("#bind($uid)");
    sb.append(")");

    String sqlString = sb.toString();

    SQLTemplate<TurbineUser> template =
      Database.sql(TurbineUser.class, sqlString);
    template.param("aclType", Integer.valueOf(acl_type));
    template.param("feat", feat.trim());
    template.param("uid", Integer.valueOf(uid));

    for (int i = 0; i < u_size; i++) {
      ALEipUser member = ulist.get(i);
      template.param(Integer.toString(i), Integer.valueOf((int) member
        .getUserId()
        .getValue()));
    }

    List<TurbineUser> list = template.fetchList();

    for (TurbineUser tuser : list) {
      userIds.add(tuser.getUserId());
    }
    return userIds;
  }

  @Override
  public List<TurbineUser> getAuthorityUsersFromGroup(RunData rundata,
      String feat, String groupname, boolean includeLoginuser) {

    int listNumber = ALAccessControlConstants.VALUE_ACL_LIST;
    int detailNumber = ALAccessControlConstants.VALUE_ACL_DETAIL;
    int updateNumber = ALAccessControlConstants.VALUE_ACL_UPDATE;

    int aclNumber = listNumber | detailNumber | updateNumber;

    StringBuffer sb = new StringBuffer();

    sb.append("(SELECT ");
    sb.append(TurbineUser.USER_ID_PK_COLUMN);
    sb.append(" FROM eip_t_acl_user_role_map WHERE ");
    sb.append(EipTAclUserRoleMap.ROLE_ID_COLUMN);
    sb.append(" IN (SELECT ");
    sb.append(EipTAclUserRoleMap.ROLE_ID_COLUMN);
    sb.append(" FROM eip_t_acl_role WHERE ((");
    sb.append(EipTAclRole.ACL_TYPE_COLUMN);
    sb.append(" & ");
    sb.append("#bind($aclNumber)");
    sb.append(") = ");
    sb.append("#bind($aclNumber)");
    sb.append(") AND (");
    sb.append(EipTAclPortletFeature.FEATURE_ID_PK_COLUMN);
    sb.append(" IN (SELECT ");
    sb.append(EipTAclPortletFeature.FEATURE_ID_PK_COLUMN);
    sb.append(" FROM eip_t_acl_portlet_feature WHERE ");
    sb.append(EipTAclPortletFeature.FEATURE_NAME_COLUMN);
    sb.append("= ");
    sb.append("#bind($feat)");
    sb.append("))))");

    StringBuffer statement = new StringBuffer();

    statement.append("SELECT DISTINCT ");
    statement
      .append("B.USER_ID, B.LOGIN_NAME, B.FIRST_NAME, B.LAST_NAME, D.POSITION ");
    statement.append("FROM turbine_user_group_role as A ");
    statement.append("LEFT JOIN turbine_user as B ");
    statement.append("on A.USER_ID = B.USER_ID ");
    statement.append("LEFT JOIN turbine_group as C ");
    statement.append("on A.GROUP_ID = C.GROUP_ID ");
    statement.append("LEFT JOIN eip_m_user_position as D ");
    statement.append("on A.USER_ID = D.USER_ID ");
    statement.append("WHERE B.USER_ID IN ");
    statement.append(sb);
    statement.append(" AND B.DISABLED = 'F'");
    statement.append(" AND C.GROUP_NAME = #bind($groupname) ");
    statement.append("ORDER BY D.POSITION");

    SQLTemplate<TurbineUser> template =
      Database.sql(TurbineUser.class, statement.toString());
    template.param("aclNumber", Integer.valueOf(aclNumber));
    template.param("feat", feat.trim());
    template.param("groupname", groupname);

    List<TurbineUser> list = template.fetchList();
    return list;
  }

  /**
   * ACLの登録（ここではコミット処理はしない）
   * 
   */
  @Override
  public void insertDefaultRole(int uid) throws Exception {
    TurbineUser tuser = Database.get(TurbineUser.class, Integer.valueOf(uid));

    // デフォルトロールはcreat_dateがない
// <#01> --- S
//  StringBuilder sql =
//  new StringBuilder().append("SELECT * FROM eip_t_acl_role ").append(
//      " WHERE create_date IS NULL");
    Integer employeeType = tuser.getEmployeeType();
    boolean isAdmin = ALEipUtils.isAdmin(tuser.getUserId());

    StringBuilder sql =
    new StringBuilder().append("SELECT * FROM eip_t_acl_role ");

    if (isAdmin == true) {
      // 管理者
      sql.append(" WHERE SUBSTR(role_name,1,1)<>")
         .append("\'" + TurbineUser.EMPLOYEE_ROLL_MARK_OTHER + "\'");
    } else if (employeeType == TurbineUser.EMPLOYEE_TYPE_EMPLOYEE) {
      // 社員
      sql.append(" WHERE SUBSTR(role_name,1,1)<>")
         .append("\'" + TurbineUser.EMPLOYEE_ROLL_MARK_ADMIN + "\'")
         .append(" AND SUBSTR(role_name,1,1)<>")
         .append("\'" + TurbineUser.EMPLOYEE_ROLL_MARK_OTHER + "\'");
    } else {
      // 社員以外
      sql.append(" WHERE SUBSTR(role_name,1,1)=").append("\'" + TurbineUser.EMPLOYEE_ROLL_MARK_OTHER + "\'");
    }
// <#01> --- E

    SQLTemplate<EipTAclRole> sqltemp =
      Database.sql(EipTAclRole.class, String.valueOf(sql));

    // リレーションを行う
    List<DataRow> fetchList = sqltemp.fetchListAsDataRow();
    List<EipTAclRole> list = new ArrayList<EipTAclRole>();
    for (DataRow row : fetchList) {
      EipTAclRole object = Database.objectFromRowData(row, EipTAclRole.class);
      // objectIDを同期する一手間
      EipTAclRole eipTAclRole =
        Database.get(EipTAclRole.class, object.getRoleId());
      // EipTAclPortletFeatureのデータをeipTAclRoleにマージ
      EipTAclPortletFeature feature =
        Database.get(EipTAclPortletFeature.class, row.get("feature_id"));
      eipTAclRole.setEipTAclPortletFeature(feature);
      list.add(eipTAclRole);
    }

    for (EipTAclRole role : list) {
      EipTAclUserRoleMap map = Database.create(EipTAclUserRoleMap.class);
      map.setEipTAclRole(role);
      map.setTurbineUser(tuser);
    }
  }

}
