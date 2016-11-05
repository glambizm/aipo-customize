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
package com.aimluck.eip.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONArray;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.jetspeed.capability.CapabilityMap;
import org.apache.jetspeed.capability.CapabilityMapFactory;
import org.apache.jetspeed.om.profile.Entry;
import org.apache.jetspeed.om.profile.Layout;
import org.apache.jetspeed.om.profile.PSMLDocument;
import org.apache.jetspeed.om.profile.Parameter;
import org.apache.jetspeed.om.profile.Portlets;
import org.apache.jetspeed.om.profile.Profile;
import org.apache.jetspeed.om.profile.ProfileException;
import org.apache.jetspeed.om.profile.ProfileLocator;
import org.apache.jetspeed.om.profile.psml.PsmlLayout;
import org.apache.jetspeed.om.profile.psml.PsmlParameter;
import org.apache.jetspeed.om.registry.ClientEntry;
import org.apache.jetspeed.om.registry.ClientRegistry;
import org.apache.jetspeed.om.registry.MediaTypeEntry;
import org.apache.jetspeed.om.security.Role;
import org.apache.jetspeed.om.security.UserIdPrincipal;
import org.apache.jetspeed.portal.Portlet;
import org.apache.jetspeed.portal.PortletConfig;
import org.apache.jetspeed.portal.portlets.VelocityPortlet;
import org.apache.jetspeed.portal.security.portlets.PortletWrapper;
import org.apache.jetspeed.services.JetspeedSecurity;
import org.apache.jetspeed.services.PortletFactory;
import org.apache.jetspeed.services.Profiler;
import org.apache.jetspeed.services.PsmlManager;
import org.apache.jetspeed.services.Registry;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.jetspeed.services.resources.JetspeedResources;
import org.apache.jetspeed.services.rundata.JetspeedRunData;
import org.apache.jetspeed.services.security.JetspeedSecurityException;
import org.apache.jetspeed.util.MimeType;
import org.apache.jetspeed.util.template.ContentTemplateLink;
import org.apache.jetspeed.util.template.JetspeedLink;
import org.apache.jetspeed.util.template.JetspeedLinkFactory;
import org.apache.turbine.om.security.User;
import org.apache.turbine.services.TurbineServices;
import org.apache.turbine.util.DynamicURI;
import org.apache.turbine.util.RunData;
import org.apache.turbine.util.TurbineException;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;

import com.aimluck.commons.field.ALDateTimeField;
import com.aimluck.commons.field.ALStringField;
import com.aimluck.eip.cayenne.om.account.AipoLicense;
import com.aimluck.eip.cayenne.om.account.EipMCompany;
import com.aimluck.eip.cayenne.om.account.EipMUserPosition;
import com.aimluck.eip.cayenne.om.portlet.EipMFacilityGroup;
import com.aimluck.eip.cayenne.om.security.TurbineGroup;
import com.aimluck.eip.cayenne.om.security.TurbineUser;
import com.aimluck.eip.cayenne.om.security.TurbineUserGroupRole;
import com.aimluck.eip.common.ALBaseUser;
import com.aimluck.eip.common.ALDBErrorException;
import com.aimluck.eip.common.ALEipConstants;
import com.aimluck.eip.common.ALEipGroup;
import com.aimluck.eip.common.ALEipManager;
import com.aimluck.eip.common.ALEipUser;
import com.aimluck.eip.common.ALMyGroups;
import com.aimluck.eip.common.ALPermissionException;
import com.aimluck.eip.http.HttpServletRequestLocator;
import com.aimluck.eip.modules.actions.controls.Restore;
import com.aimluck.eip.orm.Database;
import com.aimluck.eip.orm.query.Operations;
import com.aimluck.eip.orm.query.SelectQuery;
import com.aimluck.eip.services.accessctl.ALAccessControlConstants;
import com.aimluck.eip.services.accessctl.ALAccessControlFactoryService;
import com.aimluck.eip.services.accessctl.ALAccessControlHandler;
import com.aimluck.eip.services.config.ALConfigHandler.Property;
import com.aimluck.eip.services.config.ALConfigService;

/**
 *
 */
public class ALEipUtils {

  public static final String dummy_user_head = "dummy_";

  /** logger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(ALEipUtils.class.getName());

  // iPhone メニューを表示させたい順番に並べる
  // TODO: 設定ファイルで管理して VM と統合する
  public static String[] IPHONE_APPS = {
    "Msgboard",	// @@@
    "Timeline",
    "Schedule",
    "ExtTimecard",
    "Activity",
    "Blog",
//  "Msgboard",	// @@@
    "ToDo",
    "Cabinet",
    "Workflow",
    "Note",
    "Report",
    "WebMail",
    "AddressBook",
    "UserList",
    "Memo" };

  /**
   * セッション変数に値を格納します。 <br />
   * セッション変数は各ポートレット毎に管理されます。
   *
   * @param rundata
   * @param context
   * @param key
   *          セッション変数名
   * @param value
   *          セッション変数の値
   */
  public static void setTemp(RunData rundata, Context context, String key,
      String value) {

    JetspeedRunData jdata = (JetspeedRunData) rundata;
    VelocityPortlet portlet =
      ((VelocityPortlet) context.get(JetspeedResources.PATH_PORTLET_KEY));

    if (portlet == null) {
      // Screen の場合
      String js_peid =
        rundata.getParameters().getString(JetspeedResources.PATH_PORTLETID_KEY);
      jdata.getUser().setTemp(
        new StringBuffer().append(js_peid).append(key).toString(),
        value);
    } else {
      // Action の場合
      jdata.getUser().setTemp(
        new StringBuffer().append(portlet.getID()).append(key).toString(),
        value);
    }
  }

  /**
   * セッション変数を削除します。 <br />
   * セッション変数は各ポートレット毎に管理されます。
   *
   * @param rundata
   * @param context
   * @param key
   *          セッション変数名
   */
  public static void removeTemp(RunData rundata, Context context, String key) {
    JetspeedRunData jdata = (JetspeedRunData) rundata;
    VelocityPortlet portlet =
      ((VelocityPortlet) context.get(JetspeedResources.PATH_PORTLET_KEY));
    if (portlet == null) {
      // Screen の場合
      String js_peid =
        rundata.getParameters().getString(JetspeedResources.PATH_PORTLETID_KEY);
      jdata.getUser().removeTemp(
        new StringBuffer().append(js_peid).append(key).toString());
    } else {
      // Action の場合
      jdata.getUser().removeTemp(
        new StringBuffer().append(portlet.getID()).append(key).toString());
    }
  }

  public static void removeTemp(RunData rundata, Context context,
      List<String> list) {
    JetspeedRunData jdata = (JetspeedRunData) rundata;
    VelocityPortlet portlet =
      ((VelocityPortlet) context.get(JetspeedResources.PATH_PORTLET_KEY));
    int size = list.size();
    if (portlet == null) {
      // Screen の場合
      String js_peid =
        rundata.getParameters().getString(JetspeedResources.PATH_PORTLETID_KEY);
      for (int i = 0; i < size; i++) {
        jdata.getUser().removeTemp(
          new StringBuffer()
            .append(js_peid)
            .append(list.get(i).toString())
            .toString());
      }
    } else {
      // Action の場合
      String peid = portlet.getID();
      for (int i = 0; i < size; i++) {
        jdata.getUser().removeTemp(
          new StringBuffer()
            .append(peid)
            .append(list.get(i).toString())
            .toString());
      }
    }
  }

  /**
   * セッション変数の値を取得します。 <br />
   * セッション変数は各ポートレット毎に管理されます。
   *
   * @param rundata
   * @param context
   * @param key
   *          セッション変数名
   * @return セッション変数の値
   */
  public static String getTemp(RunData rundata, Context context, String key) {
    JetspeedRunData jdata = (JetspeedRunData) rundata;
    Object obj = null;
    VelocityPortlet portlet =
      ((VelocityPortlet) context.get(JetspeedResources.PATH_PORTLET_KEY));
    if (portlet == null) {
      // Screen の場合
      String js_peid =
        rundata.getParameters().getString(JetspeedResources.PATH_PORTLETID_KEY);
      obj =
        jdata.getUser().getTemp(
          new StringBuffer().append(js_peid).append(key).toString());
    } else {
      // Action の場合
      obj =
        jdata.getUser().getTemp(
          new StringBuffer().append(portlet.getID()).append(key).toString());
    }
    return (obj == null) ? null : obj.toString();
  }

  /**
   * セッションに保存されているエンティティIDを整数値として返します。
   *
   * @param rundata
   * @param context
   * @return
   */
  public static int getEntityId(RunData rundata, Context context) {
    int entity_id = 0;
    String entity_id_str =
      ALEipUtils.getTemp(rundata, context, ALEipConstants.ENTITY_ID);
    try {
      entity_id = Integer.parseInt(entity_id_str);
    } catch (Exception e) {
      entity_id = 0;
    }
    return entity_id;
  }

  /**
   * ユーザーIDを返します。
   *
   * @param rundata
   * @return ユーザーID
   */
  public static int getUserId(RunData rundata) {
    JetspeedRunData jdata = (JetspeedRunData) rundata;
    String id = jdata.getJetspeedUser().getUserId();
    return Integer.parseInt(id);
  }

  /**
   * ポートレットを返します。
   *
   * @param rundata
   * @param context
   * @return 自ポートレット
   */
  public static VelocityPortlet getPortlet(RunData rundata, Context context) {
    return ((VelocityPortlet) context.get(JetspeedResources.PATH_PORTLET_KEY));
  }

