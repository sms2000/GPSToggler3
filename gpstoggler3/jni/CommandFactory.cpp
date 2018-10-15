#include "CommandFactory.h"
#include "ReadProcDirectory.h"
#include "ReadProcDirectoryDebug.h"


CCommand* CCommandFactory::create(const char* command) {
    if (0 == strncmp (CReadProcDirectory::COMMAND, command, strlen(CReadProcDirectory::COMMAND))) {
        return new CReadProcDirectory();
    }

    if (0 == strncmp (CReadProcDirectoryDebug::COMMAND, command, strlen(CReadProcDirectoryDebug::COMMAND))) {
        return new CReadProcDirectoryDebug();
    }

    return NULL;
}
