package org.nisshiee.acsclient;

import java.io.PrintStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * [Immutable][Singleton]コミュニティの掲示板に投稿します
 * 
 * @author Hirokazu NISHIOKA
 * 
 */
public class BbsPoster {
	/**
	 * Server encoding
	 */
	private static final String ENCODING = "EUC-JP";
	/**
	 * Login URL
	 */
	private static final String LOGIN_URL = "https://acs.is.nagoya-u.ac.jp/login/index.php?module=User&action=Login";
	/**
	 * Post URL format(%1$:community_id %2$d:bbs_id)
	 */
	private static final String POST_URL_FORMAT = "https://acs.is.nagoya-u.ac.jp/login/index.php?module=Community&action=BBSResPre&community_id=%1$d&bbs_id=%2$d&move_id=1";
	/**
	 * Confirm URL format(%1$:community_id %2$d:bbs_id)
	 */
	private static final String CONFIRM_URL_FORMAT = "https://acs.is.nagoya-u.ac.jp/login/index.php?module=Community&action=BBSResPre&community_id=%1$d&bbs_id=%2$d&move_id=2";

	/**
	 * Singleton instance
	 */
	private static BbsPoster instance = null;

	/**
	 * インスタンスを取得
	 * 
	 * @return
	 */
	public synchronized static BbsPoster getInstance() {
		if (instance == null) {
			instance = new BbsPoster();
		}
		return instance;
	}

	/**
	 * コンストラクタ
	 */
	private BbsPoster() {
	}

	/**
	 * 掲示板に投稿
	 * 
	 * @param userId
	 *            名大ID
	 * @param password
	 *            パスワード
	 * @param communityId
	 *            コミュニティID
	 * @param bbsId
	 *            掲示板ID
	 * @param subject
	 *            タイトル
	 * @param body
	 *            内容
	 * @return 成否
	 */
	public synchronized boolean post(String userId, String password,
			int communityId, int bbsId, String subject, String body) {
		// In-memory CookieStoreを設定
		CookieManager cookieManager = new CookieManager();
		cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
		CookieHandler.setDefault(cookieManager);

		// Login
		{
			Map<String, String> loginParams = new LinkedHashMap<String, String>();
			loginParams.put("module", "User");
			loginParams.put("action", "Login");
			loginParams.put("search", "1");
			loginParams.put("userid", userId);
			loginParams.put("passwd", password);
			if (!httpPost(LOGIN_URL, loginParams)) {
				return false;
			}
		}

		// Post
		{
			String postUrl = String.format(POST_URL_FORMAT, communityId, bbsId);
			Map<String, String> postParams = new LinkedHashMap<String, String>();
			postParams.put("subject", subject);
			postParams.put("body", body);
			if (!httpPost(postUrl, postParams)) {
				return false;
			}
		}

		// Confirm
		{
			String confirmUrl = String.format(CONFIRM_URL_FORMAT, communityId,
					bbsId);
			Map<String, String> confirmParams = new LinkedHashMap<String, String>();
			confirmParams.put("except_community_id_array[]",
					Integer.toString(communityId));
			if (!httpPost(confirmUrl, confirmParams)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * POSTメソッドを使ったHTTP通信
	 * 
	 * @param urlString
	 *            URL文字列
	 * @param params
	 *            パラメータ
	 * @return 成否
	 */
	private boolean httpPost(String urlString, Map<String, String> params) {
		HttpURLConnection connection = null;
		boolean result = false;
		try {
			URL url = new URL(urlString);
			connection = (HttpURLConnection) url.openConnection();
			connection.setDoOutput(true);
			connection.setRequestProperty("Accept-Language", "ja");

			PrintStream out = new PrintStream(connection.getOutputStream());
			boolean firstParam = true;
			for (Entry<String, String> param : params.entrySet()) {
				String key = URLEncoder.encode(param.getKey(), ENCODING);
				String value = URLEncoder.encode(param.getValue(), ENCODING);
				if (firstParam) {
					out.print(key + "=" + value);
					firstParam = false;
				} else {
					out.print("&" + key + "=" + value);
				}
			}
			boolean streamError = out.checkError();
			out.close();

			if (!streamError) {
				int responseCode = connection.getResponseCode();
				result = (responseCode == HttpURLConnection.HTTP_OK);
			}
		} catch (Exception e) {
			return false;
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
		return result;
	}
}