  /**
   * 指定したポートレット ID を持つポートレットのオブジェクトを取得します。
   *
   * @param rundata
   * @param portletId
   * @return 自ポートレット
   */
  public static Portlet getPortlet(RunData rundata, String portletId) {
    try {
      Profile profile = ((JetspeedRunData) rundata).getProfile();
      if (profile == null) {
        return null;
      }
      Portlets portlets = profile.getDocument().getPortlets();
      if (portlets == null) {
        return null;
      }

      @SuppressWarnings("unchecked")
      Iterator<Entry> iterator = portlets.getEntriesIterator();
      while (iterator.hasNext()) {
        Entry next = iterator.next();
        if (portletId.equals(next.getId())) {
          PortletWrapper activityWrapper =
            (PortletWrapper) PortletFactory.getPortlet(next);
          if (activityWrapper != null) {
            return activityWrapper.getPortlet();
          }
        }
      }

      Portlets[] portletList = portlets.getPortletsArray();
      if (portletList == null) {
        return null;
      }

      int length = portletList.length;
      for (int i = 0; i < length; i++) {
        Entry[] entries = portletList[i].getEntriesArray();
        if (entries == null || entries.length <= 0) {
          continue;
        }

        int ent_length = entries.length;
        for (int j = 0; j < ent_length; j++) {
          if (entries[j].getId().equals(portletId)) {
            PortletWrapper wrapper =
              (PortletWrapper) PortletFactory.getPortlet(entries[j]);
            if (wrapper != null) {
              return wrapper.getPortlet();
            } else {
              return null;
            }
          }
        }
      }
    } catch (Exception ex) {
      logger.error("ALEipUtils.getPortlet", ex);
      return null;
    }
    return null;
  }

  /**
   * 指定したポートレット ID を持つポートレットのオブジェクトを取得します。
   *
   * @param rundata
   * @param portletId
   * @return 自ポートレット
   */
  public static HashMap<String, String> getPortletFromAppIdMap(RunData rundata) {
    HashMap<String, String> hash = new HashMap<String, String>();
    try {
      Portlets portlets =
        ((JetspeedRunData) rundata).getProfile().getDocument().getPortlets();
      if (portlets == null) {
        return hash;
      }
      for (@SuppressWarnings("unchecked")
      Iterator<Entry> it = portlets.getEntriesIterator(); it.hasNext();) {
        Entry next = it.next();
        if (!hash.containsKey(next.getParent())) {
          hash.put(next.getParent(), next.getId());
        }
      }
      {
        Portlets[] portletList = portlets.getPortletsArray();

        if (portletList == null) {
          return hash;
        }

        for (Portlets portlet : portletList) {
          Entry[] entries = portlet.getEntriesArray();
          if (entries == null) {
            continue;
          }
          for (Entry entry : entries) {
            hash.put(entry.getParent(), entry.getId());
          }
        }
      }
    } catch (Exception ex) {
      logger.error("ALEipUtils.getPortletFromAppIdMap", ex);
      return hash;
    }
    return hash;
  }

  /**
   * リクエストが自ポートレットに対するものであるかを返します。 <br />
   * true となる場合、そのポートレットに対するフォーム送信となります。
   *
   * @param rundata
   * @param context
   * @return
   */
  public static boolean isMatch(RunData rundata, Context context) {
    VelocityPortlet portlet = getPortlet(rundata, context);

    if (portlet == null) {
      // Screen の場合
      return true;
    }
    String peid1 = portlet.getID();
    String peid2 =
      rundata.getParameters().getString(JetspeedResources.PATH_PORTLETID_KEY);
    if (peid1 == null || peid2 == null) {
      return false;
    }
    return peid1.equals(peid2);
  }

  /**
   * 指定されたグループに所属するユーザーを取得します。<br/>
   * DISABLEDがNのユーザー（即ち無効化されたユーザー）は取得しないことに注意してください。
   *
   * @param groupname
   *          グループ名
   * @return ALEipUser の List
   */
  public static List<ALEipUser> getUsers(String groupname) {
    List<ALEipUser> list = new ArrayList<ALEipUser>();

    // SQLの作成
    StringBuffer statement = new StringBuffer();
    statement.append("SELECT DISTINCT ");
    statement
      .append("  B.USER_ID, B.LOGIN_NAME, B.FIRST_NAME, B.LAST_NAME, D.POSITION ");
    statement.append("FROM turbine_user_group_role as A ");
    statement.append("LEFT JOIN turbine_user as B ");
    statement.append("  on A.USER_ID = B.USER_ID ");
    statement.append("LEFT JOIN turbine_group as C ");
    statement.append("  on A.GROUP_ID = C.GROUP_ID ");
    statement.append("LEFT JOIN eip_m_user_position as D ");
    statement.append("  on A.USER_ID = D.USER_ID ");
    statement.append("WHERE B.USER_ID > 3 AND B.DISABLED = 'F'");
    statement.append(" AND C.GROUP_NAME = #bind($groupName) ");
    statement.append("ORDER BY D.POSITION");

    String query = statement.toString();

    try {
      List<TurbineUser> list2 =
        Database
          .sql(TurbineUser.class, query)
          .param("groupName", groupname)
          .fetchList();
      for (TurbineUser tuser : list2) {
        list.add(getALEipUser(tuser));
      }
    } catch (Throwable t) {
      logger.error("ALEipUtils.getUsers", t);
    }

    return list;
  }

  /**
   * 指定されたグループに所属するユーザーを取得します。<br/>
   * DISABLEDがNのユーザー（即ち無効化されたユーザー）も取得します。
   *
   * @param groupname
   *          グループ名
   * @return ALEipUser の List
   */
  public static List<ALEipUser> getUsersIncludingN(String groupname) {
    List<ALEipUser> list = new ArrayList<ALEipUser>();

    // SQLの作成
    StringBuffer statement = new StringBuffer();
    statement.append("SELECT DISTINCT ");
    statement
      .append("  B.USER_ID, B.LOGIN_NAME, B.FIRST_NAME, B.LAST_NAME, D.POSITION ");
    statement.append("FROM turbine_user_group_role as A ");
    statement.append("LEFT JOIN turbine_user as B ");
    statement.append("  on A.USER_ID = B.USER_ID ");
    statement.append("LEFT JOIN turbine_group as C ");
    statement.append("  on A.GROUP_ID = C.GROUP_ID ");
    statement.append("LEFT JOIN eip_m_user_position as D ");
    statement.append("  on A.USER_ID = D.USER_ID ");
    statement.append("WHERE B.USER_ID > 3 AND B.DISABLED != 'T'");
    statement.append(" AND C.GROUP_NAME = #bind($groupName) ");
    statement.append("ORDER BY D.POSITION");

    String query = statement.toString();

    try {
      List<TurbineUser> list2 =
        Database
          .sql(TurbineUser.class, query)
          .param("groupName", groupname)
          .fetchList();
      for (TurbineUser tuser : list2) {
        list.add(getALEipUser(tuser));
      }
    } catch (Throwable t) {
      logger.error("ALEipUtils.getUsersIncludingN", t);
    }

    return list;
  }

  /**
   * 指定されたグループに所属するユーザーのIDを取得します。
   *
   * @param groupname
   *          グループ名
   * @return Integer の List
   */
  public static List<Integer> getUserIds(String groupname) {
    List<Integer> list = new ArrayList<Integer>();

    // SQLの作成
    StringBuffer statement = new StringBuffer();
    statement.append("SELECT DISTINCT ");
    statement.append("  B.USER_ID, D.POSITION ");
    statement.append("FROM turbine_user_group_role as A ");
    statement.append("LEFT JOIN turbine_user as B ");
    statement.append("  on A.USER_ID = B.USER_ID ");
    statement.append("LEFT JOIN turbine_group as C ");
    statement.append("  on A.GROUP_ID = C.GROUP_ID ");
    statement.append("LEFT JOIN eip_m_user_position as D ");
    statement.append("  on A.USER_ID = D.USER_ID ");
    statement.append("WHERE B.USER_ID > 3 AND B.DISABLED = 'F'");
    statement.append(" AND C.GROUP_NAME = #bind($groupName) ");
    statement.append("ORDER BY D.POSITION");
    String query = statement.toString();

    try {
      List<TurbineUser> list2 =
        Database
          .sql(TurbineUser.class, query)
          .param("groupName", groupname)
          .fetchList();
      for (TurbineUser tuser : list2) {
        list.add(tuser.getUserId());
      }
    } catch (Throwable t) {
      logger.error("ALEipUtils.getUsersFromPost", t);
    }

    return list;
  }

  /**
   * 指定された部署に所属するユーザーを取得します。
   *
   * @param postid
   *          部署ID
   * @return ALEipUser の List
   */
  public static List<ALEipUser> getUsersFromPost(int postid) {
    List<ALEipUser> list = new ArrayList<ALEipUser>();

    // SQLの作成
    StringBuffer statement = new StringBuffer();
    statement.append("SELECT DISTINCT ");
    statement
      .append("  B.USER_ID, B.LOGIN_NAME, B.FIRST_NAME, B.LAST_NAME, D.POSITION ");
    statement.append("FROM turbine_user_group_role as A ");
    statement.append("LEFT JOIN turbine_user as B ");
    statement.append("  on A.USER_ID = B.USER_ID ");
    statement.append("LEFT JOIN turbine_group as C ");
    statement.append("  on A.GROUP_ID = C.GROUP_ID ");
    statement.append("LEFT JOIN eip_m_user_position as D ");
    statement.append("  on A.USER_ID = D.USER_ID ");
    statement.append("WHERE B.USER_ID > 3 AND B.DISABLED = 'F'");
    statement.append(" AND B.POST_ID = #bind($postId) ");
    statement.append("ORDER BY D.POSITION");
    String query = statement.toString();

    try {
      List<TurbineUser> list2 =
        Database
          .sql(TurbineUser.class, query)
          .param("postId", postid)
          .fetchList();

      for (TurbineUser tuser : list2) {
        list.add(getALEipUser(tuser));
      }
    } catch (Throwable t) {
      logger.error("ALEipUtils.getUsersFromPost", t);
    }

    return list;
  }

