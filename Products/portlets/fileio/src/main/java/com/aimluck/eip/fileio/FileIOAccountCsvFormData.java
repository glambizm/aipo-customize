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
package com.aimluck.eip.fileio;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.cayenne.DataRow;                                              // <#01>
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.jetspeed.services.JetspeedSecurity;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.jetspeed.services.resources.JetspeedResources;
import org.apache.turbine.services.TurbineServices;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.aimluck.eip.cayenne.om.portlet.EipTMsgboardCategory;                 // <#01>
import com.aimluck.eip.cayenne.om.portlet.EipTMsgboardCategoryMap;              // <#01>
import com.aimluck.commons.field.ALNumberField;
import com.aimluck.commons.field.ALStringField;
import com.aimluck.commons.utils.ALStringUtil;
import com.aimluck.eip.account.util.AccountUtils;
import com.aimluck.eip.cayenne.om.account.EipMPosition;
import com.aimluck.eip.cayenne.om.account.EipMPost;
import com.aimluck.eip.cayenne.om.portlet.EipTExtTimecardSystem;
import com.aimluck.eip.cayenne.om.portlet.EipTExtTimecardSystemMap;
import com.aimluck.eip.cayenne.om.security.TurbineGroup;
import com.aimluck.eip.cayenne.om.security.TurbineUser;
import com.aimluck.eip.cayenne.om.security.TurbineUserGroupRole;
import com.aimluck.eip.common.ALAbstractFormData;
import com.aimluck.eip.common.ALBaseUser;
import com.aimluck.eip.common.ALDBErrorException;
import com.aimluck.eip.common.ALPageNotFoundException;
import com.aimluck.eip.orm.Database;
import com.aimluck.eip.orm.query.SQLTemplate;                                   // <#01>
import com.aimluck.eip.orm.query.SelectQuery;
import com.aimluck.eip.services.accessctl.ALAccessControlFactoryService;
import com.aimluck.eip.services.accessctl.ALAccessControlHandler;
import com.aimluck.eip.services.datasync.ALDataSyncFactoryService;
import com.aimluck.eip.util.ALEipUtils;
import com.aimluck.eip.util.ALLocalizationUtils;
import com.aimluck.eip.msgboard.util.MsgboardUtils;                             // <#01>

/**
 * 『アカウント』のフォームデータを管理するクラスです。 <BR>
 *
 */
public class FileIOAccountCsvFormData extends ALAbstractFormData {

  /** logger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(FileIOAccountCsvFormData.class.getName());

  /** ブラウザに表示するデフォルトのパスワード（ダミーパスワード） */
  public static final String DEFAULT_VIEW_PASSWORD = "*";

  /** ユーザー名 */
  private ALStringField username;

  /** パスワード */
  private ALStringField password;

  /** 名前（名） */
  private ALStringField firstname;

  /** 名前（姓） */
  private ALStringField lastname;

  /** フリガナ（名） */
  private ALStringField first_name_kana;

  /** フリガナ（姓） */
  private ALStringField last_name_kana;

  /** メールアドレス */
  private ALStringField email;

  /** 電話番号（内線） */
  private ALStringField in_telephone;

  /** 電話番号（外線） */
  private ALStringField out_telephone;

  /** 電話番号（携帯） */
  private ALStringField cellular_phone;

  /** 携帯メールアドレス */
  private ALStringField cellular_mail;

  /** 部署名 */
  private ALStringField post_name;

  /** 部署リスト */
  private List<String> post_name_list;

  /** 役職 */
  private ALStringField position_name;

  /** 社員コード */
  private ALStringField code;

  /** 部署がデータベースに存在するか否か */
  private boolean post_not_found;

  /** 役職がデータベースに存在するか否か */
  private boolean position_not_found;

  private boolean isSkipUsernameValidation = false;

