#include "ReadProcDirectory.h"


const char *CReadProcDirectory::COMMAND = "read_proc";


CReadProcDirectory::CReadProcDirectory(void) : CCommand() {
}


CReadProcDirectory::~CReadProcDirectory() {
}


bool CReadProcDirectory::execute (std::string &output) {
    return true;
}
