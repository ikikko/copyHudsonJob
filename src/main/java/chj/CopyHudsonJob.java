package chj;

import hudson.cli.CLI;

import java.awt.Desktop;
import java.net.URI;
import java.net.URL;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.ExampleMode;
import org.kohsuke.args4j.Option;

public class CopyHudsonJob {

	@Option(name = "-u", metaVar = "URL", usage = "HudsonのURL")
	static String url = "http://192.168.233.131/hudson/";
	@Option(name = "-s", metaVar = "src", usage = "コピー元ジョブ")
	static String src = "templateJob";
	@Option(name = "-d", metaVar = "dst", usage = "コピー先ジョブ", required = true)
	static String dst;

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

		// Jobのコピー
		// TODO 既に存在する場合はエラーにする
		CLI cli = new CLI(new URL(url));
		cli.execute("copy-job", src, dst);
		cli.close();

		// HttpClientの設定
		String jobUrl = url + "job" + "/" + dst + "/";
		HttpClient client = new DefaultHttpClient();

		// config.xmlの取得
		String configXmlUrl = jobUrl + "config.xml";
		HttpGet configXmlRequest = new HttpGet(configXmlUrl);
		HttpResponse configXmlResponse = client.execute(configXmlRequest);
		String configXml = EntityUtils.toString(configXmlResponse.getEntity());

		// ジョブの有効化
		String enableUrl = jobUrl + "enable";
		HttpPost enableRequest = new HttpPost(enableUrl);
		client.execute(enableRequest);

		// ジョブのWebページを開く
		Desktop.getDesktop().browse(new URI(jobUrl));
	}
}
