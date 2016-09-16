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
package com.aimluck.eip.msgboard;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.aimluck.commons.field.ALNumberField;
import com.aimluck.commons.field.ALStringField;
import com.aimluck.eip.cayenne.om.portlet.EipTMsgboardCategory;
import com.aimluck.eip.cayenne.om.portlet.EipTMsgboardCategoryMap;
import com.aimluck.eip.cayenne.om.portlet.EipTMsgboardTopic;
import com.aimluck.eip.cayenne.om.security.TurbineUser;
import com.aimluck.eip.common.ALAbstractFormData;
import com.aimluck.eip.common.ALDBErrorException;
import com.aimluck.eip.common.ALEipConstants;
import com.aimluck.eip.common.ALEipGroup;
import com.aimluck.eip.common.ALEipUser;
import com.aimluck.eip.common.ALPageNotFoundException;
import com.aimluck.eip.modules.actions.common.ALAction;
import com.aimluck.eip.msgboard.util.MsgboardUtils;
import com.aimluck.eip.orm.Database;
import com.aimluck.eip.orm.query.SelectQuery;
import com.aimluck.eip.services.accessctl.ALAccessControlConstants;
import com.aimluck.eip.services.eventlog.ALEventlogConstants;
import com.aimluck.eip.services.eventlog.ALEventlogFactoryService;
import com.aimluck.eip.util.ALEipUtils;
import com.aimluck.eip.util.ALLocalizationUtils;

/**
 * 掲示板カテゴリのフォームデータを管理するクラスです。 <BR>
 *
 */
public class MsgboardCategoryFormData extends ALAbstractFormData {

  /** logger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(MsgboardCategoryFormData.class.getName());

  /** カテゴリ名 */
  private ALStringField category_name;

  /** メモ */
  private ALStringField note;

  /** 閲覧/返信フラグ */
  private ALNumberField access_flag;

  /** <code>login_user</code> ログインユーザー */
  private ALEipUser login_user;

  /** <code>is_member</code> スケジュールを共有するかどうか */
  private boolean is_member;

  /** <code>memberList</code> メンバーリスト */
  private List<ALEipUser> memberList;

  /** <code>groups</code> グループ */
  private List<ALEipGroup> groups;

  /** 他人のカテゴリ編集権限 */
  private boolean authority_edit;

  /** 他人のカテゴリ削除権限 */
  private boolean authority_delete;

  /**
   *
   * @param action
   * @param rundata
   * @param context
   * @throws ALPageNotFoundException
   * @throws ALDBErrorException
   */
  @Override
  public void init(ALAction action, RunData rundata, Context context)
      throws ALPageNotFoundException, ALDBErrorException {
    is_member = rundata.getParameters().getBoolean("is_member");
    login_user = ALEipUtils.getALEipUser(rundata);
    groups = ALEipUtils.getMyGroups(rundata);

    authority_edit =
      MsgboardUtils.checkPermission(
        rundata,
        context,
        ALAccessControlConstants.VALUE_ACL_UPDATE,
        ALAccessControlConstants.POERTLET_FEATURE_MSGBOARD_CATEGORY_OTHER);

    authority_delete =
      MsgboardUtils.checkPermission(
        rundata,
        context,
        ALAccessControlConstants.VALUE_ACL_DELETE,
        ALAccessControlConstants.POERTLET_FEATURE_MSGBOARD_CATEGORY_OTHER);

    // スーパークラスのメソッドを呼び出す。
    super.init(action, rundata, context);
  }

  /**
   *
   *
   */
  @Override
  public void initField() {
    // カテゴリ名
    category_name = new ALStringField();
    category_name.setFieldName(ALLocalizationUtils
      .getl10n("MSGBOARD_CATEGORY_NAME"));
    category_name.setTrim(true);
    // メモ
    note = new ALStringField();
    note.setFieldName(ALLocalizationUtils.getl10n("MSGBOARD_MEMO"));
    note.setTrim(true);
    // 閲覧/返信フラグ
    access_flag = new ALNumberField();
    access_flag.setFieldName(ALLocalizationUtils
      .getl10n("MSGBOARD_CHANGE_VIEWS"));
    access_flag.setValue(0);
    // メンバーリスト
    memberList = new ArrayList<ALEipUser>();
  }

