# -*- coding: cp936 -*-
# 
import  logging
import  sys

class   PyMap(object):
    def __init__(self):
        self.aPyStr = []
            
    def LoadFromTxt(self, path):
        f = open(path)      
        for line in f:
            line = line.strip('\r\n')
            if len(line) == 0:
                continue
            if line.isdigit():
                continue
            if line.isupper():
                continue
            self.aPyStr.append(line)    
        f.close()
    
    def Print(self):
        for py in self.aPyStr:
            logging.debug('%4d - %s' % (self.aPyStr.index(py), py))
        
    def LoadFromPy(self, path):     
        self.aPyStr = imp.load_source('', path)
        
    def Save(self, path):
        f = open(path, 'w')
        f.write('# -*- coding: cp936 -*-\n')
        f.write('workcondition = [')
        for wc in wcList:
            f.write('\n\t{')
            for k, v in wc.items():
                f.write("\n\t\t%-12s : " % ("'%s'" % k))
                if isinstance(v, str):
                    f.write("'%s'" % v)
                elif isinstance(v, int):
                    f.write('%d' % v)
                elif isinstance(v, boolean) and v:
                    f.write('True')
                elif isinstance(v, boolean) and (not v):
                    f.write('False')
                f.write(',')
            f.write('\n\t}, \n')
        f.write(']\n')
        f.close()   
                   
class PyNetMaker(object):
    def __init__(self, pyMap):
        self.pyMap = pyMap
        
    def findValidSyllable(self, str):
        if len(str) == 0:
            return []
        idxList = []
        hit = False
        for syllable in self.pyMap.aPyStr:
            if not hit:
                if syllable[0] == str[0]:
                    hit = True
            else:
                if syllable[0] != str[0]:
                    return idxList
            if str.startswith(syllable):
                idxList.append(self.pyMap.aPyStr.index(syllable))
        return idxList                  
        
    def preProcess(self, inputStr):
        seg = {}
        length = len(inputStr)
        for i in range(length):
            idxList = self.findValidSyllable(inputStr[i : ])
            seg[i] = [(len(self.pyMap.aPyStr[idx]) - 1) for idx in idxList]
        return seg
        
    def PrintSeg(self, seg):
        for k, v in seg.items():
            msg = 'x%d - ' % k
            for l in v:
                msg += '%d = %d, ' % (v.index(l), l)
            logging.debug(msg)
        
    def printPyNet(self, inputStr, pyNet, seg):
        str = ''
        for i, j in pyNet:
            str += inputStr[i : i + seg[i][j] + 1]
            str += "'"
        print str
        
    def Proc(self, inputStr):
        seg = self.preProcess(inputStr)
        i = 0
        j = 0
        pyNet = []
        while True:
            if seg.has_key(i) and (j < len(seg[i])):
                pyNet.append((i, j))
                i = i + seg[i][j] + 1
                j = 0
                if i == len(inputStr):
                    self.printPyNet(inputStr, pyNet, seg)
            else:
                if i == 0:
                    return
                i, j = pyNet.pop()
                j += 1
           
class Main(object):         
    def Main(self):
        pyMap = PyMap()
        pyMap.LoadFromTxt('pinyin_list.txt')
        while(True):
            inputStr = raw_input('\n>')
            inputStr = inputStr.strip('\r\n')
            if len(inputStr) == 0:
                return
            pyNetMaker = PyNetMaker(pyMap)
            pyNetMaker.Proc(inputStr)                       
        
if __name__ == '__main__':
    loggingFormat   =   '%(lineno)04d %(levelname)-8s %(message)s'
    logging.basicConfig(level=logging.DEBUG,    format=loggingFormat,   datefmt='%H:%M',    )
    main = Main()
    main.Main()
