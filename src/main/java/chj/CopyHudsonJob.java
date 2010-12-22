package chj;

import java.awt.Desktop;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
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
		enabledJob(dstJob);
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
		// TODO 既に存在する場合はエラーにする
		String requestUrl = hudsonUrl + "createItem";
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("name", dstJob));
		params.add(new BasicNameValuePair("mode", "copy"));
		params.add(new BasicNameValuePair("from", srcJob));

		HttpPost ｒequest = new HttpPost(requestUrl);
		ｒequest.setEntity(new UrlEncodedFormEntity(params));
		client.execute(ｒequest).getEntity().consumeContent();

		System.out
				.println("copy job from [" + srcJob + "] to [" + dstJob + "]");
	}

	/**
	 * ジョブを有効化する。
	 */
	private void enabledJob(String dstJob) throws Exception {
		String requestUrl = hudsonUrl + "job" + "/" + dstJob + "/" + "enable";

		HttpPost request = new HttpPost(requestUrl);
		client.execute(request).getEntity().consumeContent();

		System.out.println("enable job [" + dstJob + "]");
	}

	/**
	 * ジョブの設定内容を置き換える。
	 */
	private void replaceJobSetting(String dstJob) throws Exception {
		String requestUrl = hudsonUrl + "job" + "/" + dstJob + "/"
				+ "config.xml";

		HttpGet getRequest = new HttpGet(requestUrl);
		HttpResponse getResponse = client.execute(getRequest);
		String config = EntityUtils.toString(getResponse.getEntity());
		getResponse.getEntity().consumeContent();

		String replacedConfig = config.replaceAll(REPLACE_STRING, dstJob);
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("config.xml", replacedConfig));

		HttpPost postRequest = new HttpPost(requestUrl);
		postRequest.setEntity(new StringEntity(replacedConfig, HTTP.UTF_8));
		HttpResponse postResponse = client.execute(postRequest);
		postResponse.getEntity().consumeContent();

		System.out.println("replace job setting [" + dstJob + "]");
	}
}