  /**
   * 各フィールドを初期化します。 <BR>
   *
   *
   */
  @Override
  public void initField() {
    // ユーザー名
    username = new ALStringField();
    username.setFieldName(ALLocalizationUtils.getl10n("FILEIO_USER_NAME"));
    username.setTrim(true);
    // パスワード
    password = new ALStringField();
    password.setFieldName(ALLocalizationUtils.getl10n("FILEIO_PASSWORD"));
    password.setTrim(true);
    // 名
    firstname = new ALStringField();
    firstname.setFieldName(ALLocalizationUtils.getl10n("FILEIO_LAST_NAME"));
    firstname.setTrim(true);
    // 姓
    lastname = new ALStringField();
    lastname.setFieldName(ALLocalizationUtils.getl10n("FILEIO_FIRST_NAME"));
    lastname.setTrim(true);
    // メールアドレス
    email = new ALStringField();
    email.setFieldName(ALLocalizationUtils.getl10n("FILEIO_MAILADDRESS"));
    email.setTrim(true);
    // 内線番号
    in_telephone = new ALStringField();
    in_telephone.setFieldName(ALLocalizationUtils
      .getl10n("FILEIO_EXTENTION_TELL_NUMBER"));
    in_telephone.setTrim(true);

    // 外線番号
    out_telephone = new ALStringField();
    out_telephone.setFieldName(ALLocalizationUtils
      .getl10n("FILEIO_OUTSIDE_TELL_NUMBER"));
    out_telephone.setTrim(true);
    // 携帯番号
    cellular_phone = new ALStringField();
    cellular_phone.setFieldName(ALLocalizationUtils
      .getl10n("FILEIO_MOBILE_PHONE"));
    cellular_phone.setTrim(true);

    // 携帯アドレス
    cellular_mail = new ALStringField();
    cellular_mail.setFieldName(ALLocalizationUtils
      .getl10n("FILEIO_MOBILE_PHONE_ADDRESS"));
    cellular_mail.setTrim(true);
    // 名（フリガナ）
    first_name_kana = new ALStringField();
    first_name_kana.setFieldName(ALLocalizationUtils
      .getl10n("FILEIO_NAME_SPELL"));
    first_name_kana.setTrim(true);
    // 姓（フリガナ）
    last_name_kana = new ALStringField();
    last_name_kana.setFieldName(ALLocalizationUtils
      .getl10n("FILEIO_NAME_SPELL"));
    last_name_kana.setTrim(true);

    // 部署名
    post_name_list = new ArrayList<String>();
    post_name = new ALStringField();
    post_name.setFieldName(ALLocalizationUtils.getl10n("FILIIO_UNIT_NAME"));
    post_name.setTrim(true);

    // 役職
    position_name = new ALStringField();
    position_name.setFieldName(ALLocalizationUtils.getl10n("FILEIO_POST"));
    position_name.setTrim(true);

    // 社員コード
    code = new ALStringField();
    code.setFieldName(ALLocalizationUtils.getl10n("FILEIO_CODE"));
    code.setTrim(true);

    setPostNotFound(false);
    setPositionNotFound(false);
  }

  /**
   * 各フィールドに対する制約条件を設定します。 <BR>
   *
   *
   */
  @Override
  protected void setValidator() {
    // ユーザー名
    username.setNotNull(true);
    username.setCharacterType(ALStringField.TYPE_ASCII);
    username.limitMaxLength(16);
    // パスワード
    password.setNotNull(true);
    password.setCharacterType(ALStringField.TYPE_ASCII);
    password.limitMaxLength(16);
    // 名
    firstname.setNotNull(true);
    firstname.limitMaxLength(20);
    // 姓
    lastname.setNotNull(true);
    lastname.limitMaxLength(20);

    // 名（フリガナ）
    first_name_kana.setNotNull(true);
    first_name_kana.limitMaxLength(20);
    // 姓（フリガナ）
    last_name_kana.setNotNull(true);
    last_name_kana.limitMaxLength(20);

    // 内線
    in_telephone.setCharacterType(ALStringField.TYPE_ASCII);
    in_telephone.limitMaxLength(15);
    // メールアドレス
    email.setCharacterType(ALStringField.TYPE_ASCII);
    email.limitMaxLength(50);

    // 外線
    out_telephone.setCharacterType(ALStringField.TYPE_NUMBER);
    out_telephone.limitMaxLength(15);

    // 携帯
    cellular_phone.setCharacterType(ALStringField.TYPE_NUMBER);
    cellular_phone.limitMaxLength(15);
    // 携帯メール
    cellular_mail.setCharacterType(ALStringField.TYPE_ASCII);
    cellular_mail.limitMaxLength(50);

    // 部署名
    post_name.limitMaxLength(50);
    // 役職
    position_name.limitMaxLength(50);
    // 社員コード
    code.limitMaxLength(100);

  }