  /**
   * 掲示板カテゴリの各フィールドに対する制約条件を設定します。 <BR>
   *
   *
   */
  @Override
  protected void setValidator() {
    // カテゴリ名必須項目
    category_name.setNotNull(true);
    // カテゴリ名文字数制限
    category_name.limitMaxLength(50);
    // メモ文字数制限
    note.limitMaxLength(1000);
    // 閲覧/返信フラグ
    access_flag.limitValue(0, 3);
  }

  /**
   * 掲示板カテゴリのフォームに入力されたデータの妥当性検証を行います。 <BR>
   *
   * @param msgList
   * @return
   *
   */
  @Override
  protected boolean validate(List<String> msgList) {
    // カテゴリ名
    category_name.validate(msgList);
    // メモ
    note.validate(msgList);
    // 閲覧/返信フラグ
    access_flag.validate(msgList);

    int tmp_access_flag = (int) access_flag.getValue();
    if (tmp_access_flag == MsgboardUtils.ACCESS_PUBLIC_MEMBER
      || tmp_access_flag == MsgboardUtils.ACCESS_SEACRET_MEMBER) {
      // 所属メンバー
      if (memberList.size() == 0) {
        msgList.add("所属メンバーを選択してください。");
      }
    }

    return (msgList.size() == 0);
  }

  /**
   * 掲示板カテゴリをデータベースから読み出します。 <BR>
   *
   * @param rundata
   * @param context
   * @param msgList
   * @return
   */
  @Override
  protected boolean loadFormData(RunData rundata, Context context,
      List<String> msgList) throws ALPageNotFoundException, ALDBErrorException {
    try {
      String loginUserStatus = null;
      int uid = (int) login_user.getUserId().getValue();

      // オブジェクトモデルを取得
      EipTMsgboardCategory category =
        MsgboardUtils.getEipTMsgboardCategory(rundata, context, true);
      if (category == null) {
        return false;
      }
      // カテゴリ名
      category_name.setValue(category.getCategoryName());
      // メモ
      note.setValue(category.getNote());
      // 公開区分
      boolean public_flag =
        (MsgboardUtils.PUBLIC_FLG_VALUE_PUBLIC)
          .equals(category.getPublicFlag());

      // このカテゴリを共有しているメンバーを取得
      SelectQuery<EipTMsgboardCategoryMap> mapquery =
        Database.query(EipTMsgboardCategoryMap.class);
      Expression mapexp =
        ExpressionFactory.matchDbExp(
          EipTMsgboardCategory.CATEGORY_ID_PK_COLUMN,
          category.getCategoryId());
      mapquery.setQualifier(mapexp);

      List<EipTMsgboardCategoryMap> list = mapquery.fetchList();

      List<Integer> users = new ArrayList<Integer>();
      int size = list.size();
      for (int i = 0; i < size; i++) {
        EipTMsgboardCategoryMap map = list.get(i);
        users.add(map.getUserId());
        if (uid == map.getUserId().intValue()) {
          loginUserStatus = map.getStatus();
        }
      }

      SelectQuery<TurbineUser> query = Database.query(TurbineUser.class);
      Expression exp =
        ExpressionFactory.inDbExp(TurbineUser.USER_ID_PK_COLUMN, users);
      Expression nonDisabledexp =
        ExpressionFactory.noMatchExp(TurbineUser.DISABLED_PROPERTY, "T");
      query.setQualifier(exp.andExp(nonDisabledexp));
      memberList.addAll(ALEipUtils.getUsersFromSelectQuery(query));

      if (public_flag) {
        if ((MsgboardUtils.STAT_VALUE_ALL).equals(loginUserStatus)) {
          access_flag.setValue(MsgboardUtils.ACCESS_PUBLIC_ALL);
          is_member = false;
        } else {
          access_flag.setValue(MsgboardUtils.ACCESS_PUBLIC_MEMBER);
          is_member = true;
        }
      } else {
        if ((MsgboardUtils.STAT_VALUE_ALL).equals(loginUserStatus)) {
          access_flag.setValue(MsgboardUtils.ACCESS_SEACRET_SELF);
          is_member = false;
        } else {
          access_flag.setValue(MsgboardUtils.ACCESS_SEACRET_MEMBER);
          is_member = true;
        }
      }

    } catch (Exception e) {
      logger.error("[MsgboardFormData]", e);
      throw new ALDBErrorException();
    }
    return true;
  }

