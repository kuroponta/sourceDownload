package sourceDownload;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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
		//更新された項目のListを作成
		if (dirMap.size() != 0) {
			System.out.println("\nファイルの更新日時から更新の有無を確認します.");
			List<String> updateList = getUpdateList(dirName,dirMap,downloadList);
			//リストからファイル作成
			getUpdateFile(dirName,updateList);
		}
		System.out.println("\nダウンロードを終了します.");
	}

	/**
	 * 更新されているファイル一覧を取得します.
	 * @param dirName 
	 * @param updateList
	 * @throws IOException 
	 */
	private static void getUpdateFile(String dirName, List<String> updateList) throws IOException {
		if (updateList.size() == 0) {
			System.out.println("\n更新はありません.");
		} else {
			System.out.println("\n更新したファイル一覧を取得します.");
			
			updateList.stream().forEach(u -> System.out.println(u));
			// 更新ファイル一覧が存在するかのチェック
			if (!Files.exists(Paths.get(dirName, "updateFileList.txt"))) {
				//ファイルが存在しない場合ファイルを作成
				Files.createFile(Paths.get(dirName, "updateFileList.txt"));
				/*
				System.out.println("\n取得中...");
				System.out.println("\n取得が完了しました.");
				*/
			}
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
			String updateDate = DATE_FORMAT.format(Paths.get(dirName + url).toFile().lastModified());
			//新規ファイルの場合
			if(!dirMap.containsKey(url)){
				updateList.add("新規 ： " + url + " ： " + updateDate);
			} else {
				//更新前の日時と比較し、異なる場合はファイルのurlを詰める
				if (!dirMap.get(url).equals(updateDate)){
					updateList.add("更新 ： " + url + " ： " + updateDate);
				}
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
		Map<String, String> updateDirMap = new HashMap<String,String>();

		if (!Files.isDirectory(Paths.get(localDir))) {
			//ディレクトリがない場合は新規作成（Mapはカラ）
			System.out.println("\nソースが存在しないため、新規作成します.");
		} else {
			//ディレクトリがある場合は更新日時一覧をMapに詰め込み
			downloadList.stream()
						.forEach(url -> updateDirMap.put(url,DATE_FORMAT.format(Paths.get(dirName + url).toFile().lastModified())));
		}

		return updateDirMap;
	}

	/**
	 * urlList.txtファイルの中身を取得します.
	 * @return downloadList
	 * @throws IOException 
	 */
	static List<String> getFileContents() throws IOException {
		List<String> urlList = new ArrayList<String>();
		List<String> downloadList = new ArrayList<String>();
		//ファイル読み込み
		List<String> fileContentStr = Files.readAllLines(Paths.get(FILE_PATH));
		//"\"を"/"に置換
		fileContentStr.stream().forEach(st -> urlList.add(st.replace("\\", "/")));
		downloadList = getDownloadList(urlList);

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
		String dirPath = "/";
		String[] dirStr = url.split("/");
		for (int i=0; i<dirStr.length-1; i++) {
			if (!dirStr[i].equals("")){
				dirPath += dirStr[i] + "/";
			}
		}
		return dirPath;
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
			int idx = urlStr.lastIndexOf(".");

			String extensionStr = urlStr.substring(idx + 1);
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
