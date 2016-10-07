#ifndef GPSTOGGLER3_COMMANDFACTORY_H
#define GPSTOGGLER3_COMMANDFACTORY_H

#include "Command.h"

class CCommandFactory {
public:
    static CCommand* create(const char* command);
};

#endif //GPSTOGGLER3_COMMANDFACTORY_H
