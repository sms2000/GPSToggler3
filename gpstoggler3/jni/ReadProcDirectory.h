#ifndef GPSTOGGLER3_READPROCDIRECTORY_H
#define GPSTOGGLER3_READPROCDIRECTORY_H

#include "Command.h"

class CReadProcDirectory : public CCommand {
public:
    static const char* COMMAND;

    CReadProcDirectory  (void);
    ~CReadProcDirectory();

    virtual bool execute (std::string &output);
};

#endif //GPSTOGGLER3_READPROCDIRECTORY_H
