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
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.ExampleMode;
import org.kohsuke.args4j.Option;

public class CopyHudsonJob {

	@Option(name = "-u", metaVar = "URL", usage = "HudsonのURL")
	static String hudsonUrl = "http://192.168.233.131/hudson/";
	@Option(name = "-s", metaVar = "src", usage = "コピー元ジョブ")
	static String src = "templateJob";
	@Option(name = "-d", metaVar = "dst", usage = "コピー先ジョブ", required = true)
	static String dst;

	private static HttpClient client;

	public static void main(String[] args) throws Exception {
		// コマンドライン引数のパース
		// TODO コマンドラインからじゃなく、標準入力から受け取った方が楽かも
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

		client = new DefaultHttpClient();
		copyJob(src, dst);
		enabledJob();
		replaceJobSetting(dst);
		client.getConnectionManager().shutdown();

		Desktop.getDesktop().browse(
				new URI(hudsonUrl + "job" + "/" + dst + "/"));
	}

	/**
	 * ジョブをコピーする。
	 */
	private static void copyJob(String src, String dst) throws Exception {
		// TODO 既に存在する場合はエラーにする
		String requestUrl = hudsonUrl + "createItem";
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("name", dst));
		params.add(new BasicNameValuePair("mode", "copy"));
		params.add(new BasicNameValuePair("from", src));

		HttpPost ｒequest = new HttpPost(requestUrl);
		ｒequest.setEntity(new UrlEncodedFormEntity(params));
		client.execute(ｒequest).getEntity().consumeContent();

		System.out.println("copy job from [" + src + "] to [" + dst + "]");
	}

	/**
	 * ジョブを有効化する。
	 */
	private static void enabledJob() throws Exception {
		String requestUrl = hudsonUrl + "job" + "/" + dst + "/" + "enable";

		HttpPost request = new HttpPost(requestUrl);
		client.execute(request).getEntity().consumeContent();

		System.out.println("enable job [" + dst + "]");
	}

	/**
	 * ジョブの設定内容を置き換える。
	 */
	private static void replaceJobSetting(String dst) throws Exception {
		String requestUrl = hudsonUrl + "job" + "/" + dst + "/" + "config.xml";

		HttpGet request = new HttpGet(requestUrl);
		HttpResponse response = client.execute(request);
		String configXml = EntityUtils.toString(response.getEntity());
		response.getEntity().consumeContent();
	}

}
