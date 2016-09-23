package sourceDownload;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class SourceDownload {
	
	final static String FILE_PATH = "C:\\sourceDownload\\urlList.txt";
	//更新日時表示書式
	final static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	
	@SuppressWarnings("resource")
	public static void main(String args[]) throws IOException, InterruptedException {
		System.out.println("取得したいサイトの番号を入力してください。");
		System.out.println("1 > 商用");
		System.out.println("2 > STG（SP版のみ）");

		// ドメイン格納
		String domain = "";
		String siteName = "";
		//指定サイトの番号を保存
		int val;
		do {
			Scanner scan = new Scanner(System.in);
			//1か2以外入力時、再度入力を促す。
			val = scan.nextInt();
			if(val != 1 && val != 2){
				System.out.println("選択した番号は存在しません。\nもう一度入力してください。");
			}
		} while(val != 1 && val != 2);

		//指定サイトによって取得先を変更
		if (val == 1){
			//github用に削除
			domain = "";
			siteName = "商用サイト";
		} else if (val == 2) {
			domain = "";
			siteName = "STG（SP版のみ）";
		}

		System.out.println(siteName + " ： " + domain + "よりソースをダウンロードします.\n");
		//保存先ディレクトリの指定
		String dirName = chooseSaveDirectory();
		//url一覧取得
		List<String> downloadList = getFileContents();
		//指定ディレクトリ内の更新日付取得
		Map<String, String> dirMap = checkDir(dirName,downloadList);
		//コマンド一覧取得
		List<String> commandList = getCommandList(downloadList,domain,dirName);
		//ダウンロード処理実行
		download(downloadList,commandList);
		System.out.println("\nファイルの更新日時から更新の有無を確認します.");
		//更新された項目のListを作成
		if (dirMap.size() != 0) {
			List<String> updateList = getUpdateList(dirName,dirMap,downloadList);
			//リストからファイル作成
			getUpdateFile(updateList);
		}
	}

	/**
	 * 更新されているファイル一覧を取得します.
	 * @param updateList
	 */
	private static void getUpdateFile(List<String> updateList) {
		if (updateList.size() == 0) {
			System.out.println("\n更新はありません.");
		} else {
			System.out.println("\n更新したファイル一覧を取得します.");
		}
	}

	/**
	 * 更新したファイル一覧を取得します.
	 * @param dirName
	 * @param dirMap
	 * @param downloadList
	 * @return updateList
	 */
	private static List<String> getUpdateList(String dirName, Map<String, String> dirMap, List<String> downloadList) {
		List<String> updateList = new ArrayList<String>();
		for (String url : downloadList) {
			//更新ファイルの判定をします
			String localDir = dirName + url;
			File dirFile = new File(localDir);
			//更新前の日時と比較し、異なる場合はファイルのurlを詰める
			if (!dirMap.get(url).equals(DATE_FORMAT.format(dirFile.lastModified()))){
				updateList.add(url);
			}
		}
		return updateList;
	}

	/**
	 * 保存先ディレクトリの選択をします.
	 * @return dirName
	 */
	@SuppressWarnings("resource")
	static String chooseSaveDirectory() {
		System.out.print("保存先ディレクトリ ： ");
		Scanner dirValue = new Scanner(System.in);
		//保存先ディレクトリ
		String dirName = dirValue.next();
		//"\"が入力された際は"/"に変換
		dirName = dirName.replace("\\", "/");
		String dirLast = dirName.substring(dirName.length()-1);
		//末尾に"/"がある際は除外する
		if (dirLast.equals("/")) {
			dirName = dirName.replaceFirst("/$","");
		}
		return dirName;
	}

	/**
	 * 選択ディレクトリ内の更新日付を取得します.
	 * @param dirName
	 * @param downloadList 
	 * @return 
	 */
	static Map<String, String> checkDir(String dirName, List<String> downloadList) {
		String localDir = dirName + "/onlineshop";
		File file = new File(localDir);
		Map<String, String> dirUpdateMap = new HashMap<String,String>();
		//既にファイルがあるかどうか
		if (file.exists()) {
			//ある場合は更新日時一覧をMapに詰め込み
			for (String url : downloadList){
				localDir = dirName + url;
				File dlUrl = new File(localDir);
				dirUpdateMap.put(url, DATE_FORMAT.format(dlUrl.lastModified()));
			}
		} else {
			//ない場合は新規作成（Mapはカラ）
			System.out.println("\nソースが存在しないため、新規作成します.");
		}
		return dirUpdateMap;
	}

	/**
	 * urlList.txtファイルの中身を取得します.
	 * @return downloadList
	 */
	@SuppressWarnings("resource")
	static List<String> getFileContents() {
		//URL一覧格納用
		List<String> urlList = new ArrayList<String>();
		try{
			File file = new File(FILE_PATH);
			BufferedReader br = new BufferedReader(new FileReader(file));
			String str;
			while((str = br.readLine()) != null){
				urlList.add(str.replace("\\", "/"));
			}
		}catch(FileNotFoundException e){
			System.out.println(e);
		}catch(IOException e){
			System.out.println(e);
		}
		//ダウンロードURL取得
		List<String> downloadList = getDownloadList(urlList);
		return downloadList;
	}

	/**
	 * 実行コマンドの一覧を作成します.
	 * @param downloadList
	 * @param domain
	 * @param dirName
	 * @return commandList
	 */
	static List<java.lang.String> getCommandList(List<String> downloadList,String domain, String dirName) {
		List<String> commandList = new ArrayList<String>();
		for (String url : downloadList) {
			String dirPath = getDirectoryPath(url);

			String command = "wget -N --no-check-certificate ";
			command += domain + url + " -P " + dirName + dirPath;
			commandList.add(command);
		}
		return commandList;
	}

	/**
	 * ダウンロードするファイルのディレクトリ位置を設定します.
	 * @param url
	 * @return dirPath
	 */
	
	static String getDirectoryPath(String url) {
		//ディレクトリパス格納
		String dirpath = "/";
		String[] dirStr = url.split("/");
		for (int i=0; i<dirStr.length-1; i++) {
			if (!dirStr[i].equals("")){
				dirpath += dirStr[i] + "/";
			}
		}
		return dirpath;
	}

	/**
	 * ダウンロードするファイル一覧を拡張子で絞り込みます.
	 * @param urlList
	 * @return downloadList
	 */
	static List<String> getDownloadList(List<String> urlList) {
		//ダウンロード用URL一覧
		List<String> downloadList = new ArrayList<String>();
		
		for (String urlStr : urlList) {
			String extensionStr = "";
			String[] spritArrayStr = urlStr.split("\\.");
			
			//拡張子格納用
			extensionStr = spritArrayStr[spritArrayStr.length - 1];
			//拡張子判別
			if(extensionStr.equals("html") || extensionStr.equals("css") || extensionStr.equals("js") || extensionStr.equals("xml")){
				downloadList.add(urlStr);
			}
		}
		return downloadList;
	}

	/**
	 * ダウンロード処理を実行します.
	 * @param downloadList
	 * @param commandList
	 */
	static void download(List<String> downloadList, List<String> commandList){
		Runtime rt = Runtime.getRuntime();
		Process pr = null;
		//ダウンロード番号
		try {
			System.out.println("\nダウンロード中…");
			for (String command : commandList){
				pr = rt.exec(command);
			}
			pr.waitFor();
			System.out.println("\nダウンロードが完了しました。");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