  /**
   * <code>SelectQuery</code> の条件に従ってユーザーを取得します。
   *
   * @param crt
   * @return ALEipUser の List
   */
  public static List<ALEipUser> getUsersFromSelectQuery(
      SelectQuery<TurbineUser> query) {
    List<ALEipUser> list = new ArrayList<ALEipUser>();
    try {
      List<TurbineUser> ulist =
        query.orderAscending(
          TurbineUser.EIP_MUSER_POSITION_PROPERTY
            + "."
            + EipMUserPosition.POSITION_PROPERTY).fetchList();

      for (TurbineUser tuser : ulist) {
        list.add(getALEipUser(tuser));
      }
    } catch (Throwable t) {
      logger.error("ALEipUtils.getUsersFromSelectQuery", t);
    }
    return list;
  }

  /**
   * 自ユーザーの簡易オブジェクトを取得します。
   *
   * @param crt
   * @return ALEipUser
   */
  public static ALEipUser getALEipUser(RunData rundata) {
    JetspeedRunData jdata = (JetspeedRunData) rundata;
    ALBaseUser baseuser = (ALBaseUser) jdata.getJetspeedUser();
    ALEipUser user = new ALEipUser();
    user.initField();
    user.setUserId(Integer.parseInt(baseuser.getUserId()));
    user.setName(baseuser.getUserName());
    user.setAliasName(baseuser.getFirstName(), baseuser.getLastName());
    user.setCreated(baseuser.getCreated());
    return user;
  }

  /**
   * 指定したユーザーIDの簡易オブジェクトを取得します。
   *
   * @param id
   * @return
   */
  public static ALEipUser getALEipUser(int id) throws ALDBErrorException {
    TurbineUser tuser = getTurbineUser(id);
    return getALEipUser(tuser);
  }

  /**
   * 指定したユーザーIDの簡易オブジェクトを取得します。
   *
   * @param id
   * @return
   */
  public static ALEipUser getALEipUser(TurbineUser tuser)
      throws ALDBErrorException {
    if (tuser == null) {
      return null;
    }
    ALEipUser user = new ALEipUser();
    user.initField();
    user.setUserId(tuser.getUserId().intValue());
    user.setName(tuser.getLoginName());
    user.setAliasName(tuser.getFirstName(), tuser.getLastName());
    user.setHasPhoto("T".equals(tuser.getHasPhoto())
      || "N".equals(tuser.getHasPhoto()));
    user.setPhotoModified(tuser.getPhotoModified() != null ? tuser
      .getPhotoModified()
      .getTime() : 0);
    user.setCreated(tuser.getCreated());
    return user;
  }

  /**
   * 指定したユーザーIDの簡易オブジェクトを取得します。
   *
   * @param id
   * @return
   */
  public static ALEipUser getALEipUser(String loginname)
      throws ALDBErrorException {
    TurbineUser tuser = getTurbineUser(loginname);
    if (tuser == null) {
      return null;
    }
    return getALEipUser(tuser);
  }

  /**
   * 指定したユーザーIDのオブジェクトを取得します。
   *
   * @param userid
   *          ユーザID
   * @return
   */
  public static ALBaseUser getBaseUser(Integer userid) {
    if (userid == null) {
      logger.debug("Empty ID...");
      return null;
    }
    String uid = String.valueOf(userid);
    try {
      return (ALBaseUser) JetspeedSecurity.getUser(new UserIdPrincipal(uid));
    } catch (Exception ex) {
      logger.error("ALEipUtils.getBaseUser", ex);
      return null;
    }
  }

  /**
   * 指定したユーザーIDのユーザーオブジェクトを取得します。
   *
   * @param id
   * @return
   */
  public static TurbineUser getTurbineUser(int id) throws ALDBErrorException {
    Object obj = ALEipManager.getInstance().getTurbineUser(id);
    TurbineUser tuser = null;
    if (obj == null) {
      tuser = Database.get(TurbineUser.class, id);
      ALEipManager.getInstance().setTurbineUser(id, tuser);
    } else {
      tuser = (TurbineUser) obj;
    }
    return tuser;
  }

  /**
   * 指定したログイン名のユーザーオブジェクトを取得します。
   *
   * @param login_name
   * @return
   */
  public static TurbineUser getTurbineUser(String login_name)
      throws ALDBErrorException {
    Object obj = ALEipManager.getInstance().getTurbineUser(login_name);
    TurbineUser tuser = null;
    if (obj == null) {
      tuser =
        Database
          .query(TurbineUser.class)
          .where(Operations.eq(TurbineUser.LOGIN_NAME_PROPERTY, login_name))
          .fetchSingle();
      ALEipManager.getInstance().setTurbineUser(login_name, tuser);
    } else {
      tuser = (TurbineUser) obj;
    }
    return tuser;
  }

  /**
   * 指定したユーザーIDが有効か（無効化、削除されていないか）どうか調べます。
   *
   * @param id
   * @return
   */
  public static boolean isEnabledUser(int id) throws ALDBErrorException {
    TurbineUser tuser = getTurbineUser(id);
    if (tuser == null) {
      return false;
    }
    return "F".equals(tuser.getDisabled());
  }

  /**
   * ユーザーのフルネームを取得します。
   *
   * @param userid
   *          ユーザID
   * @return
   */
  public static String getUserFullName(int userid) {
    String userName = "";
    ALBaseUser user = getBaseUser(userid);
    if (user != null) {
      userName =
        new StringBuffer().append(user.getLastName()).append(" ").append(
          user.getFirstName()).toString();
    }
    return userName;
  }

  /**
   * 部署の変更を行います。 <br>
   * 部署に関連付けされているグループの更新も同時に行います。
   *
   * @param rundata
   * @param username
   *          ユーザー名
   * @param postid
   *          部署ID
   * @return true 部署変更成功 false 部署変更失敗
   */
  public static boolean changePost(RunData rundata, String username, int postid)
      throws ALDBErrorException {
    try {
      ALBaseUser user = (ALBaseUser) JetspeedSecurity.getUser(username);

      // グループへ追加
      JetspeedSecurity.joinGroup(username, (ALEipManager
        .getInstance()
        .getPostMap().get(Integer.valueOf(postid))).getGroupName().getValue());

      // 部署を変更
      user.setPostId(postid);

      // ユーザーを更新
      JetspeedSecurity.saveUser(user);

      ALBaseUser currentUser = (ALBaseUser) rundata.getUser();
      if (currentUser.getUserName().equals(user.getUserName())) {
        // 自ユーザーのセッション情報を更新する
        currentUser.setPostId(user.getPostId());
      }

    } catch (JetspeedSecurityException ex) {
      logger.error("ALEipUtils.changePost", ex);
      throw new ALDBErrorException();
    }
    return true;
  }

  /**
   * 自ユーザーのマイグループを再読み込みします。 <br>
   * 読み込まれたマイグループはセッションに保存されます。 <br>
   * マイグループの更新が行われた場合はこのメソッドを呼び出してください。
   *
   * @param rundata
   */
  public static void reloadMygroup(RunData rundata) throws ALDBErrorException {
    List<ALEipGroup> ulist = new ArrayList<ALEipGroup>();
    try {
      Expression exp =
        ExpressionFactory.matchExp(TurbineGroup.OWNER_ID_PROPERTY, Integer
          .valueOf(getUserId(rundata)));
      List<TurbineGroup> list =
        Database.query(TurbineGroup.class, exp).fetchList();

      for (TurbineGroup record : list) {
        ALEipGroup group = new ALEipGroup();
        group.initField();
        group.setName(record.getGroupName());
        group.setAliasName(record.getGroupAliasName());
        ulist.add(group);
      }
    } catch (Exception ex) {
      logger.error("ALEipUtils.reloadMygroup", ex);
      throw new ALDBErrorException();
    }

    // ServletRequestのマイグループに保存
    ALMyGroups mygroups = new ALMyGroups();
    mygroups.addList(ulist);
    HttpServletRequest request = HttpServletRequestLocator.get();
    if (request != null) {
      request.setAttribute(ALEipConstants.MYGROUP, mygroups);
    }

  }

  /**
   * 自ユーザーのマイグループを取得します。
   *
   * @param rundata
   * @return ALEipGroup の List
   */
  public static List<ALEipGroup> getMyGroups(RunData rundata)
      throws ALDBErrorException {
    // ServletRequestからマイグループのリストを読み込み
    Object obj = null;
    HttpServletRequest request = HttpServletRequestLocator.get();
    if (request != null) {
      obj = request.getAttribute(ALEipConstants.MYGROUP);
    }

    if (obj == null || !(obj instanceof ALMyGroups)) {
      // まだMyGroupが読み込まれていない場合は
      reloadMygroup(rundata);
      if (request != null) {
        obj = request.getAttribute(ALEipConstants.MYGROUP);
      }
    }
    ALMyGroups mygroups = (ALMyGroups) obj;
    return mygroups.getList();
  }

  public static List<ALEipGroup> getFacilityGroups() {
    List<ALEipGroup> facilityGroupAllList = new ArrayList<ALEipGroup>();
    try {
      SelectQuery<EipMFacilityGroup> query =
        Database.query(EipMFacilityGroup.class);

      List<EipMFacilityGroup> facility_list =
        query.orderAscending(EipMFacilityGroup.SORT_PROPERTY).fetchList();

      for (EipMFacilityGroup record : facility_list) {
        ALEipGroup bean = new ALEipGroup();
        bean.initField();
        bean.setAliasName(record.getGroupName());
        bean.setName(record.getGroupId().toString());
        facilityGroupAllList.add(bean);
      }
    } catch (Exception ex) {
      logger.error("ALEipUtils.getFacilityGroups", ex);
    }
    return facilityGroupAllList;
  }

  public static List<ALEipGroup> getALEipGroups() {
    List<ALEipGroup> facilityGroupAllList = new ArrayList<ALEipGroup>();
    try {
      SelectQuery<EipMFacilityGroup> query =
        Database.query(EipMFacilityGroup.class);

      List<EipMFacilityGroup> facility_list =
        query.orderAscending(EipMFacilityGroup.SORT_PROPERTY).fetchList();

      for (EipMFacilityGroup record : facility_list) {
        ALEipGroup bean = new ALEipGroup();
        bean.initField();
        bean.setAliasName(record.getGroupName());
        bean.setName(record.getGroupId().toString());
        facilityGroupAllList.add(bean);
      }
    } catch (Exception ex) {
      logger.error("ALEipUtils.getALEipGroups", ex);
    }
    return facilityGroupAllList;
  }

