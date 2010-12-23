package chj;

import java.awt.Desktop;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.ExampleMode;
import org.kohsuke.args4j.Option;

public class CopyHudsonJob {

	private static Log LOGGER = LogFactory.getLog(CopyHudsonJob.class);

	/** 置換文字列 */
	private static final String REPLACE_STRING = "%PROJECT%";

	@Option(name = "-u", metaVar = "URL", usage = "HudsonのURL")
	private static String hudsonUrl = "http://192.168.233.131/hudson/";
	@Option(name = "-s", metaVar = "srcJob", usage = "コピー元ジョブ")
	private static String srcJob = "templateJob";
	@Option(name = "-d", metaVar = "dstJob", usage = "コピー先ジョブ", required = true)
	private static String dstJob;

	private HttpClient client;

	public static void main(String[] args) throws Exception {
		new CopyHudsonJob().run(args);
	}

	public void run(String[] args) throws Exception {
		// 引数のパース
		parseArgs(args);

		// Hudson APIの実行
		client = new DefaultHttpClient();
		copyJob(srcJob, dstJob);
		enableJob(dstJob);
		replaceJobSetting(dstJob);
		client.getConnectionManager().shutdown();

		// ブラウザオープン
		Desktop.getDesktop().browse(
				new URI(hudsonUrl + "job" + "/" + dstJob + "/"));
	}

	/**
	 * コマンドライン引数をパースする。
	 */
	private void parseArgs(String[] args) {
		CmdLineParser parser = new CmdLineParser(new CopyHudsonJob());

		try {
			parser.parseArgument(args);
		} catch (CmdLineException e) {
			System.err.println(e);
			System.err.printf("USAGE:%n\tjava CopyHudsonJob %s%n",
					parser.printExample(ExampleMode.ALL));
			parser.printUsage(System.err);

			System.exit(1);
		}
	}

	/**
	 * ジョブをコピーする。
	 */
	private void copyJob(String srcJob, String dstJob) throws Exception {
		LOGGER.info("[start] copyJob");

		// リクエスト構築
		String requestUrl = hudsonUrl + "createItem";
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("name", dstJob));
		params.add(new BasicNameValuePair("mode", "copy"));
		params.add(new BasicNameValuePair("from", srcJob));

		// リクエスト送信
		HttpPost ｒequest = new HttpPost(requestUrl);
		ｒequest.setEntity(new UrlEncodedFormEntity(params));
		HttpResponse response = client.execute(ｒequest);

		// レスポンス取得
		StatusLine statusLine = response.getStatusLine();
		String statusCode = String.valueOf(statusLine.getStatusCode());
		// ステータスコードが"4xx"or"5xx"ならば、終了
		if (statusCode.startsWith("4") || statusCode.startsWith("5")) {
			LOGGER.error(statusLine);
			System.exit(1);
		}
		LOGGER.info(statusLine);
		LOGGER.info("copy job from '" + srcJob + "' to '" + dstJob + "'");
		response.getEntity().consumeContent();

		LOGGER.info("[ end ] copyJob");
	}

	/**
	 * ジョブを有効化する。
	 */
	private void enableJob(String dstJob) throws Exception {
		LOGGER.info("[start] enableJob");

		// リクエスト送信
		String requestUrl = hudsonUrl + "job" + "/" + dstJob + "/" + "enable";
		HttpPost request = new HttpPost(requestUrl);
		HttpResponse response = client.execute(request);

		// レスポンス取得
		LOGGER.info(response.getStatusLine());
		LOGGER.info("enable job '" + dstJob + "'");
		response.getEntity().consumeContent();

		LOGGER.info("[ end ] enableJob");
	}

	/**
	 * ジョブの設定内容を置き換える。
	 */
	private void replaceJobSetting(String dstJob) throws Exception {
		LOGGER.info("[start] replaceJobSetting");

		// Getリクエスト送信
		String requestUrl = hudsonUrl + "job" + "/" + dstJob + "/"
				+ "config.xml";
		HttpGet getRequest = new HttpGet(requestUrl);
		HttpResponse getResponse = client.execute(getRequest);

		// Getレスポンス取得
		LOGGER.info(getResponse.getStatusLine());
		LOGGER.info("get job setting '" + dstJob + "'");
		String config = EntityUtils.toString(getResponse.getEntity());
		getResponse.getEntity().consumeContent();

		// 文字列置換
		String replacedConfig = config.replaceAll(REPLACE_STRING, dstJob);

		// Postリクエスト送信
		HttpPost postRequest = new HttpPost(requestUrl);
		postRequest.setEntity(new StringEntity(replacedConfig, HTTP.UTF_8));
		HttpResponse postResponse = client.execute(postRequest);

		// Postレスポンス取得
		LOGGER.info(postResponse.getStatusLine());
		LOGGER.info("replace job setting '" + dstJob + "'");
		postResponse.getEntity().consumeContent();

		LOGGER.info("[ end ] replaceJobSetting");
	}
}
