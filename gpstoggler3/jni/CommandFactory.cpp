#include "CommandFactory.h"
#include "ReadProcDirectory.h"
#include "ReadProcDirectoryDebug.h"


CCommand* CCommandFactory::create(const char* command) {
    if (0 == strcmp (command, CReadProcDirectory::COMMAND)) {
        return new CReadProcDirectory();
    }

    if (0 == strcmp (command, CReadProcDirectoryDebug::COMMAND)) {
        return new CReadProcDirectoryDebug();
    }

    return NULL;
}