  /**
   * 会社名を取得します。
   *
   * @param id
   * @return
   */
  public static String getCompanyName(int id) {
    String companyName = null;
    try {
      Expression exp =
        ExpressionFactory.matchDbExp(EipMCompany.COMPANY_ID_PK_COLUMN, Integer
          .valueOf(id));
      List<EipMCompany> list =
        Database.query(EipMCompany.class, exp).select(
          EipMCompany.COMPANY_NAME_COLUMN).fetchList();

      if (list == null || list.size() == 0) {
        // 指定したCompany IDのレコードが見つからない場合
        logger.debug("[ALEipUtils] Not found ComapnyID...");
        return null;
      }

      EipMCompany record = list.get(0);
      companyName = record.getCompanyName();

    } catch (Exception ex) {
      logger.error("ALEipUtils.getCompanyName", ex);
      companyName = null;
    }

    return companyName;
  }

  /**
   * 部署名を取得します。
   *
   * @param id
   * @return
   */
  public static String getPostName(int id) {
    if (ALEipManager
      .getInstance()
      .getPostMap()
      .containsKey(Integer.valueOf(id))) {
      return (ALEipManager.getInstance().getPostMap().get(Integer.valueOf(id)))
        .getPostName()
        .getValue();
    }
    return null;
  }

  /**
   * 役職名を取得します。
   *
   * @param id
   * @return
   */
  public static String getPositionName(int id) {
    if (ALEipManager.getInstance().getPositionMap().containsKey(
      Integer.valueOf(id))) {
      return (ALEipManager.getInstance().getPositionMap().get(Integer
        .valueOf(id))).getPositionName().getValue();
    }
    return null;
  }

  /**
   * ページが見つからない場合に、リダイレクト処理します。
   *
   * @return
   */
  public static boolean redirectPageNotFound(RunData rundata) {
    try {
      JetspeedLink jsLink = JetspeedLinkFactory.getInstance(rundata);
      DynamicURI duri = jsLink.getPage();
      String template =
        rundata.getParameters().getString(JetspeedResources.PATH_TEMPLATE_KEY);
      if (template != null && !("".equals(template))) {
        if (template.endsWith("DetailScreen")
          || template.endsWith("FormScreen")) {
          VelocityContext context = new VelocityContext();
          setupContext(rundata, context);
          try {
            ServletOutputStream out = null;
            HttpServletResponse response = rundata.getResponse();
            out = response.getOutputStream();
            BufferedWriter writer =
              new BufferedWriter(new OutputStreamWriter(
                out,
                ALEipConstants.DEF_CONTENT_ENCODING));
            context
              .put("l10n", ALLocalizationUtils.createLocalization(rundata));
            Template templete =
              Velocity.getTemplate("screens/html/AjaxPageNotFound.vm");
            templete.merge(context, writer);
            writer.flush();
            writer.close();
          } catch (ResourceNotFoundException e) {
            logger.error("ALEipUtils.redirectPageNotFound", e);
            throw new RuntimeException(e);
          } catch (ParseErrorException e) {
            logger.error("ALEipUtils.redirectPageNotFound", e);
            throw new RuntimeException(e);
          } catch (Exception e) {
            logger.error("ALEipUtils.redirectPageNotFound", e);
            throw new RuntimeException(e);
          }
          return true;
        }
      }
      duri.addPathInfo("template", "PageNotFound");
      rundata.setRedirectURI(duri.toString());
      rundata.getResponse().sendRedirect(rundata.getRedirectURI());

      JetspeedLinkFactory.putInstance(jsLink);
      jsLink = null;
      return true;
    } catch (TurbineException e) {

      logger.error("ALEipUtils.redirectPageNotFound", e);
      return false;
    } catch (IOException e) {

      logger.error("ALEipUtils.redirectPageNotFound", e);
      return false;
    }
  }

  /**
   * データベースエラーの場合に、リダイレクト処理します。
   *
   * @return
   */
  public static boolean redirectDBError(RunData rundata) {
    try {
      JetspeedLink jsLink = JetspeedLinkFactory.getInstance(rundata);
      DynamicURI duri = jsLink.getPage();
      String template =
        rundata.getParameters().getString(JetspeedResources.PATH_TEMPLATE_KEY);
      if (template != null && !("".equals(template))) {
        if (template.endsWith("DetailScreen")
          || (template.endsWith("FormScreen"))) {
          Context context =
            org.apache.turbine.services.velocity.TurbineVelocity
              .getContext(rundata);
          if (null == context) {
            context = new VelocityContext();
            setupContext(rundata, context);
          }
          try {
            ServletOutputStream out = null;
            HttpServletResponse response = rundata.getResponse();
            out = response.getOutputStream();
            BufferedWriter writer =
              new BufferedWriter(new OutputStreamWriter(
                out,
                ALEipConstants.DEF_CONTENT_ENCODING));
            context
              .put("l10n", ALLocalizationUtils.createLocalization(rundata));
            context.put("utils", new ALCommonUtils());
            Template templete =
              Velocity.getTemplate("screens/html/AjaxDBError.vm");
            templete.merge(context, writer);
            writer.flush();
            writer.close();
          } catch (ResourceNotFoundException e) {
            logger.error("ALEipUtils.redirectDBError", e);
            throw new RuntimeException(e);
          } catch (ParseErrorException e) {
            logger.error("ALEipUtils.redirectDBError", e);
            throw new RuntimeException(e);
          } catch (Exception e) {
            logger.error("ALEipUtils.redirectDBError", e);
            throw new RuntimeException(e);
          }
          return true;
        }
      }
      duri.addPathInfo(JetspeedResources.PATH_TEMPLATE_KEY, "DBError");
      rundata.setRedirectURI(duri.toString());
      rundata.getResponse().sendRedirect(rundata.getRedirectURI());

      JetspeedLinkFactory.putInstance(jsLink);
      jsLink = null;
      return true;
    } catch (TurbineException e) {

      logger.error("ALEipUtils.redirectDBError", e);
      return false;
    } catch (IOException e) {

      logger.error("ALEipUtils.redirectDBError", e);
      return false;
    }
  }

  /**
   * パーミッションエラーの場合に、リダイレクト処理します。
   *
   * @return
   */
  public static boolean redirectPermissionError(RunData rundata) {
    try {
      JetspeedLink jsLink = JetspeedLinkFactory.getInstance(rundata);
      DynamicURI duri = jsLink.getPage();
      String template =
        rundata.getParameters().getString(JetspeedResources.PATH_TEMPLATE_KEY);
      if (template != null && !("".equals(template))) {

        if (template.endsWith("JSONScreen")) {
          VelocityContext context = new VelocityContext();
          setupContext(rundata, context);

          ServletOutputStream out = null;
          HttpServletResponse response = rundata.getResponse();
          out = response.getOutputStream();

          List<String> list = new ArrayList<String>();
          list.add("PermissionError");
          list.add(ALAccessControlConstants.DEF_PERMISSION_ERROR_STR);
          JSONArray json = JSONArray.fromObject(list);

          StringBuffer result =
            new StringBuffer().append("/* ").append(json.toString()).append(
              " */");

          byte[] byteResult =
            result.toString().getBytes(ALEipConstants.DEF_CONTENT_ENCODING);

          out.write(byteResult);
          out.flush();
          out.close();

          return true;
        } else if (template.endsWith("FormScreen")
          || template.endsWith("DetailScreen")) {
          VelocityContext context = new VelocityContext();
          setupContext(rundata, context);
          try {
            ServletOutputStream out = null;
            HttpServletResponse response = rundata.getResponse();
            out = response.getOutputStream();
            BufferedWriter writer =
              new BufferedWriter(new OutputStreamWriter(
                out,
                ALEipConstants.DEF_CONTENT_ENCODING));
            context
              .put("l10n", ALLocalizationUtils.createLocalization(rundata));
            Template templete =
              Velocity.getTemplate("screens/html/AjaxPermissionError.vm");
            templete.merge(context, writer);
            writer.flush();
            writer.close();
          } catch (ResourceNotFoundException e) {
            logger.error("ALEipUtils.redirectPermissionError", e);
            throw new RuntimeException(e);
          } catch (ParseErrorException e) {
            logger.error("ALEipUtils.redirectPermissionError", e);
            throw new RuntimeException(e);
          } catch (Exception e) {
            logger.error("ALEipUtils.redirectPermissionError", e);
            throw new RuntimeException(e);
          }
          return true;
        } else if (template.endsWith("Screen")) {
          // 一覧表示の場合
          VelocityContext context = new VelocityContext();
          setupContext(rundata, context);
          try {
            ServletOutputStream out = null;
            HttpServletResponse response = rundata.getResponse();
            out = response.getOutputStream();
            BufferedWriter writer =
              new BufferedWriter(new OutputStreamWriter(
                out,
                ALEipConstants.DEF_CONTENT_ENCODING));
            context
              .put("l10n", ALLocalizationUtils.createLocalization(rundata));
            Template templete =
              Velocity.getTemplate("portlets/html/PermissionError.vm");
            templete.merge(context, writer);
            writer.flush();
            writer.close();
          } catch (Exception e) {
            return false;
          }
          return true;

        } else if (template.equals("Customize") || template.equals("Home")) {
          // ポートレットカスタマイズ
          duri.addPathInfo(
            JetspeedResources.PATH_TEMPLATE_KEY,
            "PermissionError");
          rundata.setRedirectURI(duri.toString());
          rundata.getResponse().sendRedirect(rundata.getRedirectURI());
          JetspeedLinkFactory.putInstance(jsLink);

          jsLink = null;
          return true;
        } else if (isCellularPhone(rundata)) {
          duri.addPathInfo(
            JetspeedResources.PATH_TEMPLATE_KEY,
            "CellPermissionError");
          rundata.setRedirectURI(duri.toString());
          rundata.getResponse().sendRedirect(rundata.getRedirectURI());
          JetspeedLinkFactory.putInstance(jsLink);
          jsLink = null;
          Restore restore = new Restore();
          try {
            restore.doPerform(rundata);
          } catch (Exception e) {
          }
          return true;
        } else if (isSmartPhone(rundata)) {
          duri.addPathInfo(
            JetspeedResources.PATH_TEMPLATE_KEY,
            "CellPermissionError");
          rundata.setRedirectURI(duri.toString());
          rundata.getResponse().sendRedirect(rundata.getRedirectURI());
          JetspeedLinkFactory.putInstance(jsLink);
          jsLink = null;
          Restore restore = new Restore();
          try {
            restore.doPerform(rundata);
          } catch (Exception e) {
          }
          return true;
        }

      }

      /*-
      try {
        Restore restore = new Restore();
        restore.doPerform(rundata);
      } catch (Exception e) {
      }
       */

      JetspeedRunData jdata = (JetspeedRunData) rundata;
      if (jdata.getMode() == JetspeedRunData.MAXIMIZE) {

        rundata.getRequest().setAttribute(
          "redirectTemplate",
          "permission-error-maximize");

        /*-
        duri
          .addPathInfo(JetspeedResources.PATH_TEMPLATE_KEY, "PermissionError");
        rundata.setRedirectURI(duri.toString());
        rundata.getResponse().sendRedirect(rundata.getRedirectURI());
        JetspeedLinkFactory.putInstance(jsLink);

        jsLink = null;
         */
      } else {
        Context context =
          (Context) jdata.getTemplateInfo().getTemplateContext(
            "VelocityPortletContext");
        context.put(JetspeedResources.PATH_TEMPLATE_KEY, "PermissionError");
      }

      return true;
    } catch (TurbineException e) {

      logger.error("ALEipUtils.redirectPermissionError", e);
      return false;
    } catch (IOException e) {

      logger.error("ALEipUtils.redirectPermissionError", e);
      return false;
    }
  }

