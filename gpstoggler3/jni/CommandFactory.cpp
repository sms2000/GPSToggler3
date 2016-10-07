#include "CommandFactory.h"
#include "ReadProcDirectory.h"


CCommand* CCommandFactory::create(const char* command) {
    if (0 == strcmp (command, CReadProcDirectory::COMMAND)) {
        return new CReadProcDirectory();
    }

    return NULL;
}
