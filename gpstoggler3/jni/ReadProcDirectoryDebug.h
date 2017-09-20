#ifndef GPSTOGGLER3_READPROCDIRECTORYDEBUG_H
#define GPSTOGGLER3_READPROCDIRECTORYDEBUG_H

#include "ReadProcDirectory.h"

class CReadProcDirectoryDebug : public CReadProcDirectory {
public:
    static const char* COMMAND;

    CReadProcDirectoryDebug  (void);
    ~CReadProcDirectoryDebug();

    virtual bool execute (std::string &output);
};

#endif //GPSTOGGLER3_READPROCDIRECTORYDEBUG_H