  /**
   * 改行コードを含む文字列を、複数行に分割します。 メソッドの統一
   *
   * @return
   */
  public static String getMessageList(String msgline) {
    return getMessageList(msgline, null);
  }

  /**
   * 左端の半角空文字を「&nbsp;」に変換する。
   *
   * @param str
   * @return
   */
  public static String replaseLeftSpace(String str) {
    if (str == null || str.length() <= 0) {
      return str;
    }

    int len = str.length();
    int st = 0;
    char[] val = str.toCharArray();

    StringBuffer sb = new StringBuffer();
    boolean left = true;
    while ((st < len)) {
      if (val[st] == ' ' && left) {
        sb.append("&nbsp;");
      } else {
        sb.append(val[st]);
        left = false;
      }
      st++;
    }
    return (sb.length() > 0) ? sb.toString() : str;
  }

  /**
   * アクセス元の端末が携帯電話であるかを判定します。
   *
   * @param data
   * @return
   */
  public static boolean isCellularPhone(RunData data) {
    boolean isCellularPhone = false;
    CapabilityMap cm =
      CapabilityMapFactory.getCapabilityMap(data.getRequest().getHeader(
        "User-Agent"));
    MimeType mime = cm.getPreferredType();
    if (mime != null) {
      MediaTypeEntry media =
        (MediaTypeEntry) Registry.getEntry(Registry.MEDIA_TYPE, cm
          .getPreferredMediaType());
      String mediatype = media.getName();
      if ("docomo_imode".equals(mediatype)
        || "docomo_foma".equals(mediatype)
        || "au".equals(mediatype)
        || "vodafone".equals(mediatype)) {
        isCellularPhone = true;
      }
    }
    return isCellularPhone;
  }

  /**
   * アクセス元の端末がスマートフォンであるかを判定します。
   *
   * @param data
   * @return
   */
  public static boolean isSmartPhone(RunData data) {
    boolean isSmartPhone = false;
    CapabilityMap cm =
      CapabilityMapFactory.getCapabilityMap(data.getRequest().getHeader(
        "User-Agent"));
    MimeType mime = cm.getPreferredType();
    if (mime != null) {
      MediaTypeEntry media =
        (MediaTypeEntry) Registry.getEntry(Registry.MEDIA_TYPE, cm
          .getPreferredMediaType());
      String mediatype = media.getName();
      if ("iphone".equals(mediatype) || "wm".equals(mediatype)) {
        isSmartPhone = true;
      }
    }
    return isSmartPhone;
  }

  /**
   * 指定した2つの日付を比較します。
   *
   * @param date1
   * @param date2
   * @return 等しい場合、0。date1>date2の場合、1。date1 < date2の場合、2。
   */
  public static int compareToDate(Date date1, Date date2) {
    Calendar cal1 = Calendar.getInstance();
    Calendar cal2 = Calendar.getInstance();
    cal1.setTime(date1);
    cal2.setTime(date2);

    int date1Year = cal1.get(Calendar.YEAR);
    int date1Month = cal1.get(Calendar.MONTH) + 1;
    int date1Day = cal1.get(Calendar.DATE);
    int date1Hour = cal1.get(Calendar.HOUR);
    int date1Minute = cal1.get(Calendar.MINUTE);
    int date1Second = cal1.get(Calendar.SECOND);
    int date2Year = cal2.get(Calendar.YEAR);
    int date2Month = cal2.get(Calendar.MONTH) + 1;
    int date2Day = cal2.get(Calendar.DATE);
    int date2Hour = cal2.get(Calendar.HOUR);
    int date2Minute = cal2.get(Calendar.MINUTE);
    int date2Second = cal2.get(Calendar.SECOND);

    if (date1Year == date2Year
      && date1Month == date2Month
      && date1Day == date2Day
      && date1Hour == date2Hour
      && date1Minute == date2Minute
      && date1Second == date2Second) {
      return 0;
    }
    if (cal1.after(cal2)) {
      return 2;
    } else {
      return 1;
    }
  }

  /**
   * データベースの検索結果から、指定したキーに対応する値を取得します。
   *
   * @param dataRow
   * @param key
   * @return
   */
  public static Object getObjFromDataRow(DataRow dataRow, String key) {
    String lowerKey = key.toLowerCase(Locale.ENGLISH);
    if (dataRow.containsKey(lowerKey)) {
      return dataRow.get(lowerKey);
    } else {
      return dataRow.get(key.toUpperCase(Locale.ENGLISH));
    }
  }

  /**
   * 会社情報のオブジェクトを取得します。
   *
   * @param id
   * @return
   */
  public static EipMCompany getEipMCompany(String id) {
    Expression exp =
      ExpressionFactory.matchDbExp(EipMCompany.COMPANY_ID_PK_COLUMN, Integer
        .valueOf(id));
    List<EipMCompany> list = Database.query(EipMCompany.class, exp).fetchList();
    if (list == null || list.size() == 0) {
      logger.debug("Not found ID...");
      return null;
    }
    return list.get(0);
  }

  /**
   * @see ALServletUtils#getAccessUrl(String, int, boolean)
   * @param ip
   * @param port
   * @param servername
   * @param isGlobal
   * @return
   * @deprecated
   */
  @Deprecated
  public static String getUrl(String ip, int port, String servername,
      boolean isGlobal) {
    if (ip == null || ip.length() == 0 || port == -1) {
      return "";
    }

    String protocol =
      isGlobal
        ? ALConfigService.get(Property.ACCESS_GLOBAL_URL_PROTOCOL)
        : ALConfigService.get(Property.ACCESS_LOCAL_URL_PROTOCOL);

    StringBuffer url = new StringBuffer();

    if (port == 80 || port == 443) {
      url.append(protocol).append("://").append(ip).append("/").append(
        servername).append(servername.isEmpty() ? "" : "/");
    } else {
      url
        .append(protocol)
        .append("://")
        .append(ip)
        .append(":")
        .append(port)
        .append("/")
        .append(servername)
        .append(servername.isEmpty() ? "" : "/");
    }

    return url.toString();
  }

  /**
   *
   * @param rundata
   * @param context
   */
  public static void setupContext(RunData rundata, Context context) {
    String js_peid;
    if (!rundata.getParameters().containsKey(
      JetspeedResources.PATH_PORTLETID_KEY)) {
      return;
    }
    js_peid =
      rundata.getParameters().getString(JetspeedResources.PATH_PORTLETID_KEY);

    Portlet portlet = getPortlet(rundata, js_peid);
    context.put("portlet", portlet);
    context.put("jslink", new ALJetspeedLink(rundata));
    context.put("clink", new ContentTemplateLink(rundata));
  }

  public static void setupContext(String js_peid, RunData rundata,
      Context context) {
    Portlet portlet = getPortlet(rundata, js_peid);
    context.put("portlet", portlet);
    context.put("jslink", new ALJetspeedLink(rundata));
    context.put("clink", new ContentTemplateLink(rundata));
  }

  /**
   *
   * @param rundata
   * @param context
   * @param key
   * @return
   */
  public static String getParameter(RunData rundata, Context context, String key) {
    String name = null;
    String idParam = rundata.getParameters().getString(key);
    name = ALEipUtils.getTemp(rundata, context, key);
    if (idParam == null && name == null) {
      ALEipUtils.removeTemp(rundata, context, key);
      name = null;
    } else if (idParam != null) {
      ALEipUtils.setTemp(rundata, context, key, idParam);
      name = idParam;
    }
    return name;
  }