  /**
   * 掲示板カテゴリをデータベースに格納します。 <BR>
   *
   * @param rundata
   * @param context
   * @param msgList
   * @return
   */
  @Override
  protected boolean insertFormData(RunData rundata, Context context,
      List<String> msgList) throws ALPageNotFoundException, ALDBErrorException {
    try {
      TurbineUser tuser =
        Database.get(TurbineUser.class, Integer.valueOf(ALEipUtils
          .getUserId(rundata)));

      // 新規オブジェクトモデル
      EipTMsgboardCategory category =
        Database.create(EipTMsgboardCategory.class);
      // カテゴリ名
      category.setCategoryName(category_name.getValue());
      // メモ
      category.setNote(note.getValue());
      int accessFlag = (int) access_flag.getValue();
      if (accessFlag == MsgboardUtils.ACCESS_PUBLIC_ALL
        || accessFlag == MsgboardUtils.ACCESS_PUBLIC_MEMBER) {
        category.setPublicFlag(MsgboardUtils.PUBLIC_FLG_VALUE_PUBLIC);
      } else {
        category.setPublicFlag(MsgboardUtils.PUBLIC_FLG_VALUE_NONPUBLIC);
      }

      // ユーザーID
      category.setTurbineUser(tuser);
      // 作成日
      category.setCreateDate(Calendar.getInstance().getTime());
      // 更新日
      category.setUpdateDate(Calendar.getInstance().getTime());

      int size = memberList.size();
      for (int i = 0; i < size; i++) {
        EipTMsgboardCategoryMap map =
          Database.create(EipTMsgboardCategoryMap.class);
        ALEipUser user = memberList.get(i);
        int userid = (int) user.getUserId().getValue();
        map.setEipTMsgboardCategory(category);
        map.setUserId(Integer.valueOf(userid));
        // O: 自カテゴリ S: 共有カテゴリ（Share）
        if (userid == ALEipUtils.getUserId(rundata)) {
          if (accessFlag == MsgboardUtils.ACCESS_PUBLIC_ALL
            || accessFlag == MsgboardUtils.ACCESS_SEACRET_SELF) {
            // 所属メンバーがログインユーザのみの場合
            map.setStatus(MsgboardUtils.STAT_VALUE_ALL);
          } else {
            map.setStatus(MsgboardUtils.STAT_VALUE_OWNER);
          }
        } else {
          map.setStatus(MsgboardUtils.STAT_VALUE_SHARE);
        }
      }

      // 掲示板カテゴリを登録
      Database.commit();

      // イベントログに保存
      ALEventlogFactoryService.getInstance().getEventlogHandler().log(
        category.getCategoryId(),
        ALEventlogConstants.PORTLET_TYPE_MSGBOARD_CATEGORY,
        category.getCategoryName());

    } catch (Exception e) {
      Database.rollback();
      logger.error("[MsgboardFormData]", e);
      throw new ALDBErrorException();
    }
    return true;
  }