  /**
   * フォームに入力されたデータの妥当性検証を行います。 <BR>
   *
   * @param msgList
   * @return
   *
   */
  @Override
  protected boolean validate(List<String> msgList) {
    if (!isSkipUsernameValidation) {
      String usernamestr = username.getValue();
      if (usernamestr == null
        || "admin".equals(usernamestr)
        || "template".equals(usernamestr)
        || "anon".equals(usernamestr)
        || usernamestr.startsWith(ALEipUtils.dummy_user_head)
        || !username.validate(msgList)) {
        msgList.add(ALLocalizationUtils.getl10n("FILEIO_USER_NAME_CAUTION"));
        username.setValue(null);
      }

      if (usernamestr != null
        && !AccountUtils.isValidSymbolUserName(usernamestr)) {
        StringBuffer msg =
          new StringBuffer(ALLocalizationUtils.getl10n("FILEIO_USER_NAME_MARK"));
        List<String> symbols = Arrays.asList(AccountUtils.USER_NAME_SYMBOLS);
        for (String symbol : symbols) {
          msg.append("『").append(symbol).append("』");
        }
        msg.append(ALLocalizationUtils.getl10n("FILEIO_ONLY"));
        msgList.add(msg.toString());
        username.setValue(null);
      }

      // if (usernameList.contains(usernamestr)) {
      // username.setValue(null);
      // msgList.add("<span class='em'>同じユーザー名は複数登録できません</span>");
      // } else {
      // usernameList.add(usernamestr);
      // }

      Map<String, TurbineUser> existedUserMap = getAllUsersFromDB();
      if (existedUserMap == null) {
        existedUserMap = new LinkedHashMap<String, TurbineUser>();
      }
      if (existedUserMap.containsKey(usernamestr)) {
        TurbineUser tmpuser2 = existedUserMap.get(usernamestr);
        if (!("F".equals(tmpuser2.getDisabled()))) {
          msgList.add(ALLocalizationUtils
            .getl10n("FILEIO_NOT_MULTIPLE_REGISTRATION"));
          username.setValue(null);
        }
      }
    }

    // パスワードの確認
    if (!password.getValue().equals(DEFAULT_VIEW_PASSWORD)) {
      if (!password.validate(msgList)) {
        password.setValue(null);
      }
    }

    if (!firstname.validate(msgList)) {
      firstname.setValue(null);
      lastname.setValue(null);
    }
    if (!lastname.validate(msgList)) {
      firstname.setValue(null);
      lastname.setValue(null);
    }

    // フリガナのカタカナへの変換
    first_name_kana.setValue(ALStringUtil.convertHiragana2Katakana(ALStringUtil
      .convertH2ZKana(first_name_kana.toString())));
    last_name_kana.setValue(ALStringUtil.convertHiragana2Katakana(ALStringUtil
      .convertH2ZKana(last_name_kana.toString())));

    if (!first_name_kana.validate(msgList)) {
      first_name_kana.setValue(null);
      last_name_kana.setValue(null);
    }
    if (!last_name_kana.validate(msgList)) {
      first_name_kana.setValue(null);
      last_name_kana.setValue(null);
    }

    // メールアドレス
    if (email.getValue() != null && !email.getValue().equals("")) {
      if (!email.validate(msgList)
        || (email.getValue() != null && email.getValue().trim().length() > 0 && !ALStringUtil
          .isMailAddress(email.getValue()))) {
        email.setValue(null);
        msgList.add(ALLocalizationUtils.getl10n("FILEIO_MAILADDRESS_CAUTION"));
      }
    } else {
      email.setValue("");
    }

    ALNumberField tel = new ALNumberField();
    List<String> errmsg = new ArrayList<String>();
    boolean isNumber = true;
    if (out_telephone.getValue() != null
      && !out_telephone.getValue().equals("")) {
      String[] out_tels = out_telephone.getValue().split("-");
      if (out_tels.length == 3) {
        for (int i = 0; i < 3; i++) {
          tel.setValue(out_tels[i]);
          isNumber = isNumber & tel.validate(errmsg);
        }
        if (!isNumber
          || out_tels[0].length() > 5
          || out_tels[1].length() > 4
          || out_tels[2].length() > 4) {
          out_telephone.setValue(null);
          msgList.add(ALLocalizationUtils
            .getl10n("FILEIO_PHONE_NUMBER_CAUTION"));
        }
      } else {
        out_telephone.setValue(null);
        msgList.add(ALLocalizationUtils.getl10n("FILEIO_PHONE_NUMBER_CAUTION"));
      }
    } else {
      out_telephone.setValue("");
    }

    if (!in_telephone.validate(msgList)) {
      in_telephone.setValue(null);
    } else if (in_telephone.getValue() != null
      && !in_telephone.getValue().equals("")) {
      Pattern ptn = Pattern.compile("[-0-9]+");/* 半角数字とハイフンのみの文字列ならマッチ */
      Matcher mc = ptn.matcher(in_telephone.getValue().toString());

      if (!mc.matches()) {
        in_telephone.setValue(null);
        msgList.add(ALLocalizationUtils
          .getl10n("FILEIO_EXTENTION_NUMBER_CAUTION"));
      }
    } else {
      in_telephone.setValue("");
    }

    isNumber = true;

    if (cellular_phone.getValue() != null
      && !cellular_phone.getValue().equals("")) {
      String[] cell_tels = cellular_phone.getValue().split("-");
      if (cell_tels.length == 3) {
        for (int i = 0; i < 3; i++) {
          tel.setValue(cell_tels[i]);
          isNumber = isNumber & tel.validate(errmsg);
        }
        if (!isNumber
          || cell_tels[0].length() > 5
          || cell_tels[1].length() > 4
          || cell_tels[2].length() > 4) {
          cellular_phone.setValue(null);
          msgList.add(ALLocalizationUtils
            .getl10n("FILEIO_CELLPHONE_NUMBER_CAUTION"));
        }
      } else {
        cellular_phone.setValue(null);
        msgList.add(ALLocalizationUtils
          .getl10n("FILEIO_CELLPHONE_NUMBER_CAUTION"));
      }
    } else {
      cellular_phone.setValue("");
    }

    // 携帯メールアドレス
    if (cellular_mail.getValue() != null
      && !cellular_mail.getValue().equals("")) {
      if (!cellular_mail.validate(msgList)) {
        cellular_mail.setValue(null);
        msgList.add(ALLocalizationUtils
          .getl10n("FILEIO_CELLPHONE_MAILADDRESS_CAUTION"));
      } else if (cellular_mail.getValue().trim().length() > 0
        && !ALStringUtil.isCellPhoneMailAddress(cellular_mail.getValue())) {
        cellular_mail.setValue(null);
        msgList.add(ALLocalizationUtils
          .getl10n("FILEIO_CELLPHONE_MAILADDRESS_CAUTION"));
      }
    } else {
      cellular_mail.setValue("");
    }

    if (post_name.getValue() != null && !post_name.getValue().equals("")) {

      // ArrayList postnames = new ArrayList();
      String[] st = post_name.toString().split("[/、]");
      List<String> postCollection = new ArrayList<String>();
      for (int k = 0; k < st.length; k++) {
        ALStringField post = new ALStringField(st[k]);
        if (!postCollection.contains(post.toString())) {
          postCollection.add(post.toString());
        }
        if (post.validate(msgList)) {
          if ((!st[k].equals("")) && (getEipMPost(post) == null)) {
            setPostNotFound(true);
            msgList.add(ALLocalizationUtils.getl10n("FILEIO_NO_POST"));
          }
        } else {
          msgList.add(ALLocalizationUtils.getl10n("FILEIO_NO_POST_NAME"));
          post_name.setValue(null);
          break;
        }
      }

      if (!post_name.toString().equals("") && post_name.toString() != null) {
        StringBuffer sb = new StringBuffer();
        for (int k = 0; k < postCollection.size(); k++) {
          if (k != 0) {
            sb.append("/");
          }
          sb.append(postCollection.get(k));
        }
        post_name.setValue(sb.toString());
      }
    } else {
      post_name.setValue("");
    }

    if (position_name.getValue() != null
      && !position_name.getValue().equals("")) {
      if (!position_name.validate(msgList)) {
        position_name.setValue(null);
        msgList.add(ALLocalizationUtils.getl10n("FILEIO_NO_POSITION_CAUTION"));
      } else if ((!position_name.toString().equals(""))
        && (getEipMPosition() == null)) {
        setPositionNotFound(true);
        msgList.add(ALLocalizationUtils.getl10n("FILEIO_NO_POSITION"));
      }
    } else {
      position_name.setValue("");
    }

    if (!code.validate(msgList)) {
      code.setValue(null);
    }

    return (msgList.size() == 0);
  }