  /**
   * 指定したエントリー名のポートレットへの URI を取得します。
   *
   * @param rundata
   * @param portletEntryName
   *          PSML ファイルに記述されているタグ entry の要素 parent
   * @return
   */
  public static String getPortletURI(RunData rundata, String portletEntryId) {
    try {
      Portlets portlets =
        ((JetspeedRunData) rundata).getProfile().getDocument().getPortlets();
      if (portlets == null) {
        return null;
      }

      Portlets[] portletList = portlets.getPortletsArray();
      if (portletList == null) {
        return null;
      }

      int length = portletList.length;
      for (int i = 0; i < length; i++) {
        Entry[] entries = portletList[i].getEntriesArray();
        if (entries == null || entries.length <= 0) {
          continue;
        }

        int ent_length = entries.length;
        for (int j = 0; j < ent_length; j++) {
          if (entries[j].getId().equals(portletEntryId)) {
            JetspeedLink jsLink = JetspeedLinkFactory.getInstance(rundata);
            DynamicURI duri =
              jsLink.getLink(
                JetspeedLink.CURRENT,
                null,
                null,
                JetspeedLink.CURRENT,
                null);
            duri =
              duri.addPathInfo(
                JetspeedResources.PATH_PANEID_KEY,
                portletList[i].getId()).addPathInfo(
                JetspeedResources.PATH_PORTLETID_KEY,
                entries[j].getId()).addQueryData(
                JetspeedResources.PATH_ACTION_KEY,
                "controls.Maximize");
            return duri.toString();
          }
        }
      }
    } catch (Exception ex) {
      logger.error("ALEipUtils.getPortletURI", ex);
      return null;
    }
    return null;
  }

  /**
   *
   * @param rundata
   * @param portletEntryId
   * @return
   */
  public static String getPortletURItoTopPage(RunData rundata,
      String portletEntryId) {
    try {
      Portlets portlets =
        ((JetspeedRunData) rundata).getProfile().getDocument().getPortlets();
      if (portlets == null) {
        return null;
      }

      Portlets[] portletList = portlets.getPortletsArray();
      if (portletList == null) {
        return null;
      }

      int length = portletList.length;
      for (int i = 0; i < length; i++) {
        Entry[] entries = portletList[i].getEntriesArray();
        if (entries == null || entries.length <= 0) {
          continue;
        }

        int ent_length = entries.length;
        for (int j = 0; j < ent_length; j++) {
          if (entries[j].getId().equals(portletEntryId)) {
            JetspeedLink jsLink = JetspeedLinkFactory.getInstance(rundata);
            DynamicURI duri =
              jsLink.getLink(
                JetspeedLink.CURRENT,
                null,
                null,
                JetspeedLink.CURRENT,
                null);
            duri =
              duri.addPathInfo(
                JetspeedResources.PATH_PANEID_KEY,
                portletList[i].getId()).addQueryData(
                JetspeedResources.PATH_ACTION_KEY,
                "controls.Restore");
            return duri.toString();
          }
        }
      }
    } catch (Exception ex) {
      logger.error("ALEipUtils.getPortletURItoTopPage", ex);
      return null;
    }
    return null;
  }

  /**
   * 文字列中の検索キーワードを<span class="searchKeyword">タグで囲います。
   *
   * @param msg
   * @return
   */
  public static String highlihgtKeywords(String msg, String keyword) {
    if (msg != null) {
      if (keyword != null && !("".equals(keyword))) {
        Boolean inTag = false;
        String regex = "((" + keyword + ")+)";
        int len = msg.length();
        int st = 0;
        char[] val = msg.toCharArray();
        StringBuffer result = new StringBuffer();
        StringBuffer temp = new StringBuffer();
        while ((st < len)) {
          if (inTag == false) {
            temp.append(val[st]);
            if (val[st] == '<') {
              inTag = true;
              String highLighted =
                temp.toString().replaceAll(
                  regex,
                  "<span class=\"searchKeyword\">$1</span>");
              result.append(highLighted);
              temp.delete(0, temp.length());
            }
          } else if (inTag == true) {
            result.append(val[st]);
            if (val[st] == '>') {
              inTag = false;
            }
          }
          st++;
        }
        if (st == len && inTag == false) {
          result.append(temp.toString().replaceAll(
            regex,
            "<span class=\"searchKeyword\">$1</span>"));
        }
        return result.toString();
      } else {
        return msg;
      }
    } else {
      return "";
    }
  }

  /**
   * 文字列内のリンクにタグAを追加します。 メソッドの統一
   *
   * @param msg
   * @return
   */
  public static String replaceStrToLink(String msg) {
    return replaceStrToLink(msg, null);
  }

  public static String replaceStrToLink(String msg, String keyword) {
    if (msg != null) {
      String regex =
        "(https?|ftp|gopher|telnet|whois|news)\\:([\\w|\\:\\!\\#\\$\\%\\=\\&\\-\\^\\`\\\\|\\@\\~\\[\\{\\]\\}\\;\\+\\*\\,\\.\\?\\/]+)";
      Pattern p = Pattern.compile(regex);
      boolean check = true;
      while (check) {
        check = false;
        Matcher m = p.matcher(msg);
        while (m.find()) {
          if (m.group(0).contains("@")) {
            String matchString = m.group(0);
            matchString = matchString.replaceAll("@", "%40");
            String pre = msg.substring(0, m.start(0));
            String post = msg.substring(m.end(0), msg.length());
            msg = pre + matchString + post;
            check = true;
          }
        }
      }

      // 日本語（ひらがな、かたかな、漢字）が含まれていてもリンク化されるように正規表現を追加する。
      // URL
      String newMsg =
        msg
          .replaceAll(
            "(https?|ftp|gopher|telnet|whois|news)\\:([\\w|\\p{InHiragana}\\p{InKatakana}\\p{InCJKUnifiedIdeographs}\\:\\!\\#\\$\\%\\=\\&\\-\\^\\`\\\\|\\@\\~\\[\\{\\]\\}\\;\\+\\*\\,\\.\\?\\/]+)",
            "<a href=\"$1\\:$2\" target=\"_blank\">$1\\:$2</a>");
      // mail
      String newnewMsg =
        newMsg.replaceAll(
          "[\\w\\.\\-]+@([\\w\\-]+\\.)+[\\w\\-]+",
          "<a href='mailto:$0'>$0</a>");
      // highlight
      String highlightedMsg = highlihgtKeywords(newnewMsg, keyword);
      return highlightedMsg;
    } else {
      return "";
    }
  }

  /**
   * フォルダを再帰的に消します。
   *
   * @param parent_folder
   *          親フォルダ
   * @param cal
   *
   * @return フォルダの中身が全て消去されたときのみtrueを返します
   */

  /**
   * ユーザーの所属する部署を取得します。
   *
   * @param id
   *          ユーザーID
   * @return 所属する部署リスト
   */
  public static List<ALStringField> getPostNameList(int id) {
    SelectQuery<TurbineUserGroupRole> query =
      Database.query(TurbineUserGroupRole.class);
    Expression exp1 =
      ExpressionFactory.matchExp(
        TurbineUserGroupRole.TURBINE_USER_PROPERTY,
        Integer.valueOf(id));
    Expression exp2 =
      ExpressionFactory.greaterExp(
        TurbineUserGroupRole.TURBINE_GROUP_PROPERTY,
        Integer.valueOf(3));
    Expression exp3 =
      ExpressionFactory.matchExp(TurbineUserGroupRole.TURBINE_GROUP_PROPERTY
        + "."
        + TurbineGroup.OWNER_ID_PROPERTY, Integer.valueOf(1));
    query.setQualifier(exp1);
    query.andQualifier(exp2);
    query.andQualifier(exp3);
    List<TurbineUserGroupRole> list = query.fetchList();

    ALStringField sf = null;
    List<ALStringField> postNames = new ArrayList<ALStringField>();
    for (TurbineUserGroupRole role : list) {
      sf = new ALStringField(role.getTurbineGroup().getGroupAliasName());
      postNames.add(sf);
    }

    return postNames;
  }

  /**
   * ユーザーの所属する部署のIDを取得します。
   *
   * @param id
   *          ユーザーID
   * @return 所属する部署リスト
   */
  public static List<Integer> getPostIdList(int id) {
    SelectQuery<TurbineUserGroupRole> query =
      Database.query(TurbineUserGroupRole.class);
    Expression exp1 =
      ExpressionFactory.matchExp(
        TurbineUserGroupRole.TURBINE_USER_PROPERTY,
        Integer.valueOf(id));
    Expression exp2 =
      ExpressionFactory.greaterExp(
        TurbineUserGroupRole.TURBINE_GROUP_PROPERTY,
        Integer.valueOf(3));
    Expression exp3 =
      ExpressionFactory.matchExp(TurbineUserGroupRole.TURBINE_GROUP_PROPERTY
        + "."
        + TurbineGroup.OWNER_ID_PROPERTY, Integer.valueOf(1));
    query.setQualifier(exp1);
    query.andQualifier(exp2);
    query.andQualifier(exp3);
    List<TurbineUserGroupRole> list = query.fetchList();

    List<Integer> postIds = new ArrayList<Integer>();
    for (TurbineUserGroupRole role : list) {
      postIds.add(role.getTurbineGroup().getEipMPost().getPostId());
    }

    return postIds;
  }

  /**
   * アクセス権限をチェックします（ポートレットカスタマイズ）
   *
   * @return
   */
  public static boolean checkAclPermissionForCustomize(RunData rundata,
      Context context, int defineAclType) {
    try {
      if (defineAclType == 0) {
        return true;
      }

      boolean hasAuthority = getHasAuthority(rundata, context, defineAclType);
      if (!hasAuthority) {
        throw new ALPermissionException();
      }

      return true;
    } catch (ALPermissionException e) {
      ALEipUtils.redirectPermissionError(rundata);
      return false;
    }
  }

  public static boolean getHasAuthority(RunData rundata, Context context,
      int defineAclType) {
    String pfeature =
      ALAccessControlConstants.POERTLET_FEATURE_PORTLET_CUSTOMIZE;
    if (pfeature == null || "".equals(pfeature)) {
      return true;
    }

    ALAccessControlFactoryService aclservice =
      (ALAccessControlFactoryService) ((TurbineServices) TurbineServices
        .getInstance()).getService(ALAccessControlFactoryService.SERVICE_NAME);
    ALAccessControlHandler aclhandler = aclservice.getAccessControlHandler();
    return aclhandler.hasAuthority(
      ALEipUtils.getUserId(rundata),
      pfeature,
      defineAclType);
  }

