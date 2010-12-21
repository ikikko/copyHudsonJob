package chj;

import hudson.cli.CLI;

import java.net.URL;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.ExampleMode;
import org.kohsuke.args4j.Option;

public class CopyHudsonJob {

	@Option(name = "-u", metaVar = "URL", usage = "count number")
	static String url = "http://192.168.233.131/hudson/";
	@Option(name = "-s", metaVar = "src", usage = "コピー元ジョブ")
	static String src = "templateJob";
	@Option(name = "-d", metaVar = "dst", usage = "コピー先ジョブ", required = true)
	static String dst;

	public static void main(String[] args) throws Exception {

		// コマンドライン引数のパース
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
		CLI cli = new CLI(new URL(url));
		cli.execute("copy-job", "helloUbuntuHudson", "helloCliHudson");
		cli.close();
	}
}