  @Override
  protected boolean setFormData(RunData rundata, Context context,
      List<String> msgList) throws ALPageNotFoundException, ALDBErrorException {
    return super.setFormData(rundata, context, msgList);
  }

  /**
   * 『ユーザー』を読み込みます。 <BR>
   *
   * @param rundata
   * @param context
   * @param msgList
   * @return
   */
  @Override
  protected boolean loadFormData(RunData rundata, Context context,
      List<String> msgList) {
    return false;
  }

  /**
   * 『ユーザー』を追加します。 <BR>
   *
   * @param rundata
   * @param context
   * @param msgList
   * @return
   */
  @Override
  protected boolean insertFormData(RunData rundata, Context context,
      List<String> msgList) {

    // WebAPIのDBへ接続できるか確認
    if (!ALDataSyncFactoryService
      .getInstance()
      .getDataSyncHandler()
      .checkConnect()) {
      msgList.add(ALLocalizationUtils.getl10n("FILEIO_FAILED"));
      return false;
    }

    boolean res = true;
    try {
      boolean isNewUser = false;
      ALBaseUser user = null;
      try {
        user = (ALBaseUser) JetspeedSecurity.getUser(getUserName().getValue());
        // ユーザー名
        user.setUserName(JetspeedSecurity.convertUserName(username.getValue()));
        if (!password.getValue().equals(DEFAULT_VIEW_PASSWORD)) {
          JetspeedSecurity.forcePassword(user, password.getValue());
        }
        isNewUser = false;
      } catch (Exception e) {
        // オブジェクトモデルを生成
        user = (ALBaseUser) JetspeedSecurity.getUserInstance();
        rundata.getParameters().setProperties(user);
        // ユーザー名
        user.setUserName(JetspeedSecurity.convertUserName(getUserName()
          .getValue()));
        // JetspeedSecurity.forcePassword(user, password.getValue());
        user.setPassword(password.getValue());
        isNewUser = true;
      }

      Date now = new Date();
      // 作成日
      // 以下のメソッドは動作しないため、ALBaseUserにてオーバーライド
      // user.setCreateDate(now);
      // JetspeedSecurity.forcePassword(user, password.getValue());
      user.setCreated(now);
      user.setModified(now);
      user.setLastLogin(now);
      user.setCreatedUserId(ALEipUtils.getUserId(rundata));
      user.setUpdatedUserId(ALEipUtils.getUserId(rundata));
      user.setConfirmed(JetspeedResources.CONFIRM_VALUE);
      user.setDisabled("F");
      // user.setPassword(password.getValue());
      user.setPasswordChanged(now);
      user.setInTelephone(in_telephone.getValue());
      user.setOutTelephone(out_telephone.getValue());
      user.setCellularPhone(cellular_phone.getValue());
      user.setCellularMail(cellular_mail.getValue());
      user.setCompanyId(1);
      user.setPositionId(0);
      user.setPostId(0);
      user.setFirstName(getFirstName().getValue());
      user.setLastName(getLastName().getValue());
      user.setFirstNameKana(first_name_kana.getValue());
      user.setLastNameKana(last_name_kana.getValue());
      user.setEmail(getEmail().getValue());
      user.setMigrateVersion(0);
      user.setCode(code.getValue());

      if (!position_name.getValue().equals("")) {
        EipMPosition position = getEipMPosition();
        if (position != null) {
          user.setPositionId(position.getPositionId());
        }
      }

      if (!isNewUser) {
        // ユーザーを既にいる部署から削除
        SelectQuery<TurbineUserGroupRole> query2 =
          Database.query(TurbineUserGroupRole.class);
        Expression exp2 =
          ExpressionFactory.matchExp(
            TurbineUserGroupRole.TURBINE_USER_PROPERTY,
            user.getUserId());
        Expression exp3 =
          ExpressionFactory.noMatchExp(
            TurbineUserGroupRole.TURBINE_GROUP_PROPERTY,
            Integer.valueOf(1));
        Expression exp4 =
          ExpressionFactory.noMatchExp(
            TurbineUserGroupRole.TURBINE_GROUP_PROPERTY,
            Integer.valueOf(2));
        query2.setQualifier(exp2);
        query2.andQualifier(exp3.andExp(exp4));
        List<TurbineUserGroupRole> list = query2.fetchList();
        TurbineUserGroupRole ugr = null;
        for (int i = 0; i < list.size(); i++) {
          ugr = list.get(i);
          if (isPost(ugr.getTurbineGroup().getGroupName())) {
            Database.delete(ugr);
          }
        }

        // ユーザーを部署に追加
        if (!post_name.toString().equals("") && post_name.toString() != null) {
          String[] postnames = post_name.toString().split("/");
          for (int i = 0; i < postnames.length; i++) {
            SelectQuery<TurbineGroup> query =
              Database.query(TurbineGroup.class);
            Expression exp =
              ExpressionFactory.matchExp(
                TurbineGroup.GROUP_ALIAS_NAME_PROPERTY,
                postnames[i]);
            query.setQualifier(exp);
            List<TurbineGroup> alist = query.fetchList();
            JetspeedSecurity.joinGroup(user.getUserName(), (alist.get(0))
              .getName());
          }
        }
        // ユーザーを更新
        JetspeedSecurity.saveUser(user);
      } else {
        int postid = 0;
        EipMPost post = getEipMPost(post_name);
        if (post != null) {
          postid = post.getPostId();
          user.setPostId(postid);
        }
        // ユーザーを追加
        JetspeedSecurity.addUser(user);

        // // 部署Mapを取得
        // Map map = ALEipManager.getInstance().getPostMap();
        // int size = map.size();
        // if (map != null && size != 0 && postid >= 1) {
        // // グループへユーザを登録
        // JetspeedSecurity.joinGroup(user.getUserName(),
        // ((ALEipPost) ALEipManager.getInstance().getPostMap().get(
        // Integer.valueOf(postid))).getGroupName().getValue());
        // }

        // // ログインユーザーにはグループ LoginUser に所属させる
        // JetspeedSecurity.joinGroup(user.getUserName(), "LoginUser");
        // logger.debug("JOIN GROUP:" + "LoginUser");

        // ユーザーをグループに追加。
        if (!post_name.toString().equals("") && post_name.toString() != null) {
          String[] postnames = post_name.toString().split("/");
          for (int i = 0; i < postnames.length; i++) {
            SelectQuery<TurbineGroup> query =
              Database.query(TurbineGroup.class);
            Expression exp =
              ExpressionFactory.matchExp(
                TurbineGroup.GROUP_ALIAS_NAME_PROPERTY,
                postnames[i]);
            query.setQualifier(exp);
            List<TurbineGroup> alist = query.fetchList();
            JetspeedSecurity.joinGroup(user.getUserName(), (alist.get(0))
              .getName());
          }
        }

        // // ユーザの順番を登録する．
        // StringBuffer statement = new StringBuffer();
        // statement.append("INSERT INTO eip_m_user_position ");
        // statement.append("(USER_ID, POSITION) VALUES (");
        // statement.append(user.getUserId());
        // statement
        // .append(", (SELECT COALESCE(MAX(EIP_M_USER_POSITION.POSITION),0)+1");
        // statement.append(" FROM EIP_M_USER_POSITION))");
        // String query = statement.toString();
        // orm.executeStatement(query);

        // 初期メールアカウントの作成
        // if (email.getValue() != null && (!email.getValue().equals(""))) {
        // ALMailUtils.insertMailAccountData(rundata, msgList, Integer
        // .parseInt(user.getUserId()), "初期アカウント",
        // ALMailUtils.ACCOUNT_TYPE_INIT, email.getValue(), "未設定", "未設定",
        // 25, "未設定", 110, "未設定", "未設定", ALSmtpMailSender.AUTH_SEND_NONE,
        // null, null, ALPop3MailReceiver.AUTH_RECEIVE_NORMAL, 0, 0, 1, "0");
        // }

        // ACLの登録
        int userid = Integer.parseInt(user.getUserId());

        // アクセス権限
        ALAccessControlFactoryService aclservice =
          (ALAccessControlFactoryService) ((TurbineServices) TurbineServices
            .getInstance())
            .getService(ALAccessControlFactoryService.SERVICE_NAME);
        ALAccessControlHandler aclhandler =
          aclservice.getAccessControlHandler();
        aclhandler.insertDefaultRole(userid);

        // 勤務形態
        EipTExtTimecardSystem system =
          Database.get(EipTExtTimecardSystem.class, 1);
        if (system != null) {
          EipTExtTimecardSystemMap rd = new EipTExtTimecardSystemMap();
          rd.setEipTExtTimecardSystem(system);
          int user_id = Integer.parseInt(user.getUserId());
          rd.setUserId(user_id);
          rd.setCreateDate(now);
          rd.setUpdateDate(now);
        }

// <#01> --- S
        // 掲示板カテゴリ
        TurbineUser tuser = Database.get(TurbineUser.class, Integer.parseInt(user.getUserId()));
        Integer employeeType = tuser.getEmployeeType();

        if (employeeType == TurbineUser.EMPLOYEE_TYPE_EMPLOYEE) {
          // 社員
          StringBuilder sql =
            new StringBuilder().append("SELECT * FROM eip_t_msgboard_category ")
                               .append(" WHERE SUBSTR(category_name,1,1)=")
                               .append("\'" + TurbineUser.EMPLOYEE_MSGBOARD_CATEGORY_MARK_EMPLOYEE + "\'");

          SQLTemplate<EipTMsgboardCategory> sqltemp =
            Database.sql(EipTMsgboardCategory.class, String.valueOf(sql));

          List<DataRow> fetchList = sqltemp.fetchListAsDataRow();
          List<EipTMsgboardCategory> list = new ArrayList<EipTMsgboardCategory>();
          for (DataRow row : fetchList) {
            EipTMsgboardCategory object = Database.objectFromRowData(row, EipTMsgboardCategory.class);
            EipTMsgboardCategory eipTMsgboardCategory = Database.get(EipTMsgboardCategory.class, object.getCategoryId());
            list.add(eipTMsgboardCategory);
          }

          for (EipTMsgboardCategory category : list) {
            EipTMsgboardCategoryMap map = Database.create(EipTMsgboardCategoryMap.class);
            map.setEipTMsgboardCategory(category);
            map.setUserId(Integer.valueOf(userid));
            map.setStatus(MsgboardUtils.STAT_VALUE_SHARE);
          }
        }
// <#01> --- E

        Database.commit();
      }

      // WebAPIとのDB同期
      if (!ALDataSyncFactoryService.getInstance().getDataSyncHandler().addUser(
        user)) {
        return false;
      }

    } catch (Exception e) {
      Database.rollback();
      logger.error("fileio", e);
      res = false;
    }

    return res;
  }