  /**
   * データベースに格納されている掲示板カテゴリを更新します。 <BR>
   *
   * @param rundata
   * @param context
   * @param msgList
   * @return
   */
  @Override
  protected boolean updateFormData(RunData rundata, Context context,
      List<String> msgList) throws ALPageNotFoundException, ALDBErrorException {
    try {
      // オブジェクトモデルを取得
      EipTMsgboardCategory category =
        MsgboardUtils.getEipTMsgboardCategory(rundata, context, true);
      if (category == null) {
        return false;
      }

      // カテゴリ名
      category.setCategoryName(category_name.getValue());
      // メモ
      category.setNote(note.getValue());
      // 閲覧返信フラグ
      int accessFlag = (int) access_flag.getValue();
      if (accessFlag == MsgboardUtils.ACCESS_PUBLIC_ALL
        || accessFlag == MsgboardUtils.ACCESS_PUBLIC_MEMBER) {
        category.setPublicFlag(MsgboardUtils.PUBLIC_FLG_VALUE_PUBLIC);
      } else {
        category.setPublicFlag(MsgboardUtils.PUBLIC_FLG_VALUE_NONPUBLIC);
      }
      // ユーザーID
      // category.setTurbineUser(tuser);
      // 更新日
      category.setUpdateDate(Calendar.getInstance().getTime());

      SelectQuery<EipTMsgboardCategoryMap> mapquery =
        Database.query(EipTMsgboardCategoryMap.class);
      Expression mapexp =
        ExpressionFactory.matchExp(
          EipTMsgboardCategoryMap.CATEGORY_ID_PROPERTY,
          category.getCategoryId());
      mapquery.setQualifier(mapexp);

      List<EipTMsgboardCategoryMap> maplist = mapquery.fetchList();
      Database.deleteAll(maplist);

      int size = memberList.size();
      for (int i = 0; i < size; i++) {
        EipTMsgboardCategoryMap map =
          Database.create(EipTMsgboardCategoryMap.class);
        ALEipUser user = memberList.get(i);
        int userid = (int) user.getUserId().getValue();
        map.setEipTMsgboardCategory(category);
        map.setUserId(Integer.valueOf(userid));
        // O: 自カテゴリ S: 共有カテゴリ（Share）
        if (userid == ALEipUtils.getUserId(rundata)) {
          if (accessFlag == MsgboardUtils.ACCESS_PUBLIC_ALL
            || accessFlag == MsgboardUtils.ACCESS_SEACRET_SELF) {
            // 所属メンバーがログインユーザのみの場合
            map.setStatus(MsgboardUtils.STAT_VALUE_ALL);
          } else {
            map.setStatus(MsgboardUtils.STAT_VALUE_OWNER);
          }
        } else {
          map.setStatus(MsgboardUtils.STAT_VALUE_SHARE);
        }
      }
      Database.commit();

      // イベントログに保存
      ALEventlogFactoryService.getInstance().getEventlogHandler().log(
        category.getCategoryId(),
        ALEventlogConstants.PORTLET_TYPE_MSGBOARD_CATEGORY,
        category.getCategoryName());

    } catch (Exception e) {
      Database.rollback();
      logger.error("[MsgboardFormData]", e);
      throw new ALDBErrorException();
    }
    return true;
  }

