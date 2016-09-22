import java.io.*;
import java.lang.Exception;
import java.util.regex.*;
import java.lang.*;
import java.util.*;
import java.util.logging.*;

class PyMap {
	private ArrayList<String> aPyStr = null;

	public void LoadFromTxt(String txtPath) throws IOException{
		aPyStr = new ArrayList<String>();

		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(txtPath)));
		String line = null;
		Pattern patternUpper = Pattern.compile("[A-Z]*");
		Pattern patternNum = Pattern.compile("[0-9]*");
		while((line = br.readLine()) != null){
			Matcher matcherUpper = patternUpper.matcher(line);
			Matcher matcherNum = patternNum.matcher(line);
			if(matcherUpper.matches() || matcherNum.matches())
				continue;
			this.aPyStr.add(line);
		}
	}

	public ArrayList<String> getPyStrArray(){
		return aPyStr;
	}

}

public class PinyinNet {
	private PyMap mPyMap = null;

	public PinyinNet(PyMap pyMap){
		this.mPyMap = pyMap;
	}

	public ArrayList<Integer> findValidSyllable(String str){
		// 返回str开头能命中的所有合法音节串的弧长
		ArrayList<Integer> result = new ArrayList<Integer>();
		if(str.length() == 0)
			return result;
		ArrayList<String> pyStrArray = mPyMap.getPyStrArray();

		for(int i=0; i<pyStrArray.size(); i++){
			String syllable = pyStrArray.get(i);
			if(syllable.charAt(0) > str.charAt(0))
				return result;

			if(str.startsWith(syllable)){
				result.add(syllable.length() - 1);
			}
		}
		return result;
	}

	public ArrayList<ArrayList<Integer> > preProcess(String inputStr){
		// [..., [an, bn, cn...], ...] inputStr第n个字符开头能匹配到的所有音节串长度
		ArrayList<ArrayList<Integer> > seg = new ArrayList<ArrayList<Integer> >();
		for(int i=0; i<inputStr.length(); i++){
			ArrayList<Integer> lenList = this.findValidSyllable(inputStr.substring(i));
			seg.add(lenList);
		}
		return seg;
	}

	private void printPyNet(String inputStr, ArrayList<Integer> pyNet, ArrayList<ArrayList<Integer> > seg){
		String str = new String();
		for(Integer item : pyNet){
			int nStart = (item.intValue() >> 16) & 0x0000ffff;
			int	nIdx = item.shortValue();
			int nEnd = nStart + seg.get(nStart).get(nIdx) + 1;
			str = str.concat(inputStr.substring(nStart, nEnd));
			str = str.concat("'");
		}
		System.out.println(str);
	}

	public void MainProc(String inputStr){
		ArrayList<ArrayList<Integer> > seg = this.preProcess(inputStr);
		System.out.println(seg);

		int nStart = 0;
		int nIdx = 0;
		ArrayList<Integer> pyNet = new ArrayList<Integer>();
		int i = 0;
		while(true){
			if(nStart<seg.size() && seg.get(nStart).size() !=0 && nIdx<seg.get(nStart).size()){
				pyNet.add(((nStart << 16)&0xffff0000) |(nIdx & 0x0000ffff));
				int nStep = seg.get(nStart).get(nIdx) + 1;
				// System.out.println("nStart:" + nStart + ", nIdx:" + nIdx + ", nStep:" + nStep);
				nStart += nStep;
				nIdx = 0;

				if(nStart == inputStr.length()){
					this.printPyNet(inputStr, pyNet, seg);
				}
			}else{
				if(nStart == 0)
					return;
				Integer item = pyNet.remove(pyNet.size() - 1);
				nStart = (item.intValue() >> 16) & 0x0000ffff;
				nIdx = item.shortValue() + 1;
			}
		}

	}

	public static void main(String [] args){
		PyMap pyMap = new PyMap();
		try{
			pyMap.LoadFromTxt("pinyin_list.txt");
		}catch(Exception e){
			e.printStackTrace();
		}
		// System.out.println(pyMap.getPyStrArray());
		PinyinNet pinyinNet = new PinyinNet(pyMap);

		Console console = System.console();
		String inputStr = null;
		while(true){
			inputStr = console.readLine(">");
			if(inputStr.length()== 0)
				break;
			pinyinNet.MainProc(inputStr);
		}

	}
}