  /**
   * 『ユーザー』を更新します。 <BR>
   *
   * @param rundata
   * @param context
   * @param msgList
   * @return
   */
  @Override
  protected boolean updateFormData(RunData rundata, Context context,
      List<String> msgList) {
    return false;
  }

  /**
   * 『ユーザー』を削除します。 <BR>
   *
   * @param rundata
   * @param context
   * @param msgList
   * @return
   */
  @Override
  protected boolean deleteFormData(RunData rundata, Context context,
      List<String> msgList) {
    return false;
  }

  /**
   * 携帯メールアドレスを取得します。 <BR>
   *
   * @return
   */
  public ALStringField getCellularMail() {
    return cellular_mail;
  }

  /**
   * メールアドレスを取得します。 <BR>
   *
   * @return
   */
  public ALStringField getEmail() {
    return email;
  }

  /**
   * フリガナ（名）を取得します。 <BR>
   *
   * @return
   */
  public ALStringField getFirstNameKana() {
    return first_name_kana;
  }

  /**
   * 名前（名）を取得します。 <BR>
   *
   * @return
   */
  public ALStringField getFirstName() {
    return firstname;
  }

  /**
   * 電話番号（内線）を取得します。 <BR>
   *
   * @return
   */
  public ALStringField getInTelephone() {
    return in_telephone;
  }