  public static int getLimitUsers() {
    try {

      List<AipoLicense> list =
        Database
          .query(AipoLicense.class)
          .select(AipoLicense.LIMIT_USERS_COLUMN)
          .fetchList();

      if (list != null && list.size() > 0) {
        AipoLicense record = list.get(0);
        Integer result = record.getLimitUsers();
        return result.intValue();
      }
    } catch (Exception e) {
      logger.error("ALEipUtils.getLimitUsers", e);
    }
    return 0;
  }

  /**
   * 現在登録されている有効なユーザー数(システムユーザ、論理削除は除く)を取得します。
   *
   * @return
   */
  public static int getCurrentUserNumEnabledOnly(RunData rundata) {
    int registeredUserNum = -1;
    try {
      // 論理削除ユーザーを除く
      // ユーザーテーブルDISABLEDがTのものが論理削除
      // システムユーザtemplateは論理削除されているため
      // RES_USER_NUMは3だが2として計算しないといけない。

      Expression exp =
        ExpressionFactory.matchExp(TurbineUser.DISABLED_PROPERTY, "F");

      List<TurbineUser> list =
        Database.query(TurbineUser.class, exp).fetchList();
      if (list == null || list.size() <= 0) {
        return -1;
      }
      int size = list.size();
      // admin,anonユーザが含まれるので2ユーザ分減算
      registeredUserNum = size - 2;
    } catch (Exception ex) {
      logger.error("ユーザー情報をDBから取得できませんでした。");
      logger.error("ALEipUtils.getCurrentUserNumEnabledOnly", ex);
      return -1;
    }
    return registeredUserNum;
  }

  /**
   * 現在登録されている有効なユーザー数(システムユーザ、論理削除、無効化は除く)を取得します。
   *
   * @return
   */
  public static int getCurrentUserNum(RunData rundata) {
    int registeredUserNum = -1;
    try {
      // 論理削除ユーザーを除く
      // ユーザーテーブルDISABLEDがTのものが論理削除
      // システムユーザtemplateは論理削除されているため
      // RES_USER_NUMは3だが2として計算しないといけない。

      Expression exp =
        ExpressionFactory.noMatchExp(TurbineUser.DISABLED_PROPERTY, "T");

      List<TurbineUser> list =
        Database.query(TurbineUser.class, exp).fetchList();
      if (list == null || list.size() <= 0) {
        return -1;
      }
      int size = list.size();
      // admin,anonユーザが含まれるので2ユーザ分減算
      registeredUserNum = size - 2;
    } catch (Exception ex) {
      logger.error("ユーザー情報をDBから取得できませんでした。");
      logger.error("ALEipUtils.getCurrentUserNum", ex);
      return -1;
    }
    return registeredUserNum;
  }

  /**
   * 指定されたユーザが管理者権限を持っているかを返します。
   *
   * @param uid
   * @return
   */
  public static boolean isAdmin(int uid) {
    boolean res = false;
    try {
      Role adminrole = JetspeedSecurity.getRole("admin");
      TurbineUserGroupRole role =
        Database
          .query(TurbineUserGroupRole.class)
          .where(
            Operations.eq(TurbineUserGroupRole.TURBINE_ROLE_PROPERTY, adminrole
              .getId()),
            Operations.eq(TurbineUserGroupRole.TURBINE_USER_PROPERTY, uid))
          .fetchSingle();
      res = role != null;
    } catch (JetspeedSecurityException e) {
      logger.error("管理者ロールが存在しません。");
      logger.error("ALEipUtils.isAdmin", e);
    }
    return res;
  }

  /**
   * ログインユーザが管理者権限を持っているかを返します。
   *
   * @param rundata
   * @return
   */
  public static boolean isAdmin(RunData rundata) {
    JetspeedRunData jdata = (JetspeedRunData) rundata;
    return isAdmin(Integer.valueOf(jdata.getUserId()));
  }

  /**
   * is LoginUser Admin ?
   */
  public static boolean isAdminUser(RunData runData) {
    int userId = getUserId(runData);
    return isAdminUser(userId);
  }

  public static boolean isAdminUser(int userId) {
    // admin user definition : user id equals 1
    return userId == 1;
  }

  public static String getLoginName(RunData runData) {
    JetspeedRunData jdata = (JetspeedRunData) runData;
    return jdata.getJetspeedUser().getUserName();
  }

  /**
   * Dateに対して整形されたALDateTimeFieldを返します。
   *
   * @param date
   * @return 整形されたALDateTimeField
   */
  public static ALDateTimeField getFormattedTime(Date date) {
    Calendar Now = new GregorianCalendar();
    Now.setTime(new Date());
    Calendar Time = new GregorianCalendar();
    Time.setTime(date);
    ALDateTimeField rtn;

    rtn =
      (Now.get(Calendar.YEAR) == Time.get(Calendar.YEAR)) ? (Now
        .get(Calendar.MONTH) == Time.get(Calendar.MONTH)
        && Now.get(Calendar.DATE) == Time.get(Calendar.DATE)
        ? new ALDateTimeField("H:mm")
        : new ALDateTimeField("M月d日")) : new ALDateTimeField("yyyy年M月d日");
    rtn.setValue(date);
    return rtn;
  }

  public static ALDateTimeField getFormattedTime(ALDateTimeField timeField) {
    if (!timeField.isNotNullValue()) {
      return null;
    }
    return getFormattedTime(timeField.getValue());
  }

  public static ALDateTimeField getFormattedTimeDetail(Date date) {
    Calendar Now = new GregorianCalendar();
    Now.setTime(new Date());
    Calendar Time = new GregorianCalendar();
    Time.setTime(date);
    ALDateTimeField rtn;

    rtn =
      (Now.get(Calendar.YEAR) == Time.get(Calendar.YEAR))
        ? new ALDateTimeField("M月d日 H:mm")
        : new ALDateTimeField("yyyy年M月d日 H:mm");
    rtn.setValue(date);
    return rtn;
  }

  public static ALDateTimeField getFormattedTimeDetail(ALDateTimeField timeField) {
    if (!timeField.isNotNullValue()) {
      return null;
    }
    return getFormattedTimeDetail(timeField.getValue());
  }

  /**
   * 指定したユーザのPSMLにシステム管理のページを追加します。
   *
   * @param user_name
   * @throws Exception
   */
  public static void addAdminPage(String user_name) throws Exception {
    ProfileLocator locator = Profiler.createLocator();
    locator.createFromPath(String.format("user/%s/media-type/html", user_name));
    /** for request cache clear */
    ALEipManager.getInstance().removeProfile(locator);
    Profile profile = Profiler.getProfile(locator);
    Portlets portlets = profile.getDocument().getPortlets();

    if (portlets != null) {
      // 既に配置されているかどうかを確認
      long max_position = 0;
      int portlet_size = portlets.getPortletsCount();
      for (int i = 0; i < portlet_size; i++) {
        Portlets p = portlets.getPortlets(i);
        if (p.getSecurityRef().getParent().equals("admin-view")) {
          return;
        }
        if (p.getLayout().getPosition() > max_position) {
          max_position = p.getLayout().getPosition();
        }
      }
      // レイアウトの作成
      Layout newLayout = new PsmlLayout();
      newLayout.setPosition(max_position + 1);
      newLayout.setSize(-1);

      ProfileLocator admin_locator = Profiler.createLocator();
      admin_locator.createFromPath("user/admin/media-type/html");
      Portlets admin_portlets =
        Profiler.createProfile(admin_locator).getDocument().getPortlets();
      admin_portlets = admin_portlets.getPortlets(0);
      Entry[] entriesArray = admin_portlets.getEntriesArray();
      int size = entriesArray.length;
      for (int i = 0; i < size; i++) {
        if (entriesArray[i].getParent().equals("DeleteSample")) {
          admin_portlets.removeEntry(i);
          break;
        }
      }
      admin_portlets.setLayout(newLayout);
      portlets.addPortlets(admin_portlets);

      PsmlManager.store(profile);
    }
  }

  public static String getFirstPortletId(String username) {
    try {
      ProfileLocator locator = Profiler.createLocator();
      locator
        .createFromPath(String.format("user/%s/media-type/html", username));
      Profile profile = Profiler.getProfile(locator);
      Portlets portlets = profile.getDocument().getPortlets();

      if (portlets == null) {
        return null;
      }

      Portlets[] portletList = portlets.getPortletsArray();
      if (portletList == null) {
        return null;
      }

      for (Portlets portlet : portletList) {
        Entry[] entries = portlet.getEntriesArray();
        if (entries == null || entries.length == 0) {
          continue;
        }
        for (String name : IPHONE_APPS) {
          for (Entry entry : entries) {
            String portletName = entry.getParent();
            if (portletName.equals("AjaxScheduleWeekly")) {
              portletName = "Schedule";
            }
            if (name.equals(portletName)) {
              return entry.getId();
            }
          }
        }
      }
    } catch (Exception ex) {
      logger.error("ALEipUtils.getFirstPortletId", ex);
      return null;
    }
    return null;
  }

  public static String getClient(RunData rundata) {
    if (Boolean.parseBoolean((String) rundata.getSession().getAttribute(
      "changeToPc"))) { // PC表示切り替え用
      return "PCIPHONE";
    } else {
      return getClient(rundata.getUserAgent().trim());
    }
  }

  public static String getClient(String userAgent) {
    return getClientEntry(userAgent).getKey();
  }

  /**
   * 期待するユーザーエージェントが含まれていればtrue
   *
   * @param expect
   * @param rundata
   * @return
   */
  public static boolean isMatchUserAgent(String expect, RunData rundata) {
    // User-Agent の取得
    String userAgent = rundata.getUserAgent().trim();
    if (userAgent == null || "".equals(userAgent)) {
      return false;
    }

    return userAgent.indexOf(expect) > -1;
  }

  /**
   * アクセスしてきたユーザが利用するブラウザ名が Windows の MSIE であるかを判定する．
   *
   * @param rundata
   * @return MSIE の場合は，true．
   */
  public static boolean isMsieBrowser(RunData rundata) {
    return isMatchUserAgent("Win", rundata)
      && (isMatchUserAgent("MSIE", rundata) || isMatchUserAgent(
        "Trident",
        rundata));
  }