  /**
   * 掲示板カテゴリを削除します。 <BR>
   *
   * @param rundata
   * @param context
   * @param msgList
   * @return
   */
  @SuppressWarnings("unchecked")
  @Override
  protected boolean deleteFormData(RunData rundata, Context context,
      List<String> msgList) throws ALPageNotFoundException, ALDBErrorException {
    try {
      // オブジェクトモデルを取得
      EipTMsgboardCategory category =
        MsgboardUtils.getEipTMsgboardCategory(rundata, context, true);
      if (category == null) {
        return false;
      }

      // 掲示板カテゴリを削除
      // DBテーブルのカスケード設定で，
      // トピックおよび添付ファイルのレコードも自動的に削除される．

      // イベントログに保存
      ALEventlogFactoryService.getInstance().getEventlogHandler().log(
        category.getCategoryId(),
        ALEventlogConstants.PORTLET_TYPE_MSGBOARD_CATEGORY,
        category.getCategoryName());

      List<EipTMsgboardTopic> topics = category.getEipTMsgboardTopics();
      for (EipTMsgboardTopic topic : topics) {
        MsgboardUtils.deleteAttachmentFiles(topic);
      }

      Database.delete(category);
      Database.commit();
      // 一覧表示画面のフィルタに設定されているカテゴリのセッション情報を削除
      String filtername =
        MsgboardTopicSelectData.class.getSimpleName()
          + ALEipConstants.LIST_FILTER;
      ALEipUtils.removeTemp(rundata, context, filtername);
    } catch (Exception e) {
      Database.rollback();
      logger.error("[MsgboardFormData]", e);
      throw new ALDBErrorException();
    }
    return true;
  }

  /**
   *
   * @param rundata
   * @param context
   * @param msgList
   * @return
   * @throws ALPageNotFoundException
   * @throws ALDBErrorException
   */
  @Override
  protected boolean setFormData(RunData rundata, Context context,
      List<String> msgList) throws ALPageNotFoundException, ALDBErrorException {
    boolean res = super.setFormData(rundata, context, msgList);
    if (res) {
      int tmp_access_flag = (int) access_flag.getValue();
      if (tmp_access_flag == MsgboardUtils.ACCESS_PUBLIC_ALL
        || tmp_access_flag == MsgboardUtils.ACCESS_SEACRET_SELF) {
        memberList.add(login_user);
      } else {
        String str[] = rundata.getParameters().getStrings("member_to");
        if (str == null || str.length == 0) {
          return res;
        }

        boolean containsLoginUser = false;
        String str_loginuserid = login_user.getName().getValue();
        int strsize = str.length;
        for (int i = 0; i < strsize; i++) {
          if (str_loginuserid.equals(str[i])) {
            containsLoginUser = true;
            break;
          }
        }
        if (!containsLoginUser) {
          memberList.add(login_user);
        }

        SelectQuery<TurbineUser> query = Database.query(TurbineUser.class);
        Expression exp =
          ExpressionFactory.inExp(TurbineUser.LOGIN_NAME_PROPERTY, str);
        query.setQualifier(exp);
        memberList.addAll(ALEipUtils.getUsersFromSelectQuery(query));
      }
    }
    return res;
  }

  /**
   * カテゴリ名を取得します。 <BR>
   *
   * @return
   */
  public ALStringField getCategoryName() {
    return category_name;
  }

  /**
   * メモを取得します。 <BR>
   *
   * @return
   */
  public ALStringField getNote() {
    return note;
  }

  public ALNumberField getAccessFlag() {
    return access_flag;
  }

  /**
   *
   * @return
   */
  public boolean isMember() {
    return (is_member || memberList.size() > 1);
  }

  /**
   * グループメンバーを取得します。
   *
   * @return
   */
  public List<ALEipUser> getMemberList() {
    return memberList;
  }

  /**
   * 指定したグループ名のユーザーを取得します。
   *
   * @param groupname
   * @return
   */
  public List<ALEipUser> getUsers(String groupname) {
    return ALEipUtils.getUsers(groupname);
  }

  /**
   * グループリストを取得します。
   *
   * @return
   */
  public List<ALEipGroup> getGroupList() {
    return groups;
  }

  /**
   * ログインユーザを取得します。
   *
   * @return
   */
  public ALEipUser getLoginUser() {
    return login_user;
  }

  public boolean getAuthorityEdit() {
    return authority_edit;
  }

  public boolean getAuthorityDelete() {
    return authority_delete;
  }

  /**
   * アクセス権限チェック用メソッド。 アクセス権限の機能名を返します。
   *
   * @return
   */
  @Override
  public String getAclPortletFeature() {
    return ALAccessControlConstants.POERTLET_FEATURE_MSGBOARD_CATEGORY;
  }

}