  /**
   * フリガナ（姓）を取得します。 <BR>
   *
   * @return
   */
  public ALStringField getLastNameKana() {
    return last_name_kana;
  }

  /**
   * 名前（姓）を取得します。 <BR>
   *
   * @return
   */
  public ALStringField getLastName() {
    return lastname;
  }

  /**
   * 携帯電話番号を取得します。 <BR>
   *
   * @return
   */
  public ALStringField getCellularPhone() {
    return cellular_phone;
  }

  /**
   * 電話番号を取得します。 <BR>
   *
   * @return
   */
  public ALStringField getOutTelephone() {
    return out_telephone;
  }

  /**
   * パスワードを取得します。 <BR>
   *
   * @return
   */
  public ALStringField getPassword() {
    return password;
  }

  /**
   * ユーザー名を取得します。 <BR>
   *
   * @return
   */
  public ALStringField getUserName() {
    return username;
  }

  /**
   * 部署名を取得します <BR>
   *
   * @return
   */
  public ALStringField getPostName() {
    return post_name;
  }

  public List<String> getPostNameList() {
    return post_name_list;
  }

  /**
   * 役職名を取得します <BR>
   *
   * @return
   */
  public ALStringField getPositionName() {
    return position_name;
  }

  /**
   * 社員コードを取得します <BR>
   *
   * @return
   */
  public ALStringField getCode() {
    return code;
  }

