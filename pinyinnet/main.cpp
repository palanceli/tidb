#include <stdio.h>
#include <iostream>
#include <fstream>
#include <string.h>

#define LOG(format, ...) printf("[%-12s:%-4d ] "format"\n", __FILE__, __LINE__, ##__VA_ARGS__)

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
  short** preProcess(const char* inputStr);
  void printPyArc(const char* inputStr, short* arc[]);
  void printPyNet(const char* inputStr, int* route, short** arcArray);
  
  PyMap* mPyMap;
};

short** PyNetMaker::preProcess(const char* inputStr)
{	// arc是[..., [an, bn, cn...], ...] inputStr每个字符开头能匹配到的所有音节串长度
  LOG("preProcess...");
  if(mPyMap == NULL || inputStr == NULL || strlen(inputStr) == 0)
    return NULL;

  short** arcArray = (short**)malloc(sizeof(short*) * strlen(inputStr));

  for(int i=0; i<strlen(inputStr); i++){
    const char* tmpStr = inputStr + i;
    
    static const int nMaxArc = 16;
    short arcLen[nMaxArc] = {0};
    int arcCount = 0;
    for(int j=0; j<mPyMap->GetPyMapSize(); j++){
      char* syllable = mPyMap->GetPyMap()[j];
      if(syllable[0] > tmpStr[0])
        break;

      if(strstr(tmpStr, syllable) == tmpStr){
        arcLen[arcCount++] = strlen(syllable) - 1;
      }
    }

    arcArray[i] = (short*)malloc(sizeof(short) * arcCount + 1);
    arcArray[i][0] = arcCount;
    memcpy(arcArray[i] + 1, arcLen, sizeof(short) * arcCount);
  }
  return arcArray;
}

void PyNetMaker::printPyArc(const char* inputStr, short* arcArray[])
{
  for(int i=0; i<strlen(inputStr); i++){
    char msg[256] = {0};
    sprintf(msg, "%16s: %4d arcs (", inputStr+i, arcArray[i][0]);
    for(int j=0; j<arcArray[i][0]; j++){
      char tmp[8] = {0};
      sprintf(tmp, "%d, ", arcArray[i][j+1]);
      strcat(msg, tmp);
    }
    strcat(msg, ")");
    LOG("%s", msg);
  }
}

void PyNetMaker::printPyNet(const char* inputStr, int* route, short** arcArray)
{
  char result[256] = {0};
  for(int i=0; i<strlen(inputStr); i++){
    int value = route[i];
    short nStart = (value >> 16) & 0x0000ffff;
    short nIdx = value & 0x0000ffff;

    strncat(result, inputStr + nStart, arcArray[nStart][nIdx + 1] + 1);
    strcat(result, "'");
  }
  printf("%s\n", result);
}

void PyNetMaker::MainProc(const char* inputStr)
{
  if(mPyMap == NULL || inputStr == NULL || strlen(inputStr) == 0)
    return;

  short** arcArray = preProcess(inputStr);
  printPyArc(inputStr, arcArray);

  int nStart = 0;
  int nIdx = 0;
  int* route = (int*)malloc(sizeof(int) * strlen(inputStr));
  int top = 0;
  while(1){
    if(nStart<strlen(inputStr) && arcArray[nStart][0]!=0 && nIdx<arcArray[nStart][0]){
      route[top++] = (((nStart << 16) & 0xffff0000) | (nIdx & 0x0000ffff));
      
      nStart += arcArray[nStart][nIdx + 1] + 1;
      nIdx = 0;

      if(nStart == strlen(inputStr))
        printPyNet(inputStr, route, arcArray);
    }else{
      if(nStart == 0)
        return;
      int value = route[--top];
      nStart = (value >> 16) & 0x0000ffff;
      nIdx = (value & 0x0000ffff) + 1;
    }
    LOG("route:");
    for(int i=top - 1; i>=0; i--){
      LOG("%4d, %4d", (route[i] >> 16) & 0x0000ffff, route[i] & 0x0000ffff);
    }
    LOG("=========");
  }
  
  for(int i=0; i<strlen(inputStr); i++){
    free(arcArray[i]);
  }
  free(arcArray);
}

int main(int argc, char *argv[])
{
  PyMap pyMap;
  pyMap.LoadFromTxt("pinyin_list.txt");
  PyNetMaker pyNetMaker(&pyMap);
  pyNetMaker.MainProc("xianguo");
  return 0;
}
