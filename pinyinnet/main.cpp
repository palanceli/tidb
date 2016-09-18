#include <stdio.h>
#include <iostream>
#include <fstream>
#include <string>
#include <vector>

class PyMap
{
public:
	PyMap() : mSize(0){memset(mPyMap, 0, 512);}
	~PyMap();
	int LoadFromTxt(const char* filepath);
	void Print();
	char** GetPyMap(){return mPyMap;}
	int GetPyMapSize(){return mSize;}
	static const int MaxPyMap = 512;
private:
	char* mPyMap[MaxPyMap];
	int mSize;
};

int PyMap::LoadFromTxt(const char* filepath)
{
	std::ifstream fs(filepath);
	char line[16];
	while(!fs.eof()){
		fs.getline(line, 16);
		int length = strlen(line);
		if(length == 0)
			continue;

		if(line[length - 1] == 0x0D){
			line[--length] = '\0';
		}
		if(length == 0)
			continue;

		bool bPinyin = true;
		for(int i=0; i<length; i++){
			if(line[i] < 'a' || line[i] > 'z'){
				bPinyin = false;
				break;
			}
		}
		if(!bPinyin)
			continue;
		char *pinyin = new char[16];
		strcpy(pinyin, line);
		mPyMap[mSize++] = pinyin;

	}

	return mSize;
}

PyMap::~PyMap()
{
	for(int i=0; i<mSize; i++){
		if(mPyMap[i] != NULL){
			delete [] mPyMap[i];
			mPyMap[i] = NULL;
		}
	}
}

void PyMap::Print()
{
	for(int i=0; i<mSize; i++){
		printf("%d\t%s\t%lu\n", i, mPyMap[i], strlen(mPyMap[i]));
	}
}


class PyNetMaker
{
public:
	PyNetMaker(PyMap* pyMap):mPyMap(pyMap){}
	void MainProc(const char *inputStr);
protected:
	int preProcess(const char* inputStr, int* arc[], int len);
	int findValidSyllable(const char* inputStr, int* result, int len);
	void printPyArc(const char* inputStr, int* arc[]);

	PyMap* mPyMap;
};

int PyNetMaker::findValidSyllable(const char* inputStr, int* result, int len)
{
	if(mPyMap == NULL || result == NULL || len < PyMap::MaxPyMap)
		return -1;

	int cHit = 0;
	if(strlen(inputStr) == 0)
		return cHit;

	for(int i=0; i<mPyMap->GetPyMapSize(); i++){
		char* syllable = mPyMap->GetPyMap()[i];
		if(syllable[0] > inputStr[0])
			return cHit;
		if(strstr(inputStr, syllable) == inputStr){
			result[cHit++] = strlen(syllable) - 1;
		}
	}
	return cHit;
}

int PyNetMaker::preProcess(const char* inputStr, int* arc[], int len)
{	// arc是[..., [an, bn, cn...], ...] inputStr每个字符开头能匹配到的所有音节串长度
	if(len != strlen(inputStr))
		return -1;

	int seg[PyMap::MaxPyMap] = {0};
	for(int i=0; i<strlen(inputStr); i++){
		int c = findValidSyllable(inputStr + i, seg, PyMap::MaxPyMap);
		if(c <= 0)
			arc[i] = NULL;
		else{
			arc[i] = new int[c];
			memcpy(arc[i], seg, c * sizeof(int));
		}
	}
	return 0;
}

void PyNetMaker::printPyArc(const char* inputStr, int* arcArray[])
{
	printf("arc of [%s]", inputStr);
	for(int i=0; i<strlen(inputStr); i++){
		int* arc = arcArray[i];
		if(arc == NULL){
			printf("(), ");
			continue;
		}
		printf("(");
		for(int j=0; j<(sizeof(arc)/sizeof(int)); j++){
			printf("%d ", arc[j]);
		}
		printf("), ");
	}
	printf("\n");	
}

void PyNetMaker::MainProc(const char* inputStr)
{
	if(inputStr == NULL)
		return;

	int** arcArray = new int*[strlen(inputStr)];
	preProcess(inputStr, arcArray, strlen(inputStr));
	printPyArc(inputStr, arcArray);
}

int main(int argc, char *argv[])
{
	PyMap pyMap;
	pyMap.LoadFromTxt("pinyin_list.txt");
	pyMap.Print();
	PyNetMaker pyNetMaker(&pyMap);
	pyNetMaker.MainProc("xianguo");
	return 0;
}