  /**
   * 部署がデータベースに存在するかを示すフラグを取得します <BR>
   *
   * @return
   */
  public boolean getPostNotFound() {
    return post_not_found;
  }

  /**
   * 役職がデータベースに存在するかを示すフラグを取得します <BR>
   *
   * @return
   */
  public boolean getPositionNotFound() {
    return position_not_found;
  }

  /**
   * 携帯メールアドレスを入力します <BR>
   *
   * @param str
   */
  public void setCellularMail(String str) {
    cellular_mail.setValue(str);
  }

  /**
   * メールアドレスを入力します <BR>
   *
   * @param str
   */
  public void setEmail(String str) {
    email.setValue(str);
  }

  /**
   * フリガナ（名）を入力します <BR>
   *
   * @param str
   */
  public void setFirstNameKana(String str) {
    first_name_kana.setValue(str);
  }

  /**
   * 名前（名）を入力します <BR>
   *
   * @param str
   */
  public void setFirstName(String str) {
    firstname.setValue(str);
  }

  /**
   * フリガナ（氏）を入力します <BR>
   *
   * @param str
   */
  public void setLastNameKana(String str) {
    last_name_kana.setValue(str);
  }

  /**
   * 名前（氏）を入力します <BR>
   *
   * @param str
   */
  public void setLastName(String str) {
    lastname.setValue(str);
  }

  /**
   * 携帯電話番号を入力します <BR>
   *
   * @param str
   */
  public void setCellularPhone(String str) {
    cellular_phone.setValue(str);
  }

  /**
   * パスワードを入力します <BR>
   *
   * @param str
   */
  public void setPassword(String str) {
    password.setValue(str);
  }

  /**
   * ユーザー名を入力します <BR>
   *
   * @param str
   */
  public void setUserName(String str) {
    username.setValue(str);
  }

  /**
   * 部署名を入力します <BR>
   *
   * @param str
   */
  public void setPostName(String str) {
    post_name.setValue(str);
  }

