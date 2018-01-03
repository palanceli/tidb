
import java.io.*;
import java.lang.Exception;
import java.util.regex.*;
import java.lang.*;
import java.util.*;
import java.util.logging.*;

class PyMap {
	private ArrayList<String> aPyStr = null;

	public void LoadFromTxt(String txtPath) {
		aPyStr = new ArrayList<String>();

		FileInputStream fis = null;
		InputStreamReader isr = null;
		BufferedReader br = null;
		try {
			// BufferedReader br = new BufferedReader(new InputStreamReader(new
			// FileInputStream(txtPath)));
			// sunjie:拆分定义,在方法结束释放
			fis = new FileInputStream(txtPath);
			isr = new InputStreamReader(fis);
			br = new BufferedReader(isr);
			// sunjie:增加try catch以便在finally中释放Stream对象,避免内存泄露,但同时会增加内存和时间开销

			String line = null;
			Pattern patternUpper = Pattern.compile("[A-Z]*");
			Pattern patternNum = Pattern.compile("[0-9]*");
			while ((line = br.readLine()) != null) {
				Matcher matcherUpper = patternUpper.matcher(line);
				Matcher matcherNum = patternNum.matcher(line);
				if (matcherUpper.matches() || matcherNum.matches())
					continue;
				this.aPyStr.add(line);
			}
			System.out.println(aPyStr);
		} catch (IOException e) {
			// TODO: handle exception
			// throw e;
		} finally {
			// sunjie:在finally 释放Stream对象
			closeStream(br);
			closeStream(isr);
			closeStream(fis);
		}
	}

	public ArrayList<String> getPyStrArray() {
		return aPyStr;
	}

	public void closeStream(Closeable stream) {
		if (stream != null) {
			try {
				stream.close();
			} catch (Exception e) {
				// TODO: handle exception
				System.out.println("Could not close stream");
			}
		}
	}

}

public class PinyinNet2 {
	private PyMap mPyMap = null;
	private HashMap<Integer, String> cacheMap = null;

	public PinyinNet2(PyMap pyMap) {
		this.mPyMap = pyMap;
	}

	public ArrayList<Integer> findValidSyllable(String str) {
		// 返回str开头能命中的所有合法音节串的弧长
		ArrayList<Integer> result = new ArrayList<Integer>();
		if (str.length() == 0)
			return result;
		boolean bHit = false;
		ArrayList<String> pyStrArray = mPyMap.getPyStrArray();

		for (int i = 0; i < pyStrArray.size(); i++) {
			String syllable = pyStrArray.get(i);
			if (syllable.charAt(0) > str.charAt(0))
				return result;

			if (str.startsWith(syllable)) {
				result.add(syllable.length() - 1);
			}
		}
		return result;
	}

	public ArrayList<Integer> findValidSyllable2(String str) {
		// 返回str开头能命中的所有合法音节串的弧长
		ArrayList<Integer> result = new ArrayList<Integer>();
		if (str.length() == 0)
			return result;
		boolean bHit = false;
		ArrayList<String> pyStrArray = mPyMap.getPyStrArray();

		for (int i = 0; i < pyStrArray.size(); i++) {
			String syllable = pyStrArray.get(i);
			if (syllable.charAt(0) > str.charAt(0))
				return result;

			// sunjie:此处修改String.startsWith方法,降低耗时
			boolean startWidth = true;
			for (int j = 0; j < syllable.length(); j++) {
				if (j > str.length() - 1 || syllable.charAt(j) != str.charAt(j)) {
					startWidth = false;
					break;
				}
			}

			if (startWidth) {
				result.add(syllable.length() - 1);
			}
		}
		return result;
	}

	public ArrayList<ArrayList<Integer>> preProcess(String inputStr) {
		// [..., [an, bn, cn...], ...] inputStr第n个字符开头能匹配到的所有音节串长度
		ArrayList<ArrayList<Integer>> seg = new ArrayList<ArrayList<Integer>>();
		for (int i = 0; i < inputStr.length(); i++) {
			ArrayList<Integer> lenList = this.findValidSyllable(inputStr
					.substring(i));
			seg.add(lenList);
		}
		return seg;
	}

	private void printPyNet(String inputStr, LinkedList<Integer> pyNet,
			ArrayList<ArrayList<Integer>> seg) {
		// String str = new String();
		// sunjie:此处使用StringBuilder实现,降低内存开销,但会增加时间开销
		StringBuilder str = new StringBuilder();
		StringBuilder split = new StringBuilder("'");
		for (Integer item : pyNet) {
			int nStart = (item.intValue() >> 16) & 0x0000ffff;
			int nIdx = item.shortValue();
			int nEnd = nStart + seg.get(nStart).get(nIdx) + 1;
			// str = str.concat(inputStr.substring(nStart, nEnd));
			// str = str.concat("'");
			// sunjie: 此处使用StringBuilder实现,降低内存开销,但会增加时间开销
			str = str.append(inputStr.substring(nStart, nEnd)).append(split);
			// sunjie: 此处使用String map 缓存数据,减少new String(),但由于使用map,耗时变大
			// str = str.append(getCacheWord(nStart, nEnd,
			// inputStr)).append(split);
		}
		System.out.println(str);
	}

	private String getCacheWord(int start, int end, String src) {
		int key = start | ((end << 16) & 0xffff0000);
		if (cacheMap == null)
			cacheMap = new HashMap<>();
		String tmp = cacheMap.get(key);
		if (tmp != null) {
			return tmp;
		} else {
			tmp = src.substring(start, end);
			cacheMap.put(Integer.valueOf(key), tmp);
			return tmp;
		}
	}

	public void MainProc(String inputStr) {
		ArrayList<ArrayList<Integer>> seg = this.preProcess(inputStr);
		System.out.println(seg);

		int nStart = 0;
		int nIdx = 0;
		// sunjie: 此处使用LinkedList替换ArrayList,由于增删操作较多,可降低内存开销,但同时会增加时间开销
		LinkedList<Integer> pyNet = new LinkedList<Integer>();
		int i = 0;
		while (true) {
			if (nStart < seg.size() && seg.get(nStart).size() != 0
					&& nIdx < seg.get(nStart).size()) {
				pyNet.add(((nStart << 16) & 0xffff0000) | (nIdx & 0x0000ffff));
				int nStep = seg.get(nStart).get(nIdx) + 1;
				// System.out.println("nStart:" + nStart + ", nIdx:" + nIdx +
				// ", nStep:" + nStep);
				nStart += nStep;
				nIdx = 0;

				if (nStart == inputStr.length()) {
					this.printPyNet(inputStr, pyNet, seg);
				}
			} else {
				if (nStart == 0)
					return;
				Integer item = pyNet.remove(pyNet.size() - 1);
				nStart = (item.intValue() >> 16) & 0x0000ffff;
				nIdx = item.shortValue() + 1;
			}
		}
	}

	public static void main(String[] args) {
		PyMap pyMap = new PyMap();
		// sunjie:修改主函数中try catch,减少try catch层级
		//try {
			pyMap.LoadFromTxt("pinyin_list.txt");
		//} catch (Exception e) {
		//	e.printStackTrace();
		//}
		// System.out.println(pyMap.getPyStrArray());
		PinyinNet2 pinyinNet = new PinyinNet2(pyMap);

		Console console = System.console();
		String inputStr = null;
		while (true) {
			inputStr = console.readLine(">");
			if (inputStr.length() == 0)
				break;
			pinyinNet.MainProc(inputStr);
		}
	}
}