  /**
   * アクセスしてきたユーザが利用するブラウザ名が Windows の Edge であるかを判定する．
   *
   * @param rundata
   * @return MSIE の場合は，true．
   */
  public static boolean isEdgeBrowser(RunData rundata) {
    return isMatchUserAgent("Win", rundata)
      && (isMatchUserAgent("Edge", rundata));
  }

  /***
   * アクセスしてきたユーザが利用するブラウザ名が Mozilla の Firefox であるかを判定する．
   * 
   * @param rundata
   * @return
   */
  public static boolean isFirefoxBrowser(RunData rundata) {
    return isMatchUserAgent("Firefox", rundata)
      && isMatchUserAgent("Mozilla", rundata);
  }

  /**
   * アクセスしてきたユーザが利用するブラウザ名が Android．
   *
   * @param rundata
   * @return MSIE の場合は，true．
   */
  public static boolean isAndroidBrowser(RunData rundata) {
    return isMatchUserAgent("Android", rundata);
  }

  public static boolean isAndroid2Browser(RunData rundata) {
    int[] version = getAndroidVersion(rundata);
    return version != null && version[0] == 2;
  }

  public static int[] getAndroidVersion(RunData rundata) {
    final String userAgent = rundata.getUserAgent().trim();
    if (userAgent == null || "".equals(userAgent)) {
      return null;
    }
    Pattern androidVersion =
      Pattern.compile(
        "Android ([\\d]+).([\\d]+).([\\d]+)",
        Pattern.CASE_INSENSITIVE);
    Matcher matcher = androidVersion.matcher(userAgent);
    if (matcher.find()) {
      return new int[] {
        Integer.parseInt(matcher.group(1)),
        Integer.parseInt(matcher.group(2)),
        Integer.parseInt(matcher.group(3)) };
    } else {
      return null;
    }
  }

  public static String getClientVersion(RunData rundata) {
    return getClientVersion(rundata.getUserAgent().trim());
  }

  public static String getClientVersion(String userAgent) {
    return getClientEntry(userAgent).getValue();
  }

  protected static Map.Entry<String, String> getClientEntry(String userAgent) {
    Map<String, String> map = new HashMap<String, String>(1);

    String key = "com.aimluck.eip.util.getClient.client";
    String keyVer = "com.aimluck.eip.util.getClient.clientVer";
    String client = null;
    String clientVer = "0";

    HttpServletRequest request = HttpServletRequestLocator.get();
    if (request != null) {
      client = (String) request.getAttribute(key);
      clientVer = (String) request.getAttribute(keyVer);
      if (client != null && client.length() > 0) {
        if (clientVer == null) {
          clientVer = "";
        }
        map.put(client, clientVer);
        return map.entrySet().iterator().next();
      }
    }

    ClientRegistry registry = (ClientRegistry) Registry.get(Registry.CLIENT);
    ClientEntry entry = registry.findEntry(userAgent);
    client = entry == null ? "OUTER" : entry.getManufacturer();

    if ("IPAD".equals(client) || "IPHONE".equals(client)) {
      clientVer = String.valueOf(userAgent.charAt(userAgent.indexOf("OS") + 3));
    }

    if (isIE(userAgent)) {
      client = "IE";
      clientVer = getIEVersion(userAgent);
    }

    if (request != null) {
      request.setAttribute(key, client);
      request.setAttribute(keyVer, clientVer);
    }
    if (clientVer == null) {
      clientVer = "";
    }
    map.put(client, clientVer);

    return map.entrySet().iterator().next();
  }

  public static boolean isIE(String userAgent) {
    return (userAgent.matches(".*((MSIE)+ [0-9]\\.[0-9]).*") || userAgent
      .matches(".*(.*(Trident/)+[0-9]\\.[0-9]).*"));

  }

  /**
   * PSMLにデータを埋め込みます。
   *
   * @param rundata
   * @param context
   * @param key
   * @param value
   * @return
   */
  public static boolean setPsmlParameters(RunData rundata, Context context,
      String key, String value) {
    try {
      String portletEntryId =
        rundata.getParameters().getString("js_peid", null);
      if (value == null || "".equals(value)) {// nullで送信するとpsmlが破壊される
        return false;
      }

      Profile profile = ((JetspeedRunData) rundata).getProfile();
      Portlets portlets = profile.getDocument().getPortlets();
      if (portlets == null) {
        return false;
      }

      Portlets[] portletList = portlets.getPortletsArray();
      if (portletList == null) {
        return false;
      }

      PsmlParameter param = null;
      int length = portletList.length;
      for (int i = 0; i < length; i++) {
        Entry[] entries = portletList[i].getEntriesArray();
        if (entries == null || entries.length <= 0) {
          continue;
        }

        int ent_length = entries.length;
        for (int j = 0; j < ent_length; j++) {
          if (entries[j].getId().equals(portletEntryId)) {
            boolean hasParam = false;
            Parameter params[] = entries[j].getParameter();
            int param_len = params.length;
            for (int k = 0; k < param_len; k++) {
              if (params[k].getName().equals(key)) {
                hasParam = true;
                params[k].setValue(value);
                entries[j].setParameter(k, params[k]);
              }
            }
            if (!hasParam) {
              param = new PsmlParameter();
              param.setName(key);
              param.setValue(value);
              entries[j].addParameter(param);
            }
            break;
          }
        }
      }
      profile.store();

    } catch (Exception ex) {
      logger.error("ALEipUtils.setPsmlParameters", ex);
      return false;
    }
    return true;
  }

  /**
   *
   * PSMLに設定されているデータと比較して valueが正しい値ならその値を新しくPSMLに保存。
   *
   *
   * @param rundata
   * @param context
   * @param config
   * @return
   */
  public static String passPSML(RunData rundata, Context context, String key,
      String value) {
    VelocityPortlet portlet = ALEipUtils.getPortlet(rundata, context);
    PortletConfig config = portlet.getPortletConfig();
    if (value == null || "".equals(value)) {
      value = config != null ? config.getInitParameter(key) : "";
    } else {
      ALEipUtils.setPsmlParameters(rundata, context, key, value);
    }
    return value;
  }

  public static boolean isFileUploadable(RunData rundata) {

    if (isMatchUserAgent("iPhone", rundata)) {
      String iOSver = getIOSVersion(rundata.getUserAgent().trim());
      if (iOSver.length() > 1) {
        Integer num = Integer.parseInt(iOSver.substring(0, 1));
        if (num.intValue() < 6) {
          return false;
        }
      }
    }

    return true;
  }

  public static String getIOSVersion(String userAgent) {
    Pattern pattern = Pattern.compile("OS\\s[0-9_]+");
    Matcher matcher = pattern.matcher(userAgent);

    if (matcher.find()) {
      String words = matcher.group();
      return words.replaceAll("OS\\s", "");
    }

    return "";
  }

  public static String getIEVersion(String userAgent) {
    Pattern pattern = Pattern.compile("MSIE\\s[0-9_]+");
    Matcher matcher = pattern.matcher(userAgent);
    Pattern pattern2 = Pattern.compile("rv:[0-9_]+");
    Matcher matcher2 = pattern2.matcher(userAgent);

    if (matcher.find()) {
      String words = matcher.group();
      return words.replaceAll("MSIE\\s", "");
    } else if (matcher2.find()) {
      String words2 = matcher2.group();
      return words2.replaceAll("rv:", "");
    }

    return "";
  }

  public static Map<String, Entry> getGlobalPortlets(RunData rundata) {
    Map<String, Entry> maps = new HashMap<String, Entry>();
    Profile profile = ((JetspeedRunData) rundata).getProfile();
    if (profile == null) {
      User user = rundata.getUser();
      if (user != null) {
        ProfileLocator locator = Profiler.createLocator();
        locator.createFromPath(String.format("user/%s/media-type/html", user
          .getUserName()));
        try {
          profile = Profiler.getProfile(locator);
        } catch (ProfileException ignore) {
          // ignore
        }
      }
    }
    if (profile != null) {
      PSMLDocument doc = profile.getDocument();
      if (doc != null) {
        @SuppressWarnings("unchecked")
        Iterator<Entry> iterator = doc.getPortlets().getEntriesIterator();
        while (iterator.hasNext()) {
          Entry next = iterator.next();
          maps.put(next.getParent(), next);
        }
      }
    }
    return maps;
  }

  /**
   * @param msgline
   * @param keyword
   * @return
   */
  public static String getMessageList(String msgline, String keyword) {
    StringBuffer sb = new StringBuffer();
    ALStringField field = null;
    ALStringField key = null;

    if (msgline == null || msgline.equals("")) {
      return "";
    }
    msgline = Normalizer.normalize(msgline, Normalizer.Form.NFC);
    if (msgline.indexOf("\r") < 0
      && msgline.indexOf("\n") < 0
      && msgline.indexOf("\r\n") < 0) {
      field = new ALStringField();
      field.setTrim(false);
      field.setValue(msgline);
      if (!(keyword == null || "".equals(keyword))) {
        key = new ALStringField();
        key.setTrim(true);
        key.setValue(keyword);
        return ALCommonUtils.replaceToAutoCR((replaceStrToLink(
          replaseLeftSpace(field.toString()),
          key.toString())));
      }
      return ALCommonUtils
        .replaceToAutoCR((replaceStrToLink(replaseLeftSpace(field.toString()))));
    }

    String token = null;
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new StringReader(msgline));
      while ((token = reader.readLine()) != null) {
        field = new ALStringField();
        field.setTrim(false);
        field.setValue(token);
        sb.append(
          ALCommonUtils.replaceToAutoCR((replaceStrToLink(
            replaseLeftSpace(field.toString()),
            keyword)))).append("<br/>");
      }
      reader.close();
    } catch (IOException ioe) {
      try {
        reader.close();
      } catch (IOException e) {
      }
      return "";
    }

    int index = sb.lastIndexOf("<br/>");
    if (index == -1) {
      return sb.toString();
    }

    return sb.substring(0, index).replaceAll("<wbr/><br/>", "<br/>");

  }
}