  public void setPostNameList(List<String> list) {
    post_name_list.addAll(list);
  }

  /**
   * 役職名を入力します <BR>
   *
   * @param str
   */
  public void setPositionName(String str) {
    position_name.setValue(str);
  }

  /**
   * 社員コードを入力します <BR>
   *
   * @param str
   */
  public void setCode(String str) {
    code.setValue(str);
  }

  /**
   * 電話番号を入力します（部署） <BR>
   *
   * @param str
   */
  public void setOutTelephone(String str) {
    out_telephone.setValue(str);
  }

  /**
   * 内線番号を入力します（部署） <BR>
   *
   * @param str
   */
  public void setInTelephone(String str) {
    in_telephone.setValue(str);
  }

  /**
   * 部署がデータベースに存在するかを示すフラグを入力します <BR>
   *
   * @param flg
   */
  public void setPostNotFound(boolean flg) {
    post_not_found = flg;
  }

  /**
   * 役職がデータベースに存在するかを示すフラグを入力します <BR>
   *
   * @param flg
   */
  public void setPositionNotFound(boolean flg) {
    position_not_found = flg;
  }

  /**
   * 部署名から部署IDを取得 <BR>
   *
   * @return
   */
  private EipMPost getEipMPost(ALStringField post_name) {
    SelectQuery<EipMPost> query = Database.query(EipMPost.class);
    Expression exp =
      ExpressionFactory.matchExp(EipMPost.POST_NAME_PROPERTY, post_name);
    query.setQualifier(exp);
    List<EipMPost> list = query.fetchList();
    if (list == null || list.size() == 0) {
      return null;
    }
    EipMPost post = list.get(0);
    return post;
  }

  /**
   * Myグループが部署かどうか判定 <BR>
   *
   * @return
   */
  private boolean isPost(String group_name) {
    SelectQuery<EipMPost> query = Database.query(EipMPost.class);
    ALStringField group_name_field = new ALStringField(group_name);
    Expression exp =
      ExpressionFactory
        .matchExp(EipMPost.GROUP_NAME_PROPERTY, group_name_field);
    query.setQualifier(exp);
    if (query.getCount() == 0) {
      return false;
    } else {
      return true;
    }
  }

  /**
   * 役職名から役職IDを取得 <BR>
   *
   * @return
   */
  private EipMPosition getEipMPosition() {
    SelectQuery<EipMPosition> query = Database.query(EipMPosition.class);
    Expression exp =
      ExpressionFactory.matchExp(
        EipMPosition.POSITION_NAME_PROPERTY,
        position_name);
    query.setQualifier(exp);
    List<EipMPosition> list = query.fetchList();
    if (list == null || list.size() == 0) {
      return null;
    }
    EipMPosition position = list.get(0);
    return position;
  }

  /**
   * 読み取った単語を指定されたフィールドに格納します。 <BR>
   *
   * @param token
   * @param i
   */
  public void addItemToken(String token, int i) {
    switch (i) {
      case -1:
        break;
      case 0:
        try {
          setUserName(token);
        } catch (Exception e) {
          logger.error(e);
        }
        break;
      case 1:
        setPassword(token);
        break;
      case 2:
        setLastName(token);
        break;
      case 3:
        setFirstName(token);
        break;
      case 4:
        setLastNameKana(token);
        break;
      case 5:
        setFirstNameKana(token);
        break;
      case 6:
        setEmail(token);
        break;
      case 7:
        setOutTelephone(token);
        break;
      case 8:
        setInTelephone(token);
        break;
      case 9:
        setCellularPhone(token);
        break;
      case 10:
        setCellularMail(token);
        break;
      case 11:
        setPostName(token);
        break;
      case 12:
        setPositionName(token);
        break;
      case 13:
        setCode(token);
        break;
      default:
        break;
    }
  }

  /**
   *
   * @return
   */
  private Map<String, TurbineUser> getAllUsersFromDB() {
    Map<String, TurbineUser> map = null;
    try {
      SelectQuery<TurbineUser> query = Database.query(TurbineUser.class);
      List<TurbineUser> list = query.fetchList();

      map = new LinkedHashMap<String, TurbineUser>();
      TurbineUser user = null;
      int size = list.size();
      for (int i = 0; i < size; i++) {
        user = list.get(i);
        map.put(user.getLoginName(), user);
      }
    } catch (Exception ex) {
      logger.error("[ALEipUtils]", ex);
      // throw new ALDBErrorException();
    }
    return map;
  }

  public boolean isSkipUsernameValidation() {
    return isSkipUsernameValidation;
  }

  public void setSkipUsernameValidation(boolean isSkipUsernameValidation) {
    this.isSkipUsernameValidation = isSkipUsernameValidation;
  }
